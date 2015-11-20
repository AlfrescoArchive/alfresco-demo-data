package org.alfresco.devops.importers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_rm.model.RecordsManagementModel;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class RMSitePatch extends AbstractPatch implements ApplicationListener<ContextRefreshedEvent>{

	private static final Log logger = LogFactory.getLog(RMSitePatch.class);
	private boolean onContextRefreshedEvent=false;

	public RMSitePatch()
	{
		onContextRefreshedEvent=false;
	}

	@Override
	protected String applyInternal() throws Exception {

		if(!onContextRefreshedEvent){
			return "This patch will be applied at Spring ContextRefreshedEvent";
		}

		ResultSet rs=null;
		try{
			SearchParameters sp = new SearchParameters();
			sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
			sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);

			sp.setQuery("ASPECT:\"rma:extendedSecurity\"");
			rs = searchService.query(sp);

			for(NodeRef nr : rs.getNodeRefs()){
				Serializable serPropReaders = nodeService.getProperty(nr, RecordsManagementModel.PROP_READERS);
				Serializable serPropWriters = nodeService.getProperty(nr, RecordsManagementModel.PROP_WRITERS);

				if(serPropReaders instanceof String){
					Map<String, Integer> map = getMapFromString(serPropReaders);
					nodeService.setProperty(nr, RecordsManagementModel.PROP_READERS,(Serializable)map);
				}

				if(serPropWriters instanceof String){
					Map<String, Integer> map = getMapFromString(serPropWriters);
					nodeService.setProperty(nr, RecordsManagementModel.PROP_WRITERS,(Serializable)map);
				}
			}

			//			sp.setQuery("ASPECT:\"rma:unpublishedUpdate\"");
			//			rs = searchService.query(sp);
			//
			//			for(NodeRef nr : rs.getNodeRefs()){
			//				Serializable serProp = nodeService.getProperty(nr, RecordsManagementModel.PROP_UPDATED_PROPERTIES);
			//				logger.debug("NodeRef: "+nr+" propertyVal :"+serProp);;
			//			}

		}

		finally
		{
			if (rs != null)
			{
				rs.close();
			}
		}

		return "RM Fix Patch applied";
		
	}

	private Map<String, Integer> getMapFromString(Serializable prop) {
		String stringProp = (String)prop;

		//remove the brackets
		if(stringProp.startsWith("{") && stringProp.endsWith("}")){
			stringProp = stringProp.substring(1, stringProp.length()-1);
		}

		String[] csProp = stringProp.split(",");

		Map<String,Integer> map = new HashMap<String, Integer>();  
		for(String s : csProp){
			String[] el = s.split("=");
			if(el.length==2){
				try{
					map.put(el[0].trim(), Integer.parseInt(el[1].trim()));
				}catch(NumberFormatException nfe){
					logger.error(el[1]+" is not a valid number");
				}
			}
		}
		return map;
	}



	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		onContextRefreshedEvent=true;

		AuthenticationUtil.runAsSystem(new RunAsWork<Void>(){
			@Override 
			public Void doWork() throws Exception
			{
				RetryingTransactionCallback<String> txnWork = new RetryingTransactionCallback<String>(){
					public String execute() throws Exception
					{
						return applyInternal();
					}
				};
				transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
				return null;
			}
		});
	}

}
