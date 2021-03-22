package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineParser;
import com.mongodb.*;
import com.mongodb.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class MongoConnector implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger( MongoConnector.class );
    private static String MONGO_DEFAULT = null;
    private static final Map<String, MongoClient> CLIENT_MAP = new HashMap<>();
    private static final Map<String, DB> DB_MAP = new HashMap<>();

    @Value( "${mongoServer}")
    private String mongoServer;

    public static final LineParser DEFAULT_CONVERTER = new LineParser() {
        @SuppressWarnings ( "unchecked")
        @Override
        public Map<String, Object> parse( String string )
        {
            return ( (DBObject) JSON.parse( string ) ).toMap();
        }
    };


    @Override
    public void afterPropertiesSet() {
        if ( MONGO_DEFAULT == null ) {
            MONGO_DEFAULT = Util.getOrDefault( System.getenv( "MONGO_CONNECTION" ), mongoServer );
        }
    }


    /**
     * Returns a {@link MongoClient} for a mongo URI
     *
     * @param mongoConnection MongoConnection String
     * @return Mongo Client
     */
    private  synchronized MongoClient getClient(String mongoConnection)
    {
        if ( !CLIENT_MAP.containsKey( mongoConnection ) ) {
            synchronized ( CLIENT_MAP ) {
                if ( !CLIENT_MAP.containsKey( mongoConnection ) ) {
                    CLIENT_MAP.put( mongoConnection, new MongoClient( new MongoClientURI( mongoConnection ) ) );
                }
            }
        }
        return CLIENT_MAP.get( mongoConnection );
    }


    /**
     * Returns the {@link MongoClient} for the URI picked from the config
     *
     * @return Mongo Client
     */
    public  MongoClient getClient()
    {
        return getClient( MONGO_DEFAULT );
    }


    /**
     * Returns the {@link DB} for the default {@link MongoClient}
     *
     * @param dbName Database name to fetch
     * @return DB Object
     */
    @SuppressWarnings ( "deprecation")
    public  DB  getDB( String dbName )
    {
        if ( !DB_MAP.containsKey( dbName ) ) {
            LOG.info( "Request for a new DB @ {}", dbName );
            synchronized ( DB_MAP ) {
                if ( !DB_MAP.containsKey( dbName ) ) {
                    LOG.info( "Obtaining DB @ {}", dbName );
                    DB_MAP.put( dbName, getClient().getDB( dbName ) );
                }
            }
        }
        return DB_MAP.get( dbName );
    }


    /**
     * Loads a stream into a collection using default converted
     *
     * @param stream         stream to be loaded
     * @param db             database name in which stream is to be loaded
     * @param collectionName collection name in which stream is to be loaded
     */
    public  void load(InputStream stream, String db, String collectionName ) throws IOException
    {
        load( stream, db, collectionName, DEFAULT_CONVERTER );
    }


    /**
     * Loads a stream into a collection
     *
     * @param stream         stream to be loaded
     * @param db             database name in which stream is to be loaded
     * @param collectionName collection name in which stream is to be loaded
     * @param parser         converter used to parse a string into DBObjects
     */
    private  void load(InputStream stream, String db, String collectionName, LineParser parser) throws IOException
    {
        LOG.info( "Parsing stream. Destination DB: {}, Destination collection: {}", db, collectionName );
        DBCollection collection = getDB( db ).getCollection( collectionName );
        try ( BufferedReader br = new BufferedReader( new InputStreamReader( stream ) ) ) {
            String line;
            int skippedLines = 0;
            int processedLines = 0;
            while ( ( line = br.readLine() ) != null ) {
                if ( line.trim().isEmpty() ) {
                    continue;
                }
                Map<String, Object> map = parser.parse( line );
                if ( map == null ) {
                    skippedLines += 1;
                } else {
                    collection.save( new BasicDBObject( map ) );
                    processedLines += 1;
                    logProgress( processedLines );
                }
            }
            LOG.info( "Finished parsing stream. Skipped Lines: {}, Processed Lines: {}, Total Lines: {}", skippedLines,
                    processedLines, skippedLines + processedLines );
        }
    }


    private static void logProgress( int processedLines )
    {
        if ( processedLines % 1000 == 0 ) {
            LOG.info( "Processed {} lines", processedLines );
        }
    }

}
