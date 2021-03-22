package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.util;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;


@Configuration
@PropertySource( value = "classpath:codes.properties", name = "codes.props")
public class ApplicationMessages
{

    @Autowired
    Environment env;

    private static Map<String, String> codesAndMessages;

    private static void setCodesAndMessages( Map<String, String> codesAndMessages )
    {
        ApplicationMessages.codesAndMessages = codesAndMessages;
    }


    public static String getMessage( String code )
    {
        return codesAndMessages.get( code );
    }


    @PostConstruct
    public void addCodes()
    {
        codesAndMessages = new HashMap<>();
        codesAndMessages.put( "FE-2000", "Successfully value extracted" );
        codesAndMessages.put( "FE-4004", "Field value not found" );
    }

}