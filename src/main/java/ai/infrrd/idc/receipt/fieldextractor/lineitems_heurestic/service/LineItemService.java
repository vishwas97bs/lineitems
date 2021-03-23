package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.commons.entities.FieldExtractionResponse;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.FieldConfiguration;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.summarizer.ReceiptLineItemSummarizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        List<FieldExtractionResponse> responseList = new ArrayList<>();
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
        List<FieldExtractionResponse> extractedValues = receiptLineItemSummarizer.summarize( fieldExtractionRequest,
            fieldConfiguration );
        if ( extractedValues != null && !extractedValues.isEmpty() ) {
            FieldExtractionResponse fieldExtractionResponse = new FieldExtractionResponse();
            fieldExtractionResponse.setListOfValues( extractedValues );
            fieldExtractionResponse.setFieldName( fieldExtractionRequest.getFieldConfigDetails().get( 0 ).getFieldName() );
            fieldExtractionResponse.setSuccess( true );
            responseList.add( fieldExtractionResponse );
            return responseList;
        }
        return new ArrayList<>();
    }
}
