package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.SpellCheckException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItemCheckRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItemCheckResponse;
import ai.infrrd.idc.spellcheckutility.entities.request.LocaleDetectionEntity;
import ai.infrrd.idc.spellcheckutility.entities.request.SpellCheckCategoryRequestEntity;
import ai.infrrd.idc.spellcheckutility.entities.request.SpellCheckRequestEntity;
import ai.infrrd.idc.spellcheckutility.entities.response.LocaleDetectionResponseEntity;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class SpellCheckClient {
    SpellCheckClientInterface spellCheckClientInterface = null;


    /**
     * Constructor for {@link SpellCheckClient}
     * @param apiUrl    api url
     */
    public SpellCheckClient( String apiUrl )
    {
        this.spellCheckClientInterface = new Retrofit.Builder().baseUrl( apiUrl )
                .addConverterFactory( GsonConverterFactory.create() )
                .client( new OkHttpClient().newBuilder().readTimeout( 5, TimeUnit.SECONDS ).build() ).build()
                .create( SpellCheckClientInterface.class );
    }


    /**
     * Hits gimlet SpellCheck API
     * @param spellCheckRequestEntity spellCheckRequestEntity
     * @return response
     */
    public Map<String, List<Map<String, Object>>> spellCheckAPI(SpellCheckRequestEntity spellCheckRequestEntity )
            throws SpellCheckException
    {
        try {
            retrofit2.Response<Map<String, List<Map<String, Object>>>> response = this.spellCheckClientInterface
                    .spellCheckAPI( spellCheckRequestEntity ).execute();
            if ( response.isSuccessful() ) {
                return response.body();
            } else {
                throw new SpellCheckException( String.format( "Error while fetching entries from SpellCheck API: %s",
                        new String( response.errorBody().bytes() ) ) );
            }
        } catch ( IOException e ) {
            throw new SpellCheckException( "Error occurred while connecting to SpellCheck service", e );
        }
    }


    /**
     * Hits gimlet SpellCheck API
     * @param spellCheckRequestEntity spellCheckRequestEntity
     * @return response
     */
    public List<Object> categorizationAPI( SpellCheckCategoryRequestEntity spellCheckRequestEntity ) throws SpellCheckException
    {
        try {
            retrofit2.Response<List<Object>> response = this.spellCheckClientInterface
                    .categorizationAPI( spellCheckRequestEntity ).execute();
            if ( response.isSuccessful() ) {
                return response.body();
            } else {
                throw new SpellCheckException( String.format( "Error while fetching entries from SpellCheck API: %s",
                        new String( response.errorBody().bytes() ) ) );
            }
        } catch ( IOException e ) {
            throw new SpellCheckException( "Error occurred while connecting to SpellCheck service", e );
        }
    }


    /**
     * Hits gimlet Locale detection API
     * @param localeDetectionEntity localeRequestEntity
     * @return response
     */
    public List<LocaleDetectionResponseEntity> localeDetectionAPI( LocaleDetectionEntity localeDetectionEntity )
            throws SpellCheckException
    {
        try {
            retrofit2.Response<List<LocaleDetectionResponseEntity>> response = this.spellCheckClientInterface
                    .localeDetectionAPI( localeDetectionEntity ).execute();
            if ( response.isSuccessful() ) {
                return response.body();
            } else {
                throw new SpellCheckException( String.format( "Error while fetching entries from SpellCheck API: %s",
                        new String( response.errorBody().bytes() ) ) );
            }
        } catch ( IOException e ) {
            throw new SpellCheckException( "Error occurred while connecting to SpellCheck Locale detection service", e );
        }
    }


    /**
     * Hits gimlet Locale detection API
     * @param lineItemCheckRequest lineItemCheckRequest
     * @return response
     */
    public LineItemCheckResponse liCheckAPI( LineItemCheckRequest lineItemCheckRequest ) throws SpellCheckException
    {
        try {
            retrofit2.Response<LineItemCheckResponse> response = this.spellCheckClientInterface
                    .liCheckAPI( lineItemCheckRequest ).execute();
            if ( response.isSuccessful() ) {
                return response.body();
            } else {
                throw new SpellCheckException( String.format( "Error while fetching entries from SpellCheck API: %s",
                        new String( response.errorBody().bytes() ) ) );
            }
        } catch ( IOException e ) {
            throw new SpellCheckException( "Error occurred while connecting to SpellCheck Locale detection service", e );
        }
    }


    /**
     * private interface for Gimlet SpellCheck
     */
    private interface SpellCheckClientInterface
    {
        @POST( "/gimlet-spellcheck/v3/suggestions")
        Call<Map<String, List<Map<String, Object>>>> spellCheckAPI(@Body SpellCheckRequestEntity spellCheckRequestEntity );


        @POST ( "/gimlet-spellcheck/getReceiptCategories")
        Call<List<Object>> categorizationAPI( @Body SpellCheckCategoryRequestEntity spellCheckRequestEntity );


        @POST ( "/gimlet-spellcheck/findCities")
        Call<List<LocaleDetectionResponseEntity>> localeDetectionAPI(@Body LocaleDetectionEntity localeDetectionEntity );


        @POST ( "/gimlet-spellcheck/text-lookup/line-item/suggestions")
        Call<LineItemCheckResponse> liCheckAPI(@Body LineItemCheckRequest lineItemCheckRequest );
    }
}
