package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;


import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


public class LineItemResponse
{
    private List<LineItem> lineItems;
    private float confidence;

    private Utils utils;

    @Autowired
    public void setUtils(Utils utils) {
        this.utils = utils;
    }

    @Override
    public String toString()
    {
        return "LineItemResponse{" + "lineItems=" + lineItems + ", confidence=" + confidence + '}';
    }


    public List<LineItem> getLineItems()
    {
        return lineItems;
    }


    public void setLineItems( List<LineItem> lineItems )
    {
        this.lineItems = lineItems;
    }


    public float getConfidence()
    {
        return confidence;
    }


    public void setConfidence( float confidence )
    {
        this.confidence = utils.roundUptoNDecimals( Float.parseFloat( String.valueOf( confidence ) ), 2 );
    }
}
