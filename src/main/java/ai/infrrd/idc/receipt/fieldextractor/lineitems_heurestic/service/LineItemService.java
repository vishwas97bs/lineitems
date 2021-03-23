package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.commons.entities.FieldExtractionResponse;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.CustomisedLineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.FieldConfiguration;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.summarizer.ReceiptLineItemSummarizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
public class LineItemService
{

    private ReceiptLineItemSummarizer receiptLineItemSummarizer;

    @Autowired
    public void setReceiptLineItemSummarizer( ReceiptLineItemSummarizer receiptLineItemSummarizer )
    {
        this.receiptLineItemSummarizer = receiptLineItemSummarizer;
    }


    public List<FieldExtractionResponse<CustomisedLineItem>> extractLineItems( FieldExtractionRequest fieldExtractionRequest )
    {
        List<FieldExtractionResponse<CustomisedLineItem>> responseList = new ArrayList<>();
        List<FieldExtractionResponse<CustomisedLineItem>> responses = new ArrayList<>();

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
        Map<String, Object> extractedValues = receiptLineItemSummarizer.summarize( fieldExtractionRequest, fieldConfiguration );

        List<Map<String, Object>> extractedLineItems = (List<Map<String, Object>>) extractedValues.get( "lineitems" );
        if ( extractedValues != null && !extractedValues.isEmpty() && extractedLineItems != null
            && extractedLineItems.size() > 0 ) {
            responseList = mapData( extractedLineItems );
            FieldExtractionResponse fieldExtractionResponse = new FieldExtractionResponse();
            fieldExtractionResponse.setListOfValues( responseList );
            fieldExtractionResponse.setConfidence( Double.parseDouble( extractedValues.get( "confidence" ).toString() ) );
            fieldExtractionResponse.setFieldName( fieldExtractionRequest.getFieldConfigDetails().get( 0 ).getFieldName() );
            fieldExtractionResponse.setSuccess( true );
            responses.add( fieldExtractionResponse );
            return responses;
        }
        return new ArrayList<>();
    }


    private List<FieldExtractionResponse<CustomisedLineItem>> mapData( List<Map<String, Object>> extractedLineItems )
    {
        List<FieldExtractionResponse<CustomisedLineItem>> responseList = new ArrayList<>();
        for ( Map<String, Object> map : extractedLineItems ) {
            CustomisedLineItem customisedLineItem = new CustomisedLineItem();
            FieldExtractionResponse fieldExtractionResponse = new FieldExtractionResponse();
            customisedLineItem.setConfidence( (Double) map.get( "confidence" ) );
            customisedLineItem.setFinalPrice( (Double) map.get( "finalPrice" ) );
            customisedLineItem.setLineNumber( (Double) map.get( "lineNumber" ) );
            customisedLineItem.setCoupon( (Boolean) map.get( "isCoupon" ) );
            customisedLineItem.setProductId( (String) map.get( "productId" ) );
            customisedLineItem.setProductName( (String) map.get( "productName" ) );
            customisedLineItem.setQuantity( (Double) map.get( "quantity" ) );
            customisedLineItem.setQuantityUnit( (String) map.get( "quantityUnit" ) );
            customisedLineItem.setRawText( (String) map.get( "rawText" ) );
            customisedLineItem.setUnitPrice( (Double) map.get( "untiPrice" ) );
            fieldExtractionResponse.setValue( customisedLineItem );
            responseList.add( fieldExtractionResponse );
        }
        return responseList;
    }
}
