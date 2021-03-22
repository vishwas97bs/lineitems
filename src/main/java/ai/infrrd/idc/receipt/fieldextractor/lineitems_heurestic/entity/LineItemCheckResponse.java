package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LineItemCheckResponse {
    private Map<String, Set<LIResponseItem>> items = new HashMap<>();


    public Map<String, Set<LIResponseItem>> getItems()
    {
        return items;
    }


    public void setItems( Map<String, Set<LIResponseItem>> items )
    {
        this.items = items;
    }


    @Override
    public String toString()
    {
        try {
            return new ObjectMapper().writeValueAsString( this );
        } catch ( JsonProcessingException e ) {
            return "{}";
        }
    }
}
