package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.controller;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.commons.entities.FieldExtractionResponse;
import ai.infrrd.idc.commons.entities.ResponseObject;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.CustomisedLineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.LineItemService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.util.Constants;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.util.ResponseBuilder;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping ( value = "/heurestic")
public class LineItemController
{
    private static final Logger LOGGER = LoggerFactory.getLogger( LineItemController.class );


    private ResponseBuilder responseBuilder;


    private LineItemService lineItemService;

    @Autowired
    public void setResponseBuilder( ResponseBuilder responseBuilder )
    {
        this.responseBuilder = responseBuilder;
    }


    @Autowired
    public void setLineItemService( LineItemService lineItemService )
    {
        this.lineItemService = lineItemService;
    }


    @PostMapping ( value = "/lineitems")
    @ApiOperation ( value = "extracting field data", response = FieldExtractionResponse.class)
    @ApiResponses ( value = { @ApiResponse ( code = 200, message = "Successfully extract field data") })
    public ResponseEntity<ResponseObject<List<FieldExtractionResponse<CustomisedLineItem>>>> fieldExtract( @RequestBody FieldExtractionRequest fieldExtractionRequest )
    {
        MDC.put( Constants.LOG_ID, fieldExtractionRequest.getRequestId() );
        long startTime = System.currentTimeMillis();
        LOGGER.info( "field request-----------------------{}", fieldExtractionRequest );
        List<FieldExtractionResponse<CustomisedLineItem>> response = lineItemService.extractLineItems( fieldExtractionRequest );
        LOGGER.info( "field response-----------------------{}", response );
        long endTime = System.currentTimeMillis();
        LOGGER.info( "Time taken for Field Extractor {} seconds for requestId: {}", ( endTime - startTime ) / 1000.0,
            fieldExtractionRequest.getRequestId() );
        MDC.remove( Constants.LOG_ID );
        if ( response != null && !response.isEmpty() ) {
            return responseBuilder.buildSuccessResponse( response, Constants.FIELD_VALUE_RETRIVED );
        } else {
            return responseBuilder.buildSuccessResponse( null, Constants.FIELD_VALUE_NOT_FOUND );
        }

    }
}
