package org.alfresco.devops.importers;

import org.alfresco.repo.admin.patch.impl.SiteLoadPatch;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class PostSiteLoadPatch extends SiteLoadPatch implements ApplicationListener<ContextRefreshedEvent>{

	private boolean onContextRefreshedEvent=false;
	private static final Log logger = LogFactory.getLog(PostSiteLoadPatch.class);



	public PostSiteLoadPatch() {
		super();
		onContextRefreshedEvent=false;
	}


	@Override
	protected String applyInternal() throws Exception
	{
		if(onContextRefreshedEvent){
			return super.applyInternal();
		}
		else{
			return "This patch will be applied at Spring ContextRefreshedEvent";
		}
	}


	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		onContextRefreshedEvent=true;
		RetryingTransactionCallback<String> txnWork = new RetryingTransactionCallback<String>(){
			public String execute() throws Exception
			{
				return applyInternal();
			}
		};
		transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
	}

}
