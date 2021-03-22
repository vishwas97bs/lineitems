package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.ConfidenceCalculator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Constants;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.QuantityValidator.QUANTITY_REGEX_INTEGER;

@Component
public class UomValidator  extends FieldValidator {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( UomValidator.class );
    private static final String UOM_REGEX = "(BT|B15|B25|B50|Bag|BBL|BKT|BOX|BRL|BSD|CAN|Carton|CM|CDM|CG|CHN|CL|CM|CMM|CRT|CS|CTN|Ctn|CUF|CUI|CUM|CUY|DAY|DG|DL|DM|DOZ|DRA|DRM|EACH|EAC|EA|Each|FOZ|FT|G|GAL|GRP|GRS|GRT|HUN|INN|Kilo|kilo|KG|KGF|KL|KM|KWH|LBS|LBT|LB|LNK|LOT|LT|ML|L|M|MDY|MG|MHR|MIL|MM|MMO|MT|MWK|OZT|PCS|PK|PKD|PL|PTD|PTL|Punnet|PWT|QTD|QTL|SCM|SDM|SF|SHT|SHW|SLV|SM|SMM|SQF|SQI|SQM|SQY|ST|TON|TRK|Tray|TUB|UNIT|UNT|YD|Quart|stück)";
    private static final String UOM_PADDED_REGEX = "([^a-zA-Z0-9]|^)" + UOM_REGEX + "([^a-zA-Z0-9]|$)";
    private static final String UOM_QUANTITY_REGEX = "(?<=("+ QUANTITY_REGEX_INTEGER +"))\\s?(?i)" + UOM_REGEX + "";


//    public UomValidator( Domain domain )
//    {
//        super( domain );
//    }


    @Override
    public boolean isCompulsory()
    {
        return false;
    }


    @Override
    public boolean isOptional()
    {
        return false;
    }

    public static String getUomRegex(){
        return UOM_REGEX;
    }

    public static String getUomPaddedRegex(){ return UOM_PADDED_REGEX; }

    @Override
    public String setField(LineItem dummyLine, List<Integer> indexes, boolean setAllMatches, LineValidator lineValidator,
                           String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionHelper, Map<String,Object> configuration )
    {
        String inputLine = lineValidator.getRemainingLineString();
        LOG.info( "Validating uom for scanReq : {} and line: {}", extractionHelper.getRequestId(),
                inputLine );
        InvoiceLineItem invoiceLineItem = null;
        if ( dummyLine instanceof InvoiceLineItem) {
            invoiceLineItem = (InvoiceLineItem) dummyLine;
        }
        LOG.trace( "Method: setField called." );
        List<String> uomMatched = new ArrayList<>();
        float fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE;
        if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
            if ( null != merchantSpecificSyntaxRegex && !merchantSpecificSyntaxRegex.isEmpty() ) {
                uomMatched = StringUtils.getMatchedTokens( inputLine, merchantSpecificSyntaxRegex );
            } else {
                uomMatched = StringUtils.getMatchedTokens( inputLine, UOM_PADDED_REGEX );
            }
        } else if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.receipt ) ) {
            uomMatched = StringUtils.getMatchedTokens( inputLine, UOM_QUANTITY_REGEX );
        }

        if ( !uomMatched.isEmpty() ) {
            if ( !setAllMatches ) {
                String uom = uomMatched.get( 0 );
                if( invoiceLineItem != null )
                    invoiceLineItem.addToUom( uom );
                inputLine = inputLine.substring( inputLine.indexOf( uom ) + uom.length() );
                try {
                    LOG.debug( "Uom field confidence: {}", fieldConfidence );
                    dummyLine.addToFieldConfidenceList( fieldConfidence );
                } catch ( LineExtractionException e ) {
                    LOG.error( "Exception while assigning UOM field confidence: " + e );
                }
            } else {
                for ( String uom : uomMatched ) {
                    indexes.add( inputLine.indexOf( uom ) );
                    inputLine = StringUtils.replacePatternWithSpaces( inputLine, uom );
                }
            }
        }
        return inputLine;
    }
}
