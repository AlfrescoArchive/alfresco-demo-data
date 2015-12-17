package org.alfresco.devops.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.devops.importers.PostSiteLoadPatch;
import org.alfresco.devops.importers.RMSitePatch;
import org.alfresco.devops.importers.UsersGroupsImporterPatchOLD;
import org.alfresco.devops.importers.UsersGroupsImporterPatch;
import org.alfresco.devops.util.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

public class DynamicBootstrapPatchPostProcessor implements BeanDefinitionRegistryPostProcessor {


	private ResourcesResolver resourcesResolver;

	private Boolean sitesDisabled = false;
	private Boolean authoritiesDisabled = false;
	private Boolean rmSiteImportDisabled=false;
	private Boolean rmFixDisabled = false;

	private String sitesContentLocation;
	private String usersLocation;
	private String peopleLocation;
	private String groupsLocation;


	private static Log logger = LogFactory.getLog(DynamicBootstrapPatchPostProcessor.class);



	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

		if(!sitesDisabled){
			try {
				Set<String> acps = resourcesResolver.resolveResourcesFromAMP(sitesContentLocation);
				for(String acp:acps){
					String siteName = getSiteNameFromAcp(acp);
					if(siteName.equalsIgnoreCase("RM") && rmSiteImportDisabled){
						continue;
					}
					if(!siteName.isEmpty() ){
						BeanDefinitionBuilder beanDefinition = getSiteBeanDefinition(acp, siteName);
						registry.registerBeanDefinition(Constants.SITES_BEAN_ID+siteName, beanDefinition.getBeanDefinition() );
					}
				}
			} catch (IOException e) {
				logger.error("Exception",e);
			}
		}

		if(!rmFixDisabled){
			BeanDefinitionBuilder beanDefinition = getRMSiteFixBeanDefinition();
			registry.registerBeanDefinition(Constants.RM_FIX_BEAN_ID, beanDefinition.getBeanDefinition());
		}

		if(!authoritiesDisabled){
			BeanDefinitionBuilder beanDefinition = getAuthoritiesBeanDefinition();
			registry.registerBeanDefinition(Constants.AUTHORITIES_BEAN_ID, beanDefinition.getBeanDefinition() );
		}
	}

	private String getSiteNameFromAcp(String acp) {
		String siteName = "";
		String[] split = acp.split("/");
		if(split.length>2){
			siteName = split[split.length-2];
		}
		return siteName;
	}

	private BeanDefinitionBuilder getAuthoritiesBeanDefinition() {

		Map<String,Properties> bootstrapViews = new HashMap<String,Properties>();

		try {
			if(resourcesResolver.existsResource(usersLocation)){
				Properties users = new Properties();
				users.setProperty(Constants.LOCATION, usersLocation);
				bootstrapViews.put(Constants.USERS, users);
			}
			
			if(resourcesResolver.existsResource(peopleLocation)){
				Properties people = new Properties();
				people.setProperty(Constants.LOCATION, peopleLocation);
				bootstrapViews.put(Constants.PEOPLE, people);
			}

			if(resourcesResolver.existsResource(groupsLocation)){
				Properties groups = new Properties();
				groups.setProperty(Constants.LOCATION, groupsLocation);
				bootstrapViews.put(Constants.GROUPS, groups);
			}
		}catch(IOException ex){
			logger.error("One or more authorities resources (acp/json) could not be found");
		}

		if(!bootstrapViews.isEmpty()){
			BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(UsersGroupsImporterPatch.class); 
			beanDefinition.setParentName(Constants.AUTHORITIES_PARENT_BEAN);
			beanDefinition.addPropertyValue(Constants.ID, Constants.AUTHORITIES_PATCH_ID);
			beanDefinition.addPropertyValue(Constants.DISABLED, authoritiesDisabled);		
			beanDefinition.addPropertyValue(Constants.BOOTSTRAPVIEWS, bootstrapViews);
			return beanDefinition;
		}
		else{
			return null;
		}
	}


	private BeanDefinitionBuilder getSiteBeanDefinition(String acp,String siteName) {
		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(PostSiteLoadPatch.class); 
		beanDefinition.setParentName(Constants.SITES_PARENT_BEAN);
		beanDefinition.addPropertyValue(Constants.ID, Constants.SITES_PATCH_ID+siteName);
		beanDefinition.addPropertyValue(Constants.DISABLED, false);
		beanDefinition.addPropertyValue(Constants.SITENAME, siteName);
		Map<String,Properties> bootstrapViews = new HashMap<String,Properties>();
		Properties p = new Properties();
		p.setProperty(Constants.LOCATION, acp);
		bootstrapViews.put(Constants.CONTENTS, p);
		beanDefinition.addPropertyValue(Constants.BOOTSTRAPVIEWS, bootstrapViews);
		return beanDefinition;
	}

	private BeanDefinitionBuilder getRMSiteFixBeanDefinition() {
		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(RMSitePatch.class); 
		beanDefinition.setParentName(Constants.RM_FIX_PARENT_BEAN);
		return beanDefinition;
	}

	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		//do nothing
	}

	public void setResourcesResolver(ResourcesResolver resourcesResolver) {
		this.resourcesResolver = resourcesResolver;
	}


	public void setSitesDisabled(boolean sitesDisabled)
	{
		this.sitesDisabled = sitesDisabled;
	}

	public boolean isSitesDisabled() {
		return sitesDisabled;
	}


	public void setSitesContentLocation(String sitesContentLocation) {
		this.sitesContentLocation = sitesContentLocation;
	}

	public void setSitesDisabled(Boolean sitesDisabled) {
		this.sitesDisabled = sitesDisabled;
	}

	public void setAuthoritiesDisabled(Boolean authoritiesDisabled) {
		this.authoritiesDisabled = authoritiesDisabled;
	}

	public void setRmFixDisabled(Boolean rmFixDisabled) {
		this.rmFixDisabled = rmFixDisabled;
	}

	public void setUsersLocation(String usersLocation) {
		this.usersLocation = usersLocation;
	}

	public void setPeopleLocation(String peopleLocation) {
		this.peopleLocation = peopleLocation;
	}

	public void setGroupsLocation(String groupsLocation) {
		this.groupsLocation = groupsLocation;
	}

	public void setRmSiteImportDisabled(Boolean rmSiteImportDisabled) {
		this.rmSiteImportDisabled = rmSiteImportDisabled;
	}


}