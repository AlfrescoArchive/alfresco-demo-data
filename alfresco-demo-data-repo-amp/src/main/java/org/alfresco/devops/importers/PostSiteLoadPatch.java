package org.alfresco.devops.importers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.patch.impl.SiteLoadPatch;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class PostSiteLoadPatch extends SiteLoadPatch implements ApplicationListener<ContextRefreshedEvent>{

	private boolean onContextRefreshedEvent=false;
	private static final Log logger = LogFactory.getLog(PostSiteLoadPatch.class);
	private String siteName;
	private AttributeService attributeService;
	private static final Serializable[] IMPORTED_SITES = new Serializable[] {"DDImportedSites"};
    private BehaviourFilter behaviourFilter;



	public PostSiteLoadPatch() {
		super();
		onContextRefreshedEvent=false;
	}


	@Override
	protected String applyInternal() throws Exception
	{
		if(onContextRefreshedEvent){
			ArrayList<String> sites = (ArrayList<String>) attributeService.getAttribute(IMPORTED_SITES);
			if(sites==null){
				sites = new ArrayList<String>();
			}
			if(!sites.contains(siteName)){
				logger.info("[DEMO-DATA] Importing Site "+siteName);
				String message =  super.applyInternal();
				sites.add(siteName);
				attributeService.setAttribute(sites, IMPORTED_SITES);
				return message;
			}else{
				return "Site "+siteName+" already installed";
			}
		}
		else{
			return "This patch will be applied at Spring ContextRefreshedEvent";
		}
	}


	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		onContextRefreshedEvent=true;
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>(){
			@Override 
			public Void doWork() throws Exception
			{
				try {
					RetryingTransactionCallback<String> txnWork = new RetryingTransactionCallback<String>(){
						public String execute() throws Exception
						{	
		                    behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
							String message = applyInternal();
		                    behaviourFilter.enableBehaviour(ContentModel.ASPECT_AUDITABLE);
		                    return message;
						}
					};
					transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false,false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.error("[DEMO-DATA] Error bootstrapping Site "+siteName,e);
				}
				return null;
			}
		});

	}



		public void setSiteName(String siteName)
		{
			this.siteName=siteName;
			super.setSiteName(siteName);
		}


		public void setAttributeService(AttributeService attributeService) {
			this.attributeService = attributeService;
		}


		public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
			this.behaviourFilter = behaviourFilter;
			super.setBehaviourFilter(behaviourFilter);
		}


	}
