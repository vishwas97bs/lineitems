package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import ai.infrrd.idc.commons.entities.ResponseObject;
import ai.infrrd.idc.commons.entities.Status;


@Component
public class ResponseBuilder
{

    @Autowired
    private MessageBuilder messageBuilder;

    public <T> ResponseEntity<ResponseObject> buildSuccessResponse(T data, String successCode )
    {
        ResponseObject<T> response = new ResponseObject<>();

        response.setData( data );
        response.setSuccess( new Status( successCode, messageBuilder.getMessage( successCode ) ) );

        return new ResponseEntity<>( response, HttpStatus.OK );
    }


    public ResponseObject buildErrorResponse(String errorCode )
    {
        ResponseObject responseObject = new ResponseObject();
        responseObject.setErrors( errorBuilder( errorCode ) );

        return responseObject;
    }


    private List<Status> errorBuilder(String errorCode )
    {
        List<Status> errorList = new ArrayList<>();
        errorList.add( new Status( errorCode, messageBuilder.getMessage( errorCode ) ) );

        return errorList;
    }

}
