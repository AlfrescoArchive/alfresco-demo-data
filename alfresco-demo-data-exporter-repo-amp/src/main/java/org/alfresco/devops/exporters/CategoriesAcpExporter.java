package org.alfresco.devops.exporters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.Location;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class CategoriesAcpExporter extends AbstractWebScript{

	private MimetypeService mimetypeService;
	private ExporterService exporterService;
	private NodeService nodeService;
	private SearchService searchService;

	private static Log logger = LogFactory.getLog(CategoriesAcpExporter.class);


	private static final String PARAM_PATH = "path";
	private static final String PARAM_CRAWL_SELF = "crawlSelf";
	private static final String PARAM_CRAWL_CHILD_NODES = "crawlChildNodes";
	private static final String PARAM_CRAWL_CONTENT = "crawlContent";
	private static final String PARAM_CRAWL_ASSOCIATIONS = "crawlAssociations";



	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res)
			throws IOException {


		String path = req.getParameter(PARAM_PATH);

		String paramSelf = req.getParameter(PARAM_CRAWL_SELF);
		String paramChildNodes = req.getParameter(PARAM_CRAWL_CHILD_NODES);
		String paramCrawlContent = req.getParameter(PARAM_CRAWL_CONTENT);
		String paramCrawlAssociations = req.getParameter(PARAM_CRAWL_ASSOCIATIONS);

		boolean crawlSelf = isNullOrEmpty(paramSelf) ? true : Boolean.parseBoolean(paramSelf);
		boolean crawlChildNodes = isNullOrEmpty(paramChildNodes) ? true : Boolean.parseBoolean(paramChildNodes);
		boolean crawlContent = isNullOrEmpty(paramCrawlContent) ? true : Boolean.parseBoolean(paramCrawlContent);
		boolean crawlAssociations = isNullOrEmpty(paramCrawlAssociations) ? true : Boolean.parseBoolean(paramCrawlAssociations);

		try
		{
//			if(isNullOrEmpty(path)){
//				throw new WebScriptException("No Path Parameter Specified in the URL");
//			}

			List<NodeRef> nodes = new ArrayList<NodeRef>();
			

			ResultSet rs = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "lucene","PATH:\"/cm:categoryRoot\"", null);
			NodeRef nr = rs.getNodeRef(0);
			nodes.add(nr);
			rs.close();
			
			logger.debug("Exporting Parameters: crawlSelf="+crawlSelf+" crawlChildNodes="+crawlChildNodes+" crawlContent="+crawlContent+ " crawlAssociations="+crawlAssociations);

			res.setContentType(MimetypeMap.MIMETYPE_ACP);
			res.setHeader("Content-Disposition","attachment; fileName=CategoriesExport." + ACPExportPackageHandler.ACP_EXTENSION);

			NodeRef[] lnodes = nodes.toArray(new NodeRef[nodes.size()]);
			
			ExporterCrawlerParameters parameters = new ExporterCrawlerParameters();
			parameters.setExportFrom(new Location(lnodes));
			parameters.setCrawlChildNodes(crawlChildNodes);
			parameters.setCrawlSelf(false);
			parameters.setCrawlContent(crawlContent);
			parameters.setCrawlAssociations(crawlAssociations);

			// And the export handler
			ACPExportPackageHandler handler = new ACPExportPackageHandler(res.getOutputStream(),new File("categories.xml"),new File("categories"),mimetypeService);
			handler.setNodeService(nodeService);
			handler.setExportAsFolders(false);
			// Do the export
			exporterService.exportView(handler, parameters, null);

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



	private boolean isNullOrEmpty(String paramSelf) {
		return paramSelf == null || paramSelf.trim().isEmpty();
	}



	public void setMimetypeService(MimetypeService mimetypeService) {
		this.mimetypeService = mimetypeService;
	}



	public void setExporterService(ExporterService exporterService) {
		this.exporterService = exporterService;
	}




	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}



	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

}
