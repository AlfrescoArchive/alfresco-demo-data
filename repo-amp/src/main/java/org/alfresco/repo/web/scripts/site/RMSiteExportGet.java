package org.alfresco.repo.web.scripts.site;

import java.io.File;
import java.io.IOException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;

public class RMSiteExportGet extends SiteExportGet{

	private MimetypeService mimetypeService;
	private ExporterService exporterService;


	@Override
	protected void doSiteACPExport(SiteInfo site, CloseIgnoringOutputStream writeTo) throws IOException
	{
		// Build the parameters
		ExporterCrawlerParameters parameters = new ExporterCrawlerParameters();
		parameters.setExportFrom(new Location(site.getNodeRef()));
		parameters.setCrawlChildNodes(true);
		parameters.setCrawlSelf(true);
		parameters.setCrawlContent(true);
		//removing sys:incomplete otherwise it would throw an exception during the import
		QName[] aspectsToExclude = {ContentModel.ASPECT_INCOMPLETE};
		parameters.setExcludeAspects(aspectsToExclude);

		// And the export handler
		ACPExportPackageHandler handler = new ACPExportPackageHandler(
				writeTo,
				new File(site.getShortName() + ".xml"),
				new File(site.getShortName()),
				mimetypeService);

		// Do the export
		exporterService.exportView(handler, parameters, null);
	}
	
	public void setExporterService(ExporterService exporterService)
    {
        this.exporterService = exporterService;
    }
    
    public void setMimetypeService(MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }


}
