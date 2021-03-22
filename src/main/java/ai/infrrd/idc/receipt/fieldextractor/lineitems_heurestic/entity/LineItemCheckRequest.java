package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

import java.util.List;
import java.util.Map;


public class LineItemCheckRequest
{
    private Map<String, Object> filters;
    private List<String> items;


    public Map<String, Object> getFilters()
    {
        return filters;
    }


    public void setFilters( Map<String, Object> filters )
    {
        this.filters = filters;
    }


    public List<String> getItems()
    {
        return items;
    }


    public void setItems( List<String> items )
    {
        this.items = items;
    }
}
