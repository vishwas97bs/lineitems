package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;

public class LineItemHelper {
    private LineValidator lineValidator;
    private DocumentMetaData metaData;
    private FieldExtractionRequest extractionHelper;


    public LineItemHelper()
    {}


    public LineItemHelper( DocumentMetaData metaData, FieldExtractionRequest extractionData )
    {
        this.metaData = metaData;
        this.extractionHelper = extractionData;
    }


    public LineValidator getLineValidator()
    {
        return lineValidator;
    }


    public void setLineValidator( LineValidator lineValidator )
    {
        this.lineValidator = lineValidator;
    }


    public DocumentMetaData getMetaData()
    {
        return metaData;
    }


    public void setMetaData( DocumentMetaData metaData )
    {
        this.metaData = metaData;
    }


    public FieldExtractionRequest getExtractionHelper() {
        return extractionHelper;
    }

    public void setExtractionHelper(FieldExtractionRequest extractionHelper) {
        this.extractionHelper = extractionHelper;
    }
}
