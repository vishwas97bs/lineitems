package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.BankLineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.ConfidenceCalculator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Constants;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
public class DateValidator extends FieldValidator
{
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( DateValidator.class );
    private static final String MON = "JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC";
    private static final String MONTH = "JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER";
    private static final String DATE_REGEX_1 = "((^|\\b)\\d{1,2}( )(" + MON + ")( )\\d{2,4}\\b(?!( )?(" + MON + "|\\d)))";
    private static final String DATE_REGEX_2 = "((^|\\b)(" + MON + ")\\s\\d{1,2}\\b)";
    private static final String DATE_REGEX_3 = "((" + MONTH + ")\\s\\d{1,2}\\b)";
    public static final String DATE_REGEX_4 = "((^|\\b)\\d{1,2}(\\/)\\d{1,2}(\\/)\\d{2,4}\\b)";
    public static final String DATE_REGEX_5 = "((^|\\b)\\d{1,2}\\/\\d{2}\\b)";
    private static final String DATE_REGEX_6 = "((^|\\b)\\d{1,2}( ){0,4}(" + MON + "))";
    public static final String DATE_REGEX_7 = "\\d{2}\\.\\d{2}\\.\\d{2}";
    private static final String DATE_REGEX_8 = "\\d{2}- ?(" + MON + ")- ?\\d{2}(?!( )?" + MON + ")";
    private static final String DATE_REGEX_9 = "((^|\\b)\\d{1,2}( )(" + MON + ")( ))";


    public static final String DATE_REGEX = Constants.CASE_INSENSITIVE_REGEX + "(" + DATE_REGEX_1 + Constants.REGEX_SEPARATOR
        + DATE_REGEX_2 + Constants.REGEX_SEPARATOR + DATE_REGEX_3 + Constants.REGEX_SEPARATOR + DATE_REGEX_4
        + Constants.REGEX_SEPARATOR + DATE_REGEX_5 + Constants.REGEX_SEPARATOR + DATE_REGEX_6 + Constants.REGEX_SEPARATOR
        + DATE_REGEX_7 + Constants.REGEX_SEPARATOR + DATE_REGEX_8 + Constants.REGEX_SEPARATOR + DATE_REGEX_9 + ")";

    private Domain domain;

    @Autowired
    public void setDomain( Domain domain )
    {
        this.domain = domain;
    }


    @Override
    public String setField( LineItem dummyLine, List<Integer> indexes, boolean setAllMatches, LineValidator lineValidator,
        String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionHelper,
        Map<String, Object> configuration )
    {
        String inputLine = lineValidator.getRemainingLineString();
        LOG.info( "Validating date for scanReq  line: {}", inputLine );
        float fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE;
        List<String> datesMatched;
        if ( null != merchantSpecificSyntaxRegex && !merchantSpecificSyntaxRegex.isEmpty() ) {
            datesMatched = StringUtils.getMatchedTokens( inputLine, merchantSpecificSyntaxRegex );
        } else {
            datesMatched = StringUtils.getMatchedTokens( inputLine, DATE_REGEX );
        }

        if ( !datesMatched.isEmpty() && !setAllMatches ) {
            inputLine = inputLine.substring( inputLine.indexOf( datesMatched.get( 0 ) ) + datesMatched.get( 0 ).length() );
            try {
                LOG.debug( "Date field confidence: {}", fieldConfidence );
                dummyLine.addToFieldConfidenceList( fieldConfidence );
            } catch ( LineExtractionException e ) {
                LOG.error( "Exception while assigning Date field confidence: " + e );
            }
        } else if ( setAllMatches && !datesMatched.isEmpty() ) {
            for ( String dateMatched : datesMatched ) {
                indexes.add( inputLine.indexOf( dateMatched ) );
                inputLine = StringUtils.replacePatternWithSpaces( inputLine, dateMatched );
            }
        }
        if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.bank ) ) {
            BankLineItem bLine = ( dummyLine instanceof BankLineItem ) ? (BankLineItem) dummyLine : null;
            if ( bLine != null ) {
                if ( !datesMatched.isEmpty() ) {
                    bLine.addToDates( datesMatched.get( 0 ) );
                }
            }
        } else if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
            InvoiceLineItem iLine = ( dummyLine instanceof InvoiceLineItem ) ? (InvoiceLineItem) dummyLine : null;
            if ( iLine != null ) {
                if ( !datesMatched.isEmpty() ) {
                    iLine.setDate( datesMatched.get( 0 ) );
                }
            }
        }
        return inputLine;
    }


    @Override
    public boolean isCompulsory()
    {
        switch ( domain.getName() ) {
            case bank:
                return true;
            case receipt:
            default:
                return false;
        }
    }


    @Override
    public boolean isOptional()
    {
        return true;
    }


}
