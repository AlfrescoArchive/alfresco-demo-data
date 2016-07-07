package org.alfresco.devops.util;

public final class Constants {

	public static final String GROUP_NAME = "name";
	public static final String GROUP_DISPLAYNAME = "displayName";
	public static final String GROUP_MEMBERS = "members";
	public static final String GROUP_ZONES = "zones";
	public static final String GROUP_SUBGROUPS = "groups";
	public static final String GROUPS = "groups";
	public static final String PEOPLE = "people";
	public static final String USERS = "users";
	public static final String LOCATION = "location";
	public static final String ID = "id";
	public static final String DISABLED = "disabled";
	public static final String BOOTSTRAPVIEWS = "bootstrapViews";
	public static final String BOOTSTRAPVIEW = "bootstrapView";
	public static final String CONTENTS = "contents";
	public static final String SITENAME = "siteName";
	public static final String MODULE_PATH="alfresco/module";
	public static final String EXTENSION_PATH="alfresco/extension";
	public static final String GROUP_SITE = "GROUP_site_";
	public static final String PATH = "path";
	public static final String UUIDBINDING = "uuidBinding";
	public static final String MESSAGES = "messages";
	public static final String REPLACE_EXISTING = "REPLACE_EXISTING";
	public static final String MESSAGES_BOOTSTRAP_SPACES = "alfresco/messages/bootstrap-spaces";
	public static final String DICTIONARY_MODEL_BOOTSTRAP ="dictionaryModelBootstrap";
	public static final String WORKFLOW_DEPLOYER ="workflowDeployer";
	public static final String DICTIONARY_BOOTSTRAP = "dictionaryBootstrap";
	public static final String MODELS = "models";
	public static final String LABELS = "labels";
	public static final String WORKFLOW_DEFINITIONS = "workflowDefinitions";
	public static final String REPO_IMPORT_BEAN_ID_PREFIX = "demodata_repoImport_";
	
	public static final String ENGINE_ID ="engineId";
	public static final String ACTIVITI ="activiti";
	public static final String MIMETYPE = "mimetype";
	public static final String REDEPLOY = "redeploy";

	
	public static final String USERS_BEAN_ID = "patch.demoData.users";
	public static final String GROUPS_BEAN_ID = "patch.demoData.groups";
	public static final String SITES_BEAN_ID = "patch.demoData.sites.";
	public static final String RM_FIX_BEAN_ID = "patch.demoData.rmFix";
	
	public static final String USERS_PATCH_ID = "patch.demoData.users";
	public static final String GROUPS_PATCH_ID = "patch.demoData.groups";
	public static final String SITES_PATCH_ID = "patch.demoData.sites.";
	public static final String RM_FIX_PATCH_ID = "patch.demoData.rmFix";

	public static final String USERS_PARENT_BEAN = "patch.usersPatch.generic";
	public static final String GROUPS_PARENT_BEAN = "patch.groupsPatch.generic";
	public static final String SITES_PARENT_BEAN = "patch.siteLoadPatch.generic";
	public static final String RM_FIX_PARENT_BEAN = "patch.abstract.fixRMSite";

	public static final String DEMODATA_REPO_PARENT_BEAN = "patch.import.demodata.repo";
	
	
	private Constants(){
		throw new AssertionError();
	}

}
