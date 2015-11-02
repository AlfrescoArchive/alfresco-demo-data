package org.alfresco.devops.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.devops.util.Constants;
import org.alfresco.repo.admin.patch.impl.SiteLoadPatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

public class SitePatchLoaderPostProcessor implements BeanDefinitionRegistryPostProcessor {


	private ResourcesResolver resourcesResolver;
    private Boolean disabled = false;
	private String parentName;
	private String patchId;
	private String beanId;
	private String sitesContentLocation;
	
	private static Log logger = LogFactory.getLog(SitePatchLoaderPostProcessor.class);



	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
			throws BeansException {

		if(!disabled){
			Set<String> acps;
			try {
				acps = resourcesResolver.resolveResourcesFromAMP(sitesContentLocation);
				for(String acp:acps){
					String[] split = acp.split("/");
					String siteName = split[split.length-2];
					BeanDefinitionBuilder beanDefinition = getSiteBeanDefinition(acp, siteName);
					registry.registerBeanDefinition(patchId+siteName, beanDefinition.getBeanDefinition() );
				}
			} catch (IOException e) {
				logger.error("Exception",e);
			}
		}
	}


	private BeanDefinitionBuilder getSiteBeanDefinition(String acp,String siteName) {
		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(SiteLoadPatch.class); 
		beanDefinition.setParentName(parentName);
		beanDefinition.addPropertyValue(Constants.ID, beanId+siteName);
		beanDefinition.addPropertyValue(Constants.DISABLED, false);
		beanDefinition.addPropertyValue(Constants.SITENAME, siteName);
		Map<String,Properties> bootstrapViews = new HashMap<String,Properties>();
		Properties p = new Properties();
		p.setProperty(Constants.LOCATION, acp);
		bootstrapViews.put(Constants.CONTENTS, p);
		beanDefinition.addPropertyValue(Constants.BOOTSTRAPVIEWS, bootstrapViews);
		return beanDefinition;
	}

	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	public void setResourcesResolver(ResourcesResolver resourcesResolver) {
		this.resourcesResolver = resourcesResolver;
	}


	public void setDisabled(boolean disabled)
	{
		this.disabled = disabled;
	}
	
	public boolean isDisabled() {
		return disabled;
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

	public void setSitesContentLocation(String sitesContentLocation) {
		this.sitesContentLocation = sitesContentLocation;
	}


}