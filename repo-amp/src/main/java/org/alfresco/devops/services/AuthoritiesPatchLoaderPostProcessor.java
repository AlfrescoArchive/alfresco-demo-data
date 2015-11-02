package org.alfresco.devops.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.alfresco.devops.importers.UsersGroupsImporterPatch;
import org.alfresco.devops.util.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.util.ResourceUtils;

public class AuthoritiesPatchLoaderPostProcessor implements BeanDefinitionRegistryPostProcessor {


	private Boolean disabled = false;
	private String usersLocation;
	private String peopleLocation;
	private String groupsLocation;
	private String parentName;
	private String patchId;
	private String beanId;
	private ResourcesResolver resourcesResolver;




	private static Log logger = LogFactory.getLog(AuthoritiesPatchLoaderPostProcessor.class);



	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
			throws BeansException {


		BeanDefinitionBuilder beanDefinition = getAuthoritiesBeanDefinition();
		registry.registerBeanDefinition(beanId, beanDefinition.getBeanDefinition() );

	}

	private BeanDefinitionBuilder getAuthoritiesBeanDefinition() {
		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(UsersGroupsImporterPatch.class); 

		beanDefinition.setParentName(parentName);
		beanDefinition.addPropertyValue(Constants.ID, patchId);
		beanDefinition.addPropertyValue(Constants.DISABLED, disabled);
		Map<String,Properties> bootstrapViews = new HashMap<String,Properties>();

		try {
			if(resourcesResolver.existsResource(usersLocation)){
				Properties users = new Properties();
				users.setProperty(Constants.LOCATION, usersLocation);
				bootstrapViews.put(Constants.USERS, users);
			}
		} catch (IOException e) {
			logger.warn("Resource "+usersLocation+" not found");
		}

		try {
			if(resourcesResolver.existsResource(peopleLocation)){
				Properties people = new Properties();
				people.setProperty(Constants.LOCATION, peopleLocation);
				bootstrapViews.put(Constants.PEOPLE, people);
			}
		} catch (IOException e) {
			logger.warn("Resource "+peopleLocation+" not found");
		}

		try {
			if(resourcesResolver.existsResource(groupsLocation)){
				Properties groups = new Properties();
				groups.setProperty(Constants.LOCATION, groupsLocation);
				bootstrapViews.put(Constants.GROUPS, groups);
			}
		} catch (IOException e) {
			logger.warn("Resource "+groupsLocation+" not found");
		}

		beanDefinition.addPropertyValue(Constants.BOOTSTRAPVIEWS, bootstrapViews);
		return beanDefinition;
	}

	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	public void setDisabled(boolean disabled)
	{
		this.disabled = disabled;
	}

	public boolean isDisabled() {
		return disabled;
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

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public void setPatchId(String patchId) {
		this.patchId = patchId;
	}

	public void setBeanId(String beanId) {
		this.beanId = beanId;
	}

	public void setResourcesResolver(ResourcesResolver resourcesResolver) {
		this.resourcesResolver = resourcesResolver;
	}



}