package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.commons.entities.FieldExtractionResponse;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.FieldConfiguration;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.summarizer.ReceiptLineItemSummarizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;


@Component
public class LineItemService
{

    private ReceiptLineItemSummarizer receiptLineItemSummarizer;

    @Autowired
    public void setReceiptLineItemSummarizer( ReceiptLineItemSummarizer receiptLineItemSummarizer )
    {
        this.receiptLineItemSummarizer = receiptLineItemSummarizer;
    }


    public List<FieldExtractionResponse> extractLineItems( FieldExtractionRequest fieldExtractionRequest )
    {
        ObjectMapper objectMapper = new ObjectMapper();
        FieldConfiguration fieldConfiguration = new FieldConfiguration();
        if ( fieldExtractionRequest.getMicroserviceFieldConfig() != null ) {
            try {
                fieldConfiguration = objectMapper.readValue( fieldExtractionRequest.getMicroserviceFieldConfig(),
                    FieldConfiguration.class );
            } catch ( JsonProcessingException e ) {
                e.printStackTrace();
            }
        }
        receiptLineItemSummarizer.summarize( new HashMap<String, Object>(),new HashMap<String, Object>(), fieldExtractionRequest,new HashMap<String, Object>(), fieldConfiguration );
       return  null;
    }
}
