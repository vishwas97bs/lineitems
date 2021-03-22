package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Constants;

import java.util.List;
import java.util.Map;


public abstract class Domain
{
    public String[] defaultSyntaxesInOrder;
    public Constants.LINE_ITEM_FIELDS[] defaultFields;
    public Constants.DOMAIN_NAME name;


    public Constants.DOMAIN_NAME getName()
    {
        return name;
    }


    public void setName( Constants.DOMAIN_NAME name )
    {
        this.name = name;
    }


    public String[] getSyntaxesInOrder()
    {
        return defaultSyntaxesInOrder;
    }


    public Constants.LINE_ITEM_FIELDS[] getDefaultFields()
    {
        return defaultFields;
    }


    public abstract void performNextLineOperations( DocumentMetaData metaData, LineItem currentLine, LineItem previousLine,
        List<LineItem> lineObjects, LineItemHelper lineItemHelper, Map<String, Object> configuration,
        FieldConfiguration fieldConfiguration );
}
