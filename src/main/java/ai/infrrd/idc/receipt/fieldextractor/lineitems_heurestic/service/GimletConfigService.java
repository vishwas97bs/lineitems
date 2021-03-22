package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service;

import java.util.HashMap;
import java.util.Map;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor.ReceiptLineItemExtractor;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.LineItemConstants;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.MongoConnector;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBCollection;


@Component
public class GimletConfigService
{
    private static final Logger LOG = LoggerFactory.getLogger( GimletConfigService.class );


    @Value ( "${dbName}")
    private String dbName;


    private MongoConnector mongoConnector;

    @Autowired
    public void setMongoConnector( MongoConnector mongoConnector )
    {
        this.mongoConnector = mongoConnector;
    }


    public Map<String, Object> getGimletConfig()
    {
        ObjectMapper mapper = new ObjectMapper();
        DBCollection gimletConfiguration;
        try {
            gimletConfiguration = mongoConnector.getDB( dbName ).getCollection( LineItemConstants.GIMLET_CONFIGURATION );
            Object configObject;
            if ( gimletConfiguration != null ) {
                configObject = gimletConfiguration.findOne( LineItemConstants.RECEIPT );
            } else {
                return null;
            }
            JSONObject configJsonObj;
            Map<String, Object> map = null;
            try {
                configJsonObj = mapper.readValue( configObject.toString(), JSONObject.class );
                map = mapper.readValue( configJsonObj.toString(), HashMap.class );
            } catch ( JsonProcessingException e ) {
                LOG.error( "error occured in GimletConfigService {}", e.getMessage() );
            }
            return map;
        } catch ( Exception e ) {
            LOG.error( "problem while fetching data from Mongo : {}", e.getMessage() );
        }
        return new HashMap<>();
    }
}
