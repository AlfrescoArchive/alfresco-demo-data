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
	public static final String CONTENTS = "contents";
	public static final String SITENAME = "siteName";
	public static final String MODULE_PATH="alfresco/module";
	public static final String EXTENSION_PATH="alfresco/extension";
	
	public static final String AUTHORITIES_BEAN_ID = "patch.demoData.authorities";
	public static final String SITES_BEAN_ID = "patch.demoData.sites.";
	public static final String RM_FIX_BEAN_ID = "patch.demoData.rmFix";
	
	public static final String AUTHORITIES_PATCH_ID = "patch.demoData.authorities";
	public static final String SITES_PATCH_ID = "patch.demoData.sites.";
	public static final String RM_FIX_PATCH_ID = "patch.demoData.rmFix";

	public static final String AUTHORITIES_PARENT_BEAN = "patch.authoritiesPatch.generic";
	public static final String SITES_PARENT_BEAN = "patch.siteLoadPatch.generic";
	public static final String RM_FIX_PARENT_BEAN = "patch.abstract.fixRMSite";


	private Constants(){
		throw new AssertionError();
	}

}
