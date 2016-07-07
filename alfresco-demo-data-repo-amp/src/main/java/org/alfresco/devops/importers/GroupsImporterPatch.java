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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.devops.util.Constants;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.descriptor.DescriptorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.extensions.surf.util.I18NUtil;


public class GroupsImporterPatch extends AbstractPatch implements ApplicationListener<ContextRefreshedEvent>{

	public static final String PROPERTIES_GROUPS = "groups";
	public static final String PROPERTY_LOCATION = "location";
	private boolean onContextRefreshedEvent=false;
	private boolean updateGroups=false;
	private AttributeService attributeService;
	private static final Serializable[] IMPORTED_GROUPS = new Serializable[] {"DDImportedGroups"};



	private static final String MSG_NO_BOOTSTRAP_VIEWS_GIVEN = "patch.siteLoadPatch.noBootstrapViews";

	private static final Log logger = LogFactory.getLog(GroupsImporterPatch.class);

	private AuthorityService authorityService;
	private DescriptorService descriptorService;
	private SiteService siteService;

	private PersonService personService;


	private Map<String,Properties> bootstrapViews;

	private Boolean disabled = false;

	public GroupsImporterPatch()
	{
		// We do need to run in our own transaction
		setRequiresTransaction(true);
		onContextRefreshedEvent=false;
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
		if(!onContextRefreshedEvent){
			return "Groups will be loaded later";
		}
		if (isDisabled()) 
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Load of groups is disabled.");
			}
			return "Load of groups is disabled.";
		}

		return applyInternalImpl();
	}

	private String applyInternalImpl() throws Exception
	{
		Boolean importedGroups = (Boolean)attributeService.getAttribute(IMPORTED_GROUPS);
		if(importedGroups==null){

			if(descriptorService != null)
			{
				// if the descriptor service is wired up only load at install time (and not on upgrade)
				Descriptor installed = descriptorService.getInstalledRepositoryDescriptor();
				Descriptor live = descriptorService.getServerDescriptor();

				if(!installed.getVersion().equals(live.getVersion()))
				{
					return "Groups not created.";
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

			try{

				// Put people into groups
				if(bootstrapViews.containsKey(PROPERTIES_GROUPS))
				{
					doGroupImport(bootstrapViews.get(PROPERTIES_GROUPS).getProperty(PROPERTY_LOCATION));
					attributeService.setAttribute(Boolean.TRUE, IMPORTED_GROUPS);
				}
			}
			catch(AlfrescoRuntimeException ine){
				logger.warn("this patch might have already run -- continue -- set DEBUG on org.alfresco.repo.admin.patch if it's not the case");
			}

			return "Groups loaded";
		}
		return "Groups already loaded";
	}

	@SuppressWarnings("unchecked")
	private void doGroupImport(String location)
	{
		logger.info("[DEMO-DATA] Importing Groups");
		File groupFile = ImporterBootstrap.getFile(location);
		JSONParser parser = new JSONParser();
		try{
			Object obj = parser.parse(new FileReader(groupFile));
			JSONArray groups = (JSONArray)obj;
			JSONArray includedGroups = new JSONArray();

			List<String> includedGroupsName = new ArrayList<String>();
			for (int i = 0; i < groups.size(); i++) {
				JSONObject group = (JSONObject) groups.get(i);
				if(includeGroup(group)){

					createOrUpdateRootGroup(group);
					includedGroups.add(group);
					includedGroupsName.add((String)group.get(Constants.GROUP_NAME));
				}
			}
			//add subgroups for included groups
			for (int i = 0; i < includedGroups.size(); i++) {
				JSONObject group = (JSONObject) includedGroups.get(i);
				associateSubGroups(group,includedGroupsName);
			}
		}

		catch (Exception e) {
			logger.error("Group-Import failed:",e);
		}
	}

	private boolean includeGroup(JSONObject group){
		String name = (String) group.get(Constants.GROUP_NAME);
		//is it a site group?
		if(name.startsWith(Constants.GROUP_SITE)){
			String siteName = siteService.resolveSite(name);
			//don't include the site group if the site does not exist in the repo
			if(siteService.getSite(siteName)==null){
				return false;
			}
		}
		return true;
	}


	private void createOrUpdateRootGroup(JSONObject group) throws JSONException {
		String name = (String) group.get(Constants.GROUP_NAME);

		String displayName = (String) group.get(Constants.GROUP_DISPLAYNAME) != null ? (String) group.get(Constants.GROUP_DISPLAYNAME) : name;
		JSONArray zones = (JSONArray) group.get(Constants.GROUP_ZONES);
		JSONArray members = (JSONArray) group.get(Constants.GROUP_MEMBERS);

		Set<String> zonesList = getStringSetFromJArray(zones);
		Set<String> membersList = getStringSetFromJArray(members);


		if(!authorityService.authorityExists(name)){
			// remove 'GROUP_'
			String replacename = name.substring(6);

			logger.debug("Creating authority: "+replacename);
			authorityService.createAuthority(AuthorityType.GROUP, replacename, displayName, zonesList);
		}else{
			if(updateGroups){
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
		}

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

	}


	private void associateSubGroups(JSONObject group,List<String> includedGroupsName) throws JSONException {
		String name = (String) group.get(Constants.GROUP_NAME);
		JSONArray subgroups = (JSONArray) group.get(Constants.GROUP_SUBGROUPS);

		Set<String> subgroupsList = getStringSetFromJArray(subgroups);

		Set<String> containedGroups = authorityService.getContainedAuthorities(AuthorityType.GROUP, name, true);

		for(String subGroup:subgroupsList){
			//create or update subgroup
			if(subGroup!= null && !containedGroups.contains(subGroup) && includedGroupsName.contains(subGroup)){
				//adding authority
				authorityService.addAuthority(name, subGroup);
			}
		}

	}


	private Set<String> getStringSetFromJArray(JSONArray zones) {
		Set<String> zonesList = new HashSet<String>();     
		if (zones != null) {
			int len = zones.size();
			for (int j=0;j<len;j++){
				if(zones.get(j)!=null){
					zonesList.add(zones.get(j).toString());
				}
			} 
		}
		return zonesList;
	}


	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}


	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// TODO Auto-generated method stub

		onContextRefreshedEvent=true;

		AuthenticationUtil.runAsSystem(new RunAsWork<Void>(){
			@Override 
			public Void doWork() throws Exception
			{
				try {
					RetryingTransactionCallback<String> txnWork = new RetryingTransactionCallback<String>(){
						public String execute() throws Exception
						{
							return applyInternal();
						}
					};
					transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
				} catch (Exception e) {
					logger.error("-- Error bootstrapping --",e);
				}
				return null;
			}
		});

	}



	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}


	public void setUpdateGroups(boolean updateGroups) {
		this.updateGroups = updateGroups;
	}


	public void setAttributeService(AttributeService attributeService) {
		this.attributeService = attributeService;
	}




}
