package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConfigService
{

    @SuppressWarnings ( "unchecked")
    public List<String> getAllValueList( String key, String extension, Map<String, Object> config, String locale )
    {
        List<String> response = new ArrayList<>();

        List<String> keySet = getKeys( key, extension, new HashMap<>(), locale );

        for ( String prop : keySet ) {
            List<String> value = (List<String>) getExtractionConfiguration( prop, config );
            if ( value != null )
                response.addAll( new ArrayList<>( value ) );
        }
        return response;
    }


    private List<String> getKeys( String key, String extension, Map<String, Object> configuration, String locale )
    {
        List<String> response = new ArrayList<>();
        if ( locale != null ) {
            if ( locale.contains( "_" ) ) {
                String value = locale + "_" + key + extension;
                response.add( value );
            } else {
                String valuePattern = locale + ".{0,9}_" + key + extension;
                for ( String ky : configuration.keySet() ) {
                    if ( ky.matches( valuePattern ) ) {
                        response.add( ky );
                    }
                }
            }
        }
        if ( response.isEmpty() ) {
            String value = key + extension;
            response.add( value );
        }
        return response;
    }


    @SuppressWarnings ( "unchecked")
    public List<String> getValueList( String key, Map<String, Object> configuration )
    {
        List<String> response = (List<String>) getExtractionConfiguration( key, configuration );
        if ( response == null )
            response = new ArrayList<>();
        return new ArrayList<>( response );
    }


    public Object getExtractionConfiguration( String key, Map<String, Object> configuration )
    {
        Object value = null;
        Map<String, Object> map = (Map<String, Object>) configuration.get( "configuration" );
        if ( map.containsKey( key ) ) {
            value = map.get( key );
        }
        return value;
    }


    @SuppressWarnings ( "unchecked")
    public Map<String, Object> getExtractionConfigurationMap( String key, Map<String, Object> configuration )
    {
        Object value = getExtractionConfiguration( key,configuration );
        Map<String, Object> mapObject = null;
        if ( value != null ) {
            mapObject = (Map<String, Object>) value;
        }
        return mapObject;
    }
}
