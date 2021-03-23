package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.summarizer;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.commons.entities.FieldExtractionResponse;
import ai.infrrd.idc.commons.extractors.entities.Coordinates;
import ai.infrrd.idc.commons.extractors.entities.VisionLineBlock;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.SpellCheckException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor.ReceiptLineItemExtractor;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.CoordinatesExtractor;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.SpellCheckClient;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class ReceiptLineItemSummarizer
{
    @Value ( "${spellcheck-server}")
    String spellcheckUrl;

    private static final Logger LOG = LoggerFactory.getLogger( ReceiptLineItemSummarizer.class );
    private final SpellCheckClient SPELL_CHECK_CLIENT = new SpellCheckClient( "http://35.162.115.36:8090" );

    private Utils utils;

    private ReceiptLineItemExtractor receiptLineItemExtractor;

    @Autowired
    public void setUtils( Utils utils )
    {
        this.utils = utils;
    }


    @Autowired
    public void setReceiptLineItemExtractor( ReceiptLineItemExtractor receiptLineItemExtractor )
    {
        this.receiptLineItemExtractor = receiptLineItemExtractor;
    }


    @SuppressWarnings ( "unchecked")
    public Map<String, Object> summarize(FieldExtractionRequest extractionData,
                                         FieldConfiguration fieldConfiguration )
    {
        LOG.info( "request has entered the summarize method : {}", extractionData.getRequestId() );
        //set the merchantname into this map
        Map<String, Object> merchantMap = new HashMap<>();
        Map<String, Object> value = new HashMap<>();

        List<FieldExtractionResponse> fieldExtractionResponseList = extractionData.getExtractedFields();
        for ( FieldExtractionResponse fieldExtractionResponse : fieldExtractionResponseList ) {
            if ( fieldExtractionResponse.getFieldName().toLowerCase().equals( "receipt_merchantname" ) ) {
                if ( fieldExtractionResponse.getValue().toString() != null
                    && !fieldExtractionResponse.getValue().toString().isEmpty() ) {
                    value.put( "value", fieldExtractionResponse.getValue().toString() );
                } else {
                    value.put( "value", "" );
                }
                merchantMap.put( "merchantname", value );
            }
        }

        //local testing
        value.put( "value", "" );
        merchantMap.put( "merchantname", value );


        String scanId = extractionData.getRequestId();
        LOG.info( "Getting Receipt Line Items for scanRequest: {}", scanId );
        /*
         * Line items are extracted in this call
         */
        LineItemResponse lineItemResponse = receiptLineItemExtractor.getLineObjects( merchantMap, extractionData,
            fieldConfiguration );
        LOG.debug( "scan : {}, Line Item Response: {}", scanId, lineItemResponse );

        // Map that will contain the final line item response
        Map<String, Object> responseMap = new HashMap<>();


        if ( null != lineItemResponse ) {
            /*
             * Line item spell check
             */
            List<LineItem> lineItems = lineItemResponse.getLineItems();
            LineItemCheckRequest lineItemCheckRequest = new LineItemCheckRequest();
            Map<String, Object> filters = new HashMap<>();
            String merchantName = utils.getMerchantName( merchantMap );
            LOG.debug( "Merchant Name: {} for {}", merchantName, scanId );
            if ( merchantName != null && !StringUtils.isEmpty( merchantName ) && fieldConfiguration != null
                && !StringUtils.isEmpty( fieldConfiguration.getCustomerId() ) ) {
                LOG.info( "Calling LI Spellcheck API for ScanRequestID : {}", scanId );
                lineItems = spellcheckLineItem( scanId, lineItems, lineItemCheckRequest, filters, merchantName,
                    fieldConfiguration );
            } else {
                LOG.info( "ScanId: {}, Not doing spellcheck for merchant {} and customer {}", scanId, merchantName,
                    fieldConfiguration );
            }

            //NOTE : toString method of the lineItem should be called so as to get only the required entities in the response
            List<Map<String, Object>> items = new Gson().fromJson( lineItems.toString(), List.class );
            //            ScanRequest scanRequest = extractionData.getScanRequest();
            //            if ( scanRequest.getScanRequestAdditionalInfo() != null
            //                && scanRequest.getScanRequestAdditionalInfo().getGetCoordinates() ) {
            //                List<VisionLineBlock> visionLineBlockList = new ArrayList<>( extractionData.getOcrCoordinates() );
            //                setLineItemCoordinates( items, visionLineBlockList );
            //            }
            responseMap.put( "confidence", lineItemResponse.getConfidence() );
            responseMap.put( "lineitems", items );
            ObjectMapper objectMapper = new ObjectMapper();
            LOG.info( "Final line item response : {} for {}", responseMap, scanId );
            //            tabularData.put( fieldDetails.get( "fieldName" ).toString(), responseMap );
        }
        //        LOG.info( "Extracted {} lineItems for scanId: {} with confidence {}", lineItemResponse.getLineItems().size(), scanId,
        //           lineItemResponse.getConfidence() );
        //        tabularData.put( fieldDetails.get( "fieldName" ).toString(), responseMap );
        //        return responseMap;
        LOG.info( "request has exiting the summarize method : {}", extractionData.getRequestId() );
        return responseMap;
    }


    /**
     * Extracts the line-item coordinates using the list of VisionLineBlock corresponding to the OCR'ed text from GV
     * @param items list of extracted receipt lineitem objects as List<Map<String,Object>>
     * @param visionLineBlockList list of VisionLineBlock corresponding to the Ocr'ed text from GV
     */
    private void setLineItemCoordinates( List<Map<String, Object>> items, List<VisionLineBlock> visionLineBlockList )
    {
        List<VisionLineBlock> copyOfvisionLineBlockList = visionLineBlockList;
        int counter = 0;
        boolean saveForFirst = false;
        Map<String, Object> previousLineItem = null;
        Map<String, Object> nextLineItem = null;
        Map<String, Object> firstLineItem = null;
        for ( Map<String, Object> lineItem : items ) {
            Coordinates coordinates;
            coordinates = getCoordinatesFrom( visionLineBlockList, lineItem, "rawText" );
            if ( coordinates != null ) {
                visionLineBlockList = updateParams( visionLineBlockList, lineItem, coordinates );
            } else {
                coordinates = getCoordinatesFrom( visionLineBlockList, lineItem, "productName" );
                if ( coordinates != null ) {
                    visionLineBlockList = updateParams( visionLineBlockList, lineItem, coordinates );
                } else {
                    coordinates = getCoordinatesFrom( visionLineBlockList, lineItem, "productId" );
                    if ( coordinates != null ) {
                        visionLineBlockList = updateParams( visionLineBlockList, lineItem, coordinates );
                    } else {
                        if ( counter != 0 ) {
                            coordinates = CoordinatesExtractor.getLineCoordinatesUsingAdjacentLine( previousLineItem,
                                copyOfvisionLineBlockList, false );
                            if ( coordinates != null ) {
                                visionLineBlockList = updateParams( visionLineBlockList, lineItem, coordinates );
                            } else {
                                lineItem.put( "startX", 0 );
                                lineItem.put( "startY", 0 );
                                lineItem.put( "endX", 0 );
                                lineItem.put( "endY", 0 );
                            }
                        } else {
                            saveForFirst = true;
                        }
                    }
                }
            }
            if ( counter == 1 && saveForFirst ) {
                nextLineItem = lineItem;
                firstLineItem = previousLineItem;
            }
            previousLineItem = lineItem;
            counter++;
        }
        if ( saveForFirst && items.size() > 1 ) {
            Coordinates coordinates = CoordinatesExtractor.getLineCoordinatesUsingAdjacentLine( nextLineItem,
                copyOfvisionLineBlockList, true );
            if ( coordinates != null ) {
                visionLineBlockList = updateParams( visionLineBlockList, firstLineItem, coordinates );
            } else {
                firstLineItem.put( "startX", 0 );
                firstLineItem.put( "startY", 0 );
                firstLineItem.put( "endX", 0 );
                firstLineItem.put( "endY", 0 );
            }
        } else if ( !( items.size() > 1 ) ) {
            items.get( 0 ).put( "startX", 0 );
            items.get( 0 ).put( "startY", 0 );
            items.get( 0 ).put( "endX", 0 );
            items.get( 0 ).put( "endY", 0 );
        }
    }


    /**
     * Updates the lineItem objects with extracted coordinates &
     * visionLineBlock list by removing the visionLineBlock corresponding to the current extracted coordinates
     * @param visionLineBlockList list of VisionLineBlock corresponding to the Ocr'ed text from GV
     * @param lineItem lineItem that corresponds to the extracted coordinates
     * @param coordinates extracted coordinates
     * @return the updated visionLineBlock list after removing the visionLineBlock corresponding to the coordinates
     */
    private List<VisionLineBlock> updateParams( List<VisionLineBlock> visionLineBlockList, Map<String, Object> lineItem,
        Coordinates coordinates )
    {
        lineItem.put( "startX", coordinates.getStartX() );
        lineItem.put( "startY", coordinates.getStartY() );
        lineItem.put( "endX", coordinates.getEndX() );
        lineItem.put( "endY", coordinates.getEndY() );
        visionLineBlockList = CoordinatesExtractor.removeCorrespondingVisionLineBlock( visionLineBlockList, coordinates );
        return visionLineBlockList;
    }


    /**
     * Validates & invokes the CoordinatesExtractor using the given lineItem key
     * @param visionLineBlockList list of VisionLineBlock corresponding to the Ocr'ed text from GV
     * @param lineItem lineItem for which the coordinates has to be extracted
     * @param key key which corresponds to the text in lineitem for which the coordinates should be extracted
     * @return an instance of Coordinates for successful extraction if the key is valid else null
     */
    private Coordinates getCoordinatesFrom( List<VisionLineBlock> visionLineBlockList, Map<String, Object> lineItem,
        String key )
    {
        Coordinates coordinates = null;
        if ( lineItem.containsKey( key ) && StringUtils.isNotBlank( String.valueOf( lineItem.get( key ) ) ) ) {
            coordinates = CoordinatesExtractor.getCordinatesFromLine( visionLineBlockList,
                String.valueOf( lineItem.get( key ) ), true );
        }
        return coordinates;
    }


    /**
     * This method updates line items with corrected names if a dictionary for the customer is available
     * @param lineItems
     * @param lineItemCheckRequest
     * @param filters
     * @param merchantName
     * @return
     */
    private List<LineItem> spellcheckLineItem( String scanId, List<LineItem> lineItems,
        LineItemCheckRequest lineItemCheckRequest, Map<String, Object> filters, String merchantName,
        FieldConfiguration fieldConfiguration )
    {
        LOG.debug( "Customer Id: {} for scan {}", fieldConfiguration.getCustomerId(), scanId );
        filters.put( "merchant", merchantName );
        filters.put( "customer", fieldConfiguration.getCustomerId() );
        lineItemCheckRequest.setFilters( filters );
        List<LineItem> correctedLineItems = new ArrayList<>();
        List<String> liStrings = new ArrayList<>();
        for ( LineItem lineItem : lineItems ) {
            String productName = lineItem.getProductName();
            if ( !StringUtils.isEmpty( productName ) ) {
                liStrings.add( productName );
            }
        }
        LOG.trace( "List of product names for spell check: {} for scan {}", liStrings, scanId );
        if ( !liStrings.isEmpty() ) {
            LOG.debug( "Calling LI Spellcheck API for {} strings for ScanRequestID : {}", liStrings.size(), scanId );
            lineItemCheckRequest.setItems( liStrings );
            try {
                LineItemCheckResponse response = SPELL_CHECK_CLIENT.liCheckAPI( lineItemCheckRequest );
                LOG.debug( "Line Item spellcheck Response: {} for scan {}", response, scanId );
                Map<String, Set<LIResponseItem>> spellCheckedNames = response.getItems();
                for ( LineItem lineItem : lineItems ) {
                    ReceiptLineItem receiptLineItem = (ReceiptLineItem) lineItem;
                    ReceiptLineItem copy = receiptLineItem.getCopy();
                    String productName = lineItem.getProductName();
                    if ( !productName.isEmpty() && spellCheckedNames.containsKey( productName ) ) {
                        String correctedProductName = getBestMatch( spellCheckedNames.get( productName ) );
                        LOG.trace( "Corrected {} to {}", productName, correctedProductName );
                        copy.setProductName( correctedProductName );
                    }
                    correctedLineItems.add( copy );
                }
                lineItems = correctedLineItems;
                LOG.debug( "Spell corrected Line Items: {} for {}", lineItems, scanId );
            } catch ( SpellCheckException e ) {
                LOG.error( "Error while hitting spellcheck API for " + scanId, e );
            }
        }
        return lineItems;
    }


    private String getBestMatch( Set<LIResponseItem> set )
    {
        if ( set == null || set.isEmpty() ) {
            return null;
        }
        LOG.trace( "Matches found: {}", set.size() );
        Iterator<LIResponseItem> iterator = set.iterator();
        LIResponseItem firstVal = iterator.next();
        String bestMatch = firstVal.getValue();
        Double bestScore = firstVal.getScore();
        while ( iterator.hasNext() ) {
            LIResponseItem thisVal = iterator.next();
            LOG.trace( thisVal + "" );
            if ( Double.compare( thisVal.getScore(), bestScore ) > 0 ) {
                bestMatch = thisVal.getValue();
                bestScore = thisVal.getScore();
            }
        }
        LOG.trace( "Best match is {} with score {} ", bestMatch, bestScore );
        return bestMatch;
    }
}
