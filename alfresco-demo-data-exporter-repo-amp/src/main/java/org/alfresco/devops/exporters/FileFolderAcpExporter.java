package org.alfresco.devops.exporters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.devops.exporter.util.Utils;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.Location;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class FileFolderAcpExporter extends AbstractWebScript{

	private MimetypeService mimetypeService;
	private ExporterService exporterService;
	private FileFolderService fileFolderService;
	private NodeService nodeService;


	private static Log logger = LogFactory.getLog(FileFolderAcpExporter.class);


	private static final String PARAM_PATH = "path";
	private static final String PARAM_CRAWL_SELF = "crawlSelf";
	private static final String PARAM_CRAWL_CHILD_NODES = "crawlChildNodes";
	private static final String PARAM_CRAWL_CONTENT = "crawlContent";
	private static final String PARAM_CRAWL_ASSOCIATIONS = "crawlAssociations";
	private static Repository repositoryHelper;



	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res)
			throws IOException {


		String path = req.getParameter(PARAM_PATH);

		String paramSelf = req.getParameter(PARAM_CRAWL_SELF);
		String paramChildNodes = req.getParameter(PARAM_CRAWL_CHILD_NODES);
		String paramCrawlContent = req.getParameter(PARAM_CRAWL_CONTENT);
		String paramCrawlAssociations = req.getParameter(PARAM_CRAWL_ASSOCIATIONS);

		boolean crawlSelf = Utils.isNullOrEmpty(paramSelf) ? true : Boolean.parseBoolean(paramSelf);
		boolean crawlChildNodes = Utils.isNullOrEmpty(paramChildNodes) ? true : Boolean.parseBoolean(paramChildNodes);
		boolean crawlContent = Utils.isNullOrEmpty(paramCrawlContent) ? true : Boolean.parseBoolean(paramCrawlContent);
		boolean crawlAssociations = Utils.isNullOrEmpty(paramCrawlAssociations) ? true : Boolean.parseBoolean(paramCrawlAssociations);

		try
		{
			if(Utils.isNullOrEmpty(path)){
				throw new WebScriptException("No Path Parameter Specified in the URL");
			}

			String[] els = path.split(",");
			List<NodeRef> nodes = new ArrayList<NodeRef>();

			String exportName="multiple-contents-"+System.currentTimeMillis();

			for(String el:els){

				List<String> pathElements = new ArrayList<String>(Arrays.asList(el.split("/")));

				pathElements.removeAll(Arrays.asList("", null));

				//Will search just inside Company Home
				FileInfo fi = fileFolderService.resolveNamePath(repositoryHelper.getCompanyHome(), pathElements);

				if(els.length==1){
					exportName = fi.getName().replaceAll("[ .]", "_");
				}
				NodeRef nr  = fi.getNodeRef();
				nodes.add(nr);

				if(logger.isDebugEnabled()){
					logger.debug("Exporting "+(fi.isFolder() ? "folder " : "node '") +exportName +"' , nodeRef: "+nr);
				}
			}

			logger.debug("Exporting Parameters: crawlSelf="+crawlSelf+" crawlChildNodes="+crawlChildNodes+" crawlContent="+crawlContent+ " crawlAssociations="+crawlAssociations);

			res.setContentType(MimetypeMap.MIMETYPE_ACP);
			res.setHeader("Content-Disposition","attachment; fileName="+exportName+"." + ACPExportPackageHandler.ACP_EXTENSION);

			NodeRef[] lnodes = nodes.toArray(new NodeRef[nodes.size()]);

			ExporterCrawlerParameters parameters = new ExporterCrawlerParameters();
			parameters.setExportFrom(new Location(lnodes));
			parameters.setCrawlChildNodes(crawlChildNodes);
			parameters.setCrawlSelf(crawlSelf);
			parameters.setCrawlContent(crawlContent);
			parameters.setCrawlAssociations(crawlAssociations);

			// And the export handler
			ACPExportPackageHandler handler = new ACPExportPackageHandler(res.getOutputStream(),new File("files.xml"),new File("files"),mimetypeService);
			handler.setNodeService(nodeService);
			handler.setExportAsFolders(false);
			// Do the export
			exporterService.exportView(handler, parameters, null);

		}
		catch (FileNotFoundException e)
		{
			res.setContentType(MimetypeMap.MIMETYPE_TEXT_PLAIN);
			String text = "Path "+path+" not available \nUse existing path inside Company Home \nexamples:\n path=/Data Dictionary/Scripts\npath=/Sites/Scripts";
			byte[] bytes = text.getBytes();
			res.getOutputStream().write(bytes);
		}
		catch(WebScriptException e){
			res.setContentType(MimetypeMap.MIMETYPE_TEXT_PLAIN);
			res.getOutputStream().write(e.getMessage().getBytes());
		}

		catch (Exception e)
		{
			res.setContentType(MimetypeMap.MIMETYPE_TEXT_PLAIN);
			StringBuffer text = new StringBuffer();
			text.append("Error occurred during export:");
			text.append("\n");
			text.append(e.getMessage());
			byte[] bytes = text.toString().getBytes();
			res.getOutputStream().write(bytes);
			logger.error("Error occurred during export",e);
		}




	}



	public void setMimetypeService(MimetypeService mimetypeService) {
		this.mimetypeService = mimetypeService;
	}



	public void setExporterService(ExporterService exporterService) {
		this.exporterService = exporterService;
	}



	public static void setRepositoryHelper(Repository repositoryHelper) {
		FileFolderAcpExporter.repositoryHelper = repositoryHelper;
	}



	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}



	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

}
