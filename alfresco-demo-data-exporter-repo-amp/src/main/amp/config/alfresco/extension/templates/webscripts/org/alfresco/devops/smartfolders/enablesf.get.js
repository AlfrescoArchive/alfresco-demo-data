var ctx = Packages.org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
var smartFoldersBundle = ctx.getBean("smartFoldersBundle",org.alfresco.traitextender.SpringExtensionBundle);
if(smartFoldersBundle){
	if(args.enable && args.enable.toUpperCase() === "FALSE"){
		smartFoldersBundle.stop();
		model.enabled=false;
	}
	else{
		smartFoldersBundle.start();
		model.enabled=true;
	}
	model.message="OK";
	
}
else{
	model.enabled=false;
	model.message="Smart Folders not available";
}