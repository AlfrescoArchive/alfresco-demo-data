package org.alfresco.devops.services;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.devops.importers.GroupsImporterPatch;
import org.alfresco.devops.importers.PostSiteLoadPatch;
import org.alfresco.devops.importers.RMSitePatch;
import org.alfresco.devops.importers.UsersImporterPatch;
import org.alfresco.devops.util.Constants;
import org.alfresco.repo.admin.patch.impl.GenericBootstrapPatch;
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
	private Boolean rmFixDisabled = true;
	private Boolean modelsDisabled = false;
	private Boolean repoDisabled = false;
	private Boolean wfDisabled = false;
	private Boolean updateGroups=false;

	private String sitesContentLocation;
	private String usersLocation;
	private String peopleLocation;
	private String groupsLocation;
	private String modelsXmlLocation;
	private String modelsLabelsLocation;
	private String repoLocation;
	private String wfDefinitionsLocation;
	private String wfModelsLocation;
	private String wfLabelsLocation;
	
	private static final String LABEL_PATTERN="_[a-zA-Z]{2}$";


	private static Log logger = LogFactory.getLog(DynamicBootstrapPatchPostProcessor.class);



	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

		if(!repoDisabled){
			Set<String> reposFiles = resourcesResolver.resolveResourcesFromAMP(repoLocation);

			if(!reposFiles.isEmpty()){
				int index = repoLocation.indexOf("*");
				if(index>0){
					String location = repoLocation.substring(0, index);
					for(String fileLocation:reposFiles){
						String repoPath = fileLocation.substring(location.length());
						Path p = Paths.get(repoPath);
						if(p!=null){
							Path fileNamePath = p.getFileName();
							if(fileNamePath!=null){
								String fileName= fileNamePath.toString();
								String path = repoPath.substring(0,repoPath.length()-fileName.length());
								if (!path.startsWith("/")){
									path = "/"+ path;
								}
								if (path.endsWith("/")){
									path = path.substring(0,path.length()-1);
								}
								String linearPath = repoPath.replaceAll("[./:-]","_");
								String beanId = linearPath;
								BeanDefinitionBuilder beanDefinition = getRepoBeanDefinition(path,fileLocation,beanId);
								registry.registerBeanDefinition(beanId, beanDefinition.getBeanDefinition() );
							}
						}
					}
				}
			}
		}


		if(!modelsDisabled){
			Set<String> modelsXml = resourcesResolver.resolveResourcesFromAMP(modelsXmlLocation);
			Set<String> modelsLabels = resourcesResolver.resolveResourcesFromAMP(modelsLabelsLocation);

			if(!modelsXml.isEmpty()){
				List<String> models = new ArrayList<String>();
				List<String> labels = new ArrayList<String>();
				models.addAll(modelsXml);
				Set<String> labelSet = extractValidLabels(modelsLabels);
				labels.addAll(labelSet);


				BeanDefinitionBuilder beanDefinition = getModelBeanDefinition(models, labels);
				registry.registerBeanDefinition("demodata_models_dictionaryBootstrap", beanDefinition.getBeanDefinition() );
			}
		}

		if(!wfDisabled){
			Set<String> definitionsSet = resourcesResolver.resolveResourcesFromAMP(wfDefinitionsLocation); 
			Set<String> labelsSet = resourcesResolver.resolveResourcesFromAMP(wfLabelsLocation); 
			Set<String> modelsSet = resourcesResolver.resolveResourcesFromAMP(wfModelsLocation);

			if(!definitionsSet.isEmpty()){
				List<String> models = new ArrayList<String>();
				List<String> labels = new ArrayList<String>();
				List<String> definitions = new ArrayList<String>();

				models.addAll(modelsSet);
				definitions.addAll(definitionsSet);

				Set<String> labelSet = extractValidLabels(labelsSet);
				labels.addAll(labelSet);

				BeanDefinitionBuilder beanDefinition = getWorkflowBeanDefinition(definitions,models, labels);
				registry.registerBeanDefinition("demodata_wfss_dictionaryBootstrap", beanDefinition.getBeanDefinition() );
			}
		}

		if(!sitesDisabled){
			Set<String> acps = resourcesResolver.resolveResourcesFromAMP(sitesContentLocation);
			for(String acp:acps){
				String siteName = getSiteNameFromAcp(acp);
				boolean isRM = siteName.equalsIgnoreCase("RM");
				
				if(isRM){
					rmFixDisabled=false;
				}
				if(!siteName.isEmpty() ){
					BeanDefinitionBuilder beanDefinition = getSiteBeanDefinition(acp, siteName);
					registry.registerBeanDefinition(Constants.SITES_BEAN_ID+siteName, beanDefinition.getBeanDefinition() );
				}
			}
		}

		if(!rmFixDisabled){
			registry.registerBeanDefinition(Constants.RM_FIX_BEAN_ID, getRMSiteFixBeanDefinition().getBeanDefinition());
		}

		if(!authoritiesDisabled){
			BeanDefinitionBuilder users = getUsersBeanDefinition();
			BeanDefinitionBuilder groups = getGroupsBeanDefinition();
			if(users!=null){
				registry.registerBeanDefinition(Constants.USERS_BEAN_ID, users.getBeanDefinition() );
			}
			if(groups!=null){
				registry.registerBeanDefinition(Constants.GROUPS_BEAN_ID, groups.getBeanDefinition() );
			}
		}

	}

	private Set<String> extractValidLabels(Set<String> labelsSet) {
		Set<String> labelSet= new HashSet<String>();

		for(String ml:labelsSet){
			//labels don't have the property extension
			ml = ml.substring(0,ml.lastIndexOf(".properties"));
			//if there is a locale in the property it needs to be removed
			Pattern r = Pattern.compile(LABEL_PATTERN);
			Matcher m = r.matcher(ml);
			if(m.find()){
				ml = ml.substring(0,ml.indexOf("_"));
				labelSet.add(ml);
			}
		}
		return labelSet;
	}

	private String getSiteNameFromAcp(String acp) {
		String siteName = "";
		String[] split = acp.split("/");
		if(split.length>2){
			siteName = split[split.length-2];
		}
		return siteName;
	}

	private BeanDefinitionBuilder getUsersBeanDefinition() {

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
		}catch(IOException ex){
			logger.error("One or more authorities resources (acp/json) could not be found");
		}

		if(!bootstrapViews.isEmpty()){
			BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(UsersImporterPatch.class); 
			beanDefinition.setParentName(Constants.USERS_PARENT_BEAN);
			beanDefinition.addPropertyValue(Constants.ID, Constants.USERS_PATCH_ID);
			beanDefinition.addPropertyValue(Constants.DISABLED, authoritiesDisabled);		
			beanDefinition.addPropertyValue(Constants.BOOTSTRAPVIEWS, bootstrapViews);
			return beanDefinition;
		}
		else{
			return null;
		}
	}

	private BeanDefinitionBuilder getGroupsBeanDefinition() {

		Map<String,Properties> bootstrapViews = new HashMap<String,Properties>();

		try {
			if(resourcesResolver.existsResource(groupsLocation)){
				Properties groups = new Properties();
				groups.setProperty(Constants.LOCATION, groupsLocation);
				bootstrapViews.put(Constants.GROUPS, groups);
			}
		}catch(IOException ex){
			logger.error("One or more authorities resources (acp/json) could not be found");
		}

		if(!bootstrapViews.isEmpty()){
			BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(GroupsImporterPatch.class); 
			beanDefinition.setParentName(Constants.GROUPS_PARENT_BEAN);
			beanDefinition.addPropertyValue(Constants.ID, Constants.GROUPS_PATCH_ID);
			beanDefinition.addPropertyValue(Constants.DISABLED, authoritiesDisabled);		
			beanDefinition.addPropertyValue(Constants.BOOTSTRAPVIEWS, bootstrapViews);
			beanDefinition.addPropertyValue("updateGroups", updateGroups);
			return beanDefinition;
		}
		else{
			return null;
		}
	}

	private BeanDefinitionBuilder getModelBeanDefinition(List<String> models,List<String> labels) {
		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(); 
		beanDefinition.setParentName(Constants.DICTIONARY_MODEL_BOOTSTRAP);
		beanDefinition.addDependsOn(Constants.DICTIONARY_BOOTSTRAP);
		beanDefinition.addPropertyValue(Constants.MODELS, models);
		if(labels.size()>0){
			beanDefinition.addPropertyValue(Constants.LABELS, labels);
		}
		return beanDefinition;
	}

	private BeanDefinitionBuilder getWorkflowBeanDefinition(List<String> definitions,List<String> models,List<String> labels) {

		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(); 
		beanDefinition.setParentName(Constants.WORKFLOW_DEPLOYER);
		List<Properties> props = new ArrayList<Properties>();
		for(String definition:definitions){
			Properties p = new Properties();
			p.setProperty(Constants.ENGINE_ID, Constants.ACTIVITI);
			p.setProperty(Constants.LOCATION, definition);
			p.setProperty(Constants.MIMETYPE, "text/xml");
			p.setProperty(Constants.MIMETYPE, "false");
			props.add(p);
		}
		beanDefinition.addPropertyValue(Constants.WORKFLOW_DEFINITIONS, props);
		if(!models.isEmpty()){
			beanDefinition.addPropertyValue(Constants.MODELS, models);
		}
		if(!labels.isEmpty()){
			beanDefinition.addPropertyValue(Constants.LABELS, labels);
		}
		return beanDefinition;
	}

	private BeanDefinitionBuilder getRepoBeanDefinition(String path,String fileLocation, String id) {

		BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(GenericBootstrapPatch.class); 
		beanDefinition.setParentName(Constants.DEMODATA_REPO_PARENT_BEAN);

		beanDefinition.addPropertyValue(Constants.ID, id);

		Properties p = new Properties();
		p.setProperty(Constants.LOCATION, fileLocation);
		p.setProperty(Constants.PATH, path);
		p.setProperty(Constants.UUIDBINDING, Constants.REPLACE_EXISTING);

		beanDefinition.addPropertyValue(Constants.BOOTSTRAPVIEW, p);
		return beanDefinition;
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

	public void setUsersLocation(String usersLocation) {
		this.usersLocation = usersLocation;
	}

	public void setPeopleLocation(String peopleLocation) {
		this.peopleLocation = peopleLocation;
	}

	public void setGroupsLocation(String groupsLocation) {
		this.groupsLocation = groupsLocation;
	}


	public void setModelsXmlLocation(String modelsXmlLocation) {
		this.modelsXmlLocation = modelsXmlLocation;
	}

	public void setModelsLabelsLocation(String modelsLabelsLocation) {
		this.modelsLabelsLocation = modelsLabelsLocation;
	}

	public void setModelsDisabled(Boolean modelsDisabled) {
		this.modelsDisabled = modelsDisabled;
	}

	public void setRepoDisabled(Boolean repoDisabled) {
		this.repoDisabled = repoDisabled;
	}

	public void setRepoLocation(String repoLocation) {
		this.repoLocation = repoLocation;
	}

	public void setWfDisabled(Boolean wfDisabled) {
		this.wfDisabled = wfDisabled;
	}

	public void setWfDefinitionsLocation(String wfDefinitionsLocation) {
		this.wfDefinitionsLocation = wfDefinitionsLocation;
	}

	public void setWfModelsLocation(String wfModelsLocation) {
		this.wfModelsLocation = wfModelsLocation;
	}

	public void setWfLabelsLocation(String wfLabelsLocation) {
		this.wfLabelsLocation = wfLabelsLocation;
	}

	public void setUpdateGroups(Boolean updateGroups) {
		this.updateGroups = updateGroups;
	}
}