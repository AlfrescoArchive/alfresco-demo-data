/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.devops.importers;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.devops.util.Constants;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.view.ImporterBinding.UUID_BINDING;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.descriptor.DescriptorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.extensions.surf.util.I18NUtil;


public class UsersGroupsImporterPatch extends AbstractPatch
{
	public static final String PROPERTIES_USERS = "users";
	public static final String PROPERTIES_PEOPLE = "people";
	public static final String PROPERTIES_GROUPS = "groups";
	public static final String PROPERTY_LOCATION = "location";

	private static final Map<String,String> DEFAULT_PATHS = new HashMap<String, String>();
	static {
		DEFAULT_PATHS.put(PROPERTIES_USERS, "/${alfresco_user_store.system_container.childname}/${alfresco_user_store.user_container.childname}"); 
		DEFAULT_PATHS.put(PROPERTIES_PEOPLE, "/${system.system_container.childname}/${system.people_container.childname}"); 
	}

	private static final String MSG_NO_BOOTSTRAP_VIEWS_GIVEN = "patch.siteLoadPatch.noBootstrapViews";

	private static final Log logger = LogFactory.getLog(UsersGroupsImporterPatch.class);

	private AuthorityService authorityService;
	private DescriptorService descriptorService;

	private ImporterBootstrap spacesBootstrap;
	private ImporterBootstrap usersBootstrap;
	private PersonService personService;


	private Map<String,Properties> bootstrapViews;

	private Boolean disabled = false;

	public UsersGroupsImporterPatch()
	{
		// We do need to run in our own transaction
		setRequiresTransaction(true);
	}


	public void setSpacesBootstrap(ImporterBootstrap spacesBootstrap)
	{
		this.spacesBootstrap = spacesBootstrap;
	}
	public void setUsersBootstrap(ImporterBootstrap usersBootstrap)
	{
		this.usersBootstrap = usersBootstrap;
	}

	/**
	 * Sets the details of the bootstraps to perform
	 */
	public void setBootstrapViews(Map<String,Properties> bootstrapViews)
	{
		this.bootstrapViews = bootstrapViews;
	}

	/**
	 * Sets the Authority Service to be used for groups and people
	 * 
	 * @param authorityService The Authority Service
	 */
	public void setAuthorityService(AuthorityService authorityService)
	{
		this.authorityService = authorityService;
	}

	/**
	 * @param descriptorService the descriptorService to set
	 */
	public void setDescriptorService(DescriptorService descriptorService)
	{
		this.descriptorService = descriptorService;
	}

	public void setDisabled(boolean disabled)
	{
		this.disabled = disabled;
	}

	public boolean isDisabled() {
		return disabled;
	}

	@Override
	protected void checkProperties()
	{
		super.checkProperties();
	}

	@Override
	protected String applyInternal() throws Exception
	{
		if (isDisabled()) 
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Load of groups/users is disabled.");
			}
			return "Load of groups/users is disabled.";
		}

		return applyInternalImpl();
	}

	private String applyInternalImpl() throws Exception
	{
		if(descriptorService != null)
		{
			// if the descriptor service is wired up only load at install time (and not on upgrade)
			Descriptor installed = descriptorService.getInstalledRepositoryDescriptor();
			Descriptor live = descriptorService.getServerDescriptor();

			if(!installed.getVersion().equals(live.getVersion()))
			{
				return "Groups/users not created.";
			}
		}

		if (bootstrapViews == null || bootstrapViews.size() == 0)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("No Bootstraps given to import from - bootstrap import ignored");
			}
			return I18NUtil.getMessage(MSG_NO_BOOTSTRAP_VIEWS_GIVEN);
		}

		// Setup the Importer Bootstrap Beans
		for(ImporterBootstrap bootstrap : new ImporterBootstrap[] { spacesBootstrap, usersBootstrap })
		{
			bootstrap.setAllowWrite(true);
			bootstrap.setUseExistingStore(true);
			bootstrap.setUuidBinding(UUID_BINDING.REPLACE_EXISTING);
		}

		for(String type : DEFAULT_PATHS.keySet())
		{
			Properties props = bootstrapViews.get(type);
			if(props != null && DEFAULT_PATHS.get(type) != null)
			{
				if(!props.containsKey("path"))
				{
					props.setProperty("path", DEFAULT_PATHS.get(type));
				}
			}
		}

		if(bootstrapViews.containsKey(PROPERTIES_USERS))
		{
			List<Properties> views = new ArrayList<Properties>(1);
			views.add(bootstrapViews.get(PROPERTIES_USERS));
			usersBootstrap.setBootstrapViews(views);
			usersBootstrap.bootstrap();
		}

		if(bootstrapViews.containsKey(PROPERTIES_PEOPLE))
		{
			List<Properties> views = new ArrayList<Properties>(1);
			views.add(bootstrapViews.get(PROPERTIES_PEOPLE));
			spacesBootstrap.setBootstrapViews(views);
			spacesBootstrap.bootstrap();
		}

		// Put people into groups
		if(bootstrapViews.containsKey(PROPERTIES_GROUPS))
		{
			try
			{
				doGroupImport(bootstrapViews.get(PROPERTIES_GROUPS).getProperty(PROPERTY_LOCATION));
			} 
			catch(Throwable t)
			{
				throw new AlfrescoRuntimeException("Bootstrap failed", t);
			}
		}


		return "Users/Groups loaded";
	}


	private void doGroupImport(String location) throws Throwable
	{
		File groupFile = ImporterBootstrap.getFile(location);
		JSONParser parser = new JSONParser();
		try{
			Object obj = parser.parse(new FileReader(groupFile));
			JSONArray groups = (JSONArray)obj;
			for (int i = 0; i < groups.size(); i++) {
				JSONObject group = (JSONObject) groups.get(i);
				createOrUpdateGroup(group);
			}

		}
		catch (Exception e) {
			logger.error("Group-Import failed:",e);
		}
	}

	private void createOrUpdateGroup(JSONObject group) throws JSONException {
		String name = (String) group.get(Constants.GROUP_NAME);
		String displayName = (String) group.get(Constants.GROUP_DISPLAYNAME) != null ? (String) group.get(Constants.GROUP_DISPLAYNAME) : name;

		JSONArray zones = (JSONArray) group.get(Constants.GROUP_ZONES);
		JSONArray subgroups = (JSONArray) group.get(Constants.GROUP_SUBGROUPS);
		JSONArray members = (JSONArray) group.get(Constants.GROUP_MEMBERS);

		Set<String> zonesList = new HashSet<String>();     
		if (zones != null) {
			int len = zones.size();
			for (int j=0;j<len;j++){
				if(zones.get(j)!=null){
					zonesList.add(zones.get(j).toString());
				}
			} 
		}

		Set<JSONObject> subgroupsList = new HashSet<JSONObject>();     
		if (subgroups != null) { 
			int len = subgroups.size();
			for (int j=0;j<len;j++){ 
				if(subgroups.get(j)!=null){
					subgroupsList.add((JSONObject)subgroups.get(j));
				}
			} 
		}

		Set<String> membersList = new HashSet<String>();     
		if (members != null) {
			int len = members.size();
			for (int j=0;j<len;j++){
				if(members.get(j)!=null){
					membersList.add(members.get(j).toString());
				}
			} 
		}


		if(!authorityService.authorityExists(name)){
			String replacename = name.substring(6);
			logger.debug("Creating authority: "+replacename);
			authorityService.createAuthority(AuthorityType.GROUP, replacename, displayName, zonesList);
		}else{
			logger.debug("Updating authority: "+name);
			Set<String> zonesToAdd = new HashSet<String>();
			Set<String> existingZones = authorityService.getAuthorityZones(name);
			
			for(String currentZone:zonesList){
				if(!existingZones.contains(currentZone)){
					zonesToAdd.add(currentZone);
				}
			}
			if(!zonesToAdd.isEmpty()){
				//updating zones if necessary
				authorityService.addAuthorityToZones(name, zonesToAdd);
			}
			//updating displayname
			authorityService.setAuthorityDisplayName(name, displayName);
		}

		Set<String> containedGroups = authorityService.getContainedAuthorities(AuthorityType.GROUP, name, true);
		Set<String> containedUsers = authorityService.getContainedAuthorities(AuthorityType.USER, name, true);


		for(String member:membersList){
			if(!personService.personExists(member)){
				logger.warn("Member "+member+" does not exist in the repo - cannot be added to group "+name);
			}
			else if(!containedUsers.contains(member)){
				//add authority if not there already and if exists
				authorityService.addAuthority(name, member);
			}
		}

		for(JSONObject subGroup:subgroupsList){
			//create or update subgroup
			createOrUpdateGroup(subGroup);
			String subgroupName = (String)subGroup.get(Constants.GROUP_NAME);
			if(subgroupName!= null && !containedGroups.contains(subgroupName)){
				//adding authority
				authorityService.addAuthority(name, subgroupName);
			}
		}


	}


	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}



}
