package org.alfresco.devops.exporters;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.alfresco.devops.util.Constants;
import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.repo.security.authentication.RepositoryAuthenticationDao;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.Location;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class PeopleUsersGroupAcpExporter extends AbstractWebScript{

	private ExporterService exporterService;
	private MimetypeService mimetypeService;
	private AuthorityService authorityService;
	private PersonService personService;
	private ChildApplicationContextManager authenticationContextManager;


	private static Log logger = LogFactory.getLog(PeopleUsersGroupAcpExporter.class);


	private static final List<String> USERS_NOT_TO_EXPORT = Arrays.asList(new String[] { "guest","admin" });
	private static final String PARAM_USERS_TO_EXPORT = "usersToExport";
	private static final String PARAM_GROUPS_TO_EXPORT = "groupsToExport";
	private static final String PARAM_EXCLUDE_SITES_GROUPS = "excludeSiteGroups";
	private static final String PARAM_EXCLUDE_GROUPS = "groupsToExclude";
	private static final String PARAM_EXCLUDE_USERS = "usersToExclude";
	private static final String ZIP_FILE_NAME = "people-users-groups-export.zip";
	private static final String PEOPLE_ACP = "People.acp";
	private static final String USERS_ACP = "Users.acp";
	private static final String GROUP_JSON= "Groups.json";
	private static final String COMMA_SEPARATOR= ",";
    private static final String SITE_PREFIX = "site_";
    private static final String GROUP_SITE_PREFIX = PermissionService.GROUP_PREFIX + SITE_PREFIX;



	private List<String> usersToExcludeList = null;
	private List<String> groupsToExcludeList = null;
	private List<String> usersToExportList = null;
	private List<String> groupsToExportList = null;
	boolean excludeSiteGroups = false;


	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res)
			throws IOException {


		res.setContentType(MimetypeMap.MIMETYPE_ZIP);
		res.setHeader("Content-Disposition","attachment; fileName="+ZIP_FILE_NAME);

		String usersToExportString = req.getParameter(PARAM_USERS_TO_EXPORT);
		String groupsToExportString = req.getParameter(PARAM_GROUPS_TO_EXPORT);

		String usersToExludeString = req.getParameter(PARAM_EXCLUDE_USERS);
		String groupsToExludeString = req.getParameter(PARAM_EXCLUDE_GROUPS);
		
		initialiseVariables();

		excludeSiteGroups = req.getParameter (PARAM_EXCLUDE_SITES_GROUPS) != null ? Boolean.parseBoolean(req.getParameter(PARAM_EXCLUDE_SITES_GROUPS)) : false;


		if(groupsToExportString!=null){
			groupsToExportList = (Arrays.asList(groupsToExportString.split(COMMA_SEPARATOR)));
			for(int i=0;i<groupsToExportList.size();i++){
				if(!groupsToExportList.get(i).startsWith(PermissionService.GROUP_PREFIX)){
					groupsToExportList.set(i,PermissionService.GROUP_PREFIX+groupsToExportList.get(i));
				}
			}
		}

		if(groupsToExludeString!=null){
			groupsToExcludeList = (Arrays.asList(groupsToExludeString.split(COMMA_SEPARATOR)));
			for(int i=0;i<groupsToExcludeList.size();i++){
				if(!groupsToExcludeList.get(i).startsWith(PermissionService.GROUP_PREFIX)){
					groupsToExcludeList.set(i,PermissionService.GROUP_PREFIX+groupsToExcludeList.get(i));
				}
			}
		}

		if(usersToExportString!=null){
			usersToExportList = Arrays.asList(usersToExportString.split(COMMA_SEPARATOR));
		}

		if(usersToExludeString!=null){	
			usersToExcludeList = Arrays.asList(usersToExludeString.split(COMMA_SEPARATOR));
		}

		if(logger.isDebugEnabled()){
			logger.debug("USERSTOEXPORT: "+usersToExportList);
			logger.debug("GROUPSTOEXPORT: "+groupsToExportList);
			logger.debug("USERSTOEXLUDE: "+usersToExcludeList);
			logger.debug("GROUPSTOEXLUDE: "+groupsToExcludeList);
			logger.debug("EXCLUDESITEGROUPS: "+excludeSiteGroups);
		}

		ZipOutputStream mainZip = new ZipOutputStream(res.getOutputStream());

		CloseIgnoringOutputStream outputForNesting = new CloseIgnoringOutputStream(mainZip); 

		mainZip.putNextEntry(new ZipEntry(PEOPLE_ACP));

		doPeopleACPExport(outputForNesting);

		RepositoryAuthenticationDao authenticationDao = null;
		for(String contextName : authenticationContextManager.getInstanceIds())
		{
			ApplicationContext ctx = authenticationContextManager.getApplicationContext(contextName);
			try 
			{
				authenticationDao = (RepositoryAuthenticationDao)ctx.getBean(RepositoryAuthenticationDao.class);
			} catch(NoSuchBeanDefinitionException e) {
				logger.error("No authenticationDao",e);

			}
		}
		if (authenticationDao == null)
		{
			mainZip.putNextEntry(new ZipEntry("Users_Skipped_As_Wrong_Authentication.txt"));
			String text = 	"Users were not exported because the Authentication\n"+
					"Subsystem you are using is not repository based";
			outputForNesting.write(text.getBytes("ASCII"));
		}
		else
		{
			mainZip.putNextEntry(new ZipEntry(USERS_ACP));
			doUsersACPExport(outputForNesting, authenticationDao);
		}

		try {
			mainZip.putNextEntry(new ZipEntry(GROUP_JSON));
			doGroupsACPExport(outputForNesting);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mainZip.close();
	}




	private void initialiseVariables() {
		usersToExcludeList = new ArrayList<String>();
		groupsToExcludeList = new ArrayList<String>();
		usersToExportList = new ArrayList<String>();
		groupsToExportList = new ArrayList<String>();
		excludeSiteGroups = false;
	}




	protected void doPeopleACPExport(CloseIgnoringOutputStream writeTo) throws IOException
	{
		List<NodeRef> nrList = new ArrayList<NodeRef>();

		if(!usersToExportList.isEmpty()){
			for(String user:usersToExportList){
				NodeRef nr = personService.getPersonOrNull(user);
				if(nr!=null){
					nrList.add(nr);
				}
			}
		}
		else{
			PagingResults<PersonInfo> result = personService.getPeople(null, null, null, new PagingRequest(Integer.MAX_VALUE, null));
			List<PersonInfo> piList = result.getPage();

			for(PersonInfo pi : piList){
				if (pi!=null && includeUser(pi.getUserName())){
					nrList.add(pi.getNodeRef());
				}
			}
		}
		
		logger.debug("EXPORTING PEOPLE: "+nrList);
		
		// Build the parameters
		ExporterCrawlerParameters parameters = new ExporterCrawlerParameters();
		parameters.setExportFrom(new Location(nrList.toArray(new NodeRef[nrList.size()])));
		parameters.setCrawlChildNodes(true);
		parameters.setCrawlSelf(true);
		parameters.setCrawlContent(true);
		parameters.setCrawlAssociations(true);


		// And the export handler
		ACPExportPackageHandler handler = new ACPExportPackageHandler(
				writeTo,
				new File("people.xml"),
				new File("people"),
				mimetypeService);

		// Do the export
		exporterService.exportView(handler, parameters, null);
	}

	protected void doUsersACPExport(CloseIgnoringOutputStream writeTo,RepositoryAuthenticationDao authenticationDao) throws IOException
	{
		List<NodeRef> exportNodes = new ArrayList<NodeRef>();

		// If explicitely set users to import just import them
		if(!usersToExportList.isEmpty()){
			for(String user:usersToExportList){
				NodeRef userNodeRef = authenticationDao.getUserOrNull(user);
				if(userNodeRef!=null){
					exportNodes.add(userNodeRef);
				}
			}
		}
		else{
			// Identify all the users
			List<String> auths = authorityService.getAuthorities(AuthorityType.USER, null, null, false, false, new PagingRequest(0, Integer.MAX_VALUE, null)).getPage();
			// Now export them, and only them
			for (String user : auths)
			{
				if (includeUser(user))
				{
					NodeRef userNodeRef = authenticationDao.getUserOrNull(user);
					if(userNodeRef!=null){
						exportNodes.add(userNodeRef);
					}
				}
			}
		}

		logger.debug("EXPORTING USERS: "+exportNodes);

		// Build the parameters
		ExporterCrawlerParameters parameters = new ExporterCrawlerParameters();
		parameters.setExportFrom(new Location(exportNodes.toArray(new NodeRef[exportNodes.size()])));
		parameters.setCrawlChildNodes(true);
		parameters.setCrawlSelf(true);
		parameters.setCrawlContent(true);
		parameters.setCrawlAssociations(true);

		// And the export handler
		ACPExportPackageHandler handler = new ACPExportPackageHandler(writeTo,new File("users.xml"),new File("users"),mimetypeService);

		// Do the export
		exporterService.exportView(handler, parameters, null);
	}



	private boolean includeUser(String user) {
		return !USERS_NOT_TO_EXPORT.contains(user) && !usersToExcludeList.contains(user) && (usersToExportList.contains(user) || usersToExportList.isEmpty());
	}



	protected void doGroupsACPExport(CloseIgnoringOutputStream writeTo) throws IOException, JSONException
	{
		JSONArray rootGroupList = new JSONArray();

		Set<String> rootAuthorities = authorityService.getAllRootAuthorities(AuthorityType.GROUP);
		for(String rootAuthority :rootAuthorities ){
			if(includeGroup(rootAuthority)){
				rootGroupList.put(createRecursiveGroupObject(rootAuthority));
			}
		}
		PrintWriter out = new PrintWriter(new OutputStreamWriter(writeTo, "UTF-8"));

		String prettyJsonString = prettifyJsonString(rootGroupList.toString());

		out.print(prettyJsonString);
		out.close();
	}



	private String prettifyJsonString(String list1) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(list1);
		String prettyJsonString = gson.toJson(je);
		return prettyJsonString;
	}

	private JSONObject createRecursiveGroupObject(String group) throws JSONException{

		logger.debug("EXPORTING GROUP "+group);
		
		JSONObject jgroup = new JSONObject();
		JSONArray jsubgroups = new JSONArray();

		String displayName = authorityService.getAuthorityDisplayName(group);
		JSONArray zones = createZones(authorityService.getAuthorityZones(group));
		JSONArray members =createMembers(authorityService.getContainedAuthorities(AuthorityType.USER, group, true));
		
		Set<String> containedGroups =authorityService.getContainedAuthorities(AuthorityType.GROUP, group, true);

		for(String containedGroup : containedGroups){
			if(includeGroup(containedGroup)){
				jsubgroups.put(createRecursiveGroupObject(containedGroup));
			}
		}

		jgroup.put(Constants.GROUP_NAME, group);
		jgroup.put(Constants.GROUP_DISPLAYNAME, displayName);
		jgroup.put(Constants.GROUP_ZONES, zones);
		jgroup.put(Constants.GROUP_MEMBERS, members);
		jgroup.put(Constants.GROUP_SUBGROUPS, jsubgroups);

		return jgroup;

	}

	private JSONArray createZones(Set<String> zones) throws JSONException{
		JSONArray jzones = new JSONArray();
		for(String zone:zones){
			jzones.put(zone);
		}
		return jzones;
	}

	private JSONArray createMembers(Set<String> members) throws JSONException{
		JSONArray jmembers = new JSONArray();
		for(String member:members){
				jmembers.put(member);
		}
		return jmembers;
	}


	private boolean includeGroup(String group) {
		return (!groupsToExcludeList.contains(group) && !(excludeSiteGroups && group.startsWith(GROUP_SITE_PREFIX) && (groupsToExportList.isEmpty() || groupsToExportList.contains(group))));
	}



	protected static class CloseIgnoringOutputStream extends FilterOutputStream
	{
		public CloseIgnoringOutputStream(OutputStream out)
		{
			super(out);
		}

		@Override
		public void close() throws IOException
		{
			// Flushes, but doesn't close
			flush();
		}
	}



	public void setAuthenticationContextManager(ChildApplicationContextManager authenticationContextManager)
	{
		this.authenticationContextManager = authenticationContextManager;
	}

	public void setExporterService(ExporterService exporterService)
	{
		this.exporterService = exporterService;
	}

	public void setMimetypeService(MimetypeService mimetypeService)
	{
		this.mimetypeService = mimetypeService;
	}

	public void setAuthorityService(AuthorityService authorityService)
	{
		this.authorityService = authorityService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}
}
