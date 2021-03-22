package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.util;

import org.springframework.stereotype.Component;

@Component
public class MessageBuilder
{

    public String getMessage( String code )
    {

        String message = ApplicationMessages.getMessage( code );

        if ( message == null || message.isEmpty() ) {
            message = "default message";
        }
        return message;
    }

}
