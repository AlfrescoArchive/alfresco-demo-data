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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.view.ImporterBinding.UUID_BINDING;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.descriptor.DescriptorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.I18NUtil;


public class UsersImporterPatch extends AbstractPatch{

	public static final String PROPERTIES_USERS = "users";
	public static final String PROPERTIES_PEOPLE = "people";
	public static final String PROPERTY_LOCATION = "location";

	private AttributeService attributeService;
	private static final Serializable[] IMPORTED_USERS = new Serializable[] {"DDImportedUsers"};

	private static final Map<String,String> DEFAULT_PATHS = new HashMap<String, String>();
	static {
		DEFAULT_PATHS.put(PROPERTIES_USERS, "/${alfresco_user_store.system_container.childname}/${alfresco_user_store.user_container.childname}"); 
		DEFAULT_PATHS.put(PROPERTIES_PEOPLE, "/${system.system_container.childname}/${system.people_container.childname}"); 
	}

	private static final String MSG_NO_BOOTSTRAP_VIEWS_GIVEN = "patch.siteLoadPatch.noBootstrapViews";

	private static final Log logger = LogFactory.getLog(UsersImporterPatch.class);

	private DescriptorService descriptorService;

	private ImporterBootstrap spacesBootstrap;
	private ImporterBootstrap usersBootstrap;


	private Map<String,Properties> bootstrapViews;

	private Boolean disabled = false;

	public UsersImporterPatch()
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
				logger.debug("Load of users is disabled.");
			}
			return "Load of users is disabled.";
		}

		return applyInternalImpl();
	}

	private String applyInternalImpl() throws Exception
	{

		Boolean importedUsers = (Boolean)attributeService.getAttribute(IMPORTED_USERS);
		if(importedUsers==null || importedUsers.booleanValue()==false){
			

			if(descriptorService != null)
			{
				// if the descriptor service is wired up only load at install time (and not on upgrade)
				Descriptor installed = descriptorService.getInstalledRepositoryDescriptor();
				Descriptor live = descriptorService.getServerDescriptor();

				if(!installed.getVersion().equals(live.getVersion()))
				{
					return "Users not created.";
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

			logger.info("[DEMO-DATA] Importing Users");

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

			try{
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
				attributeService.setAttribute(Boolean.TRUE, IMPORTED_USERS);
			}
			catch(AlfrescoRuntimeException ine){
				logger.warn("this patch might have already run -- continue -- set DEBUG on org.alfresco.repo.admin.patch if it's not the case",ine);
			}

			return "Users loaded";
		}
		return "Users already imported";

	}


	public void setAttributeService(AttributeService attributeService) {
		this.attributeService = attributeService;
	}











}
