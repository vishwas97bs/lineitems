package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.DocumentMetaData;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.ConfidenceCalculator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Constants;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class IdValidator extends FieldValidator
{
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( IdValidator.class );
    private static final String CURRENCY_REGEX = "[$£€]";
    private static final String ID_REGEX = "\\b\\d{4,100}[A-Za-z]{0,2}\\b";
    //\b\d{4,}([A-Za-z/b, ]{0,2}\d+[^\d,.])*[A-Za-z/b,]{0,2}\b
    //Should not match 8X4 in 4005808747405 8X4

    //(\s|^)\d([A-WYZa-wyz/b ]{0,2}\d{1,10}){2,10}[A-WYZa-wyz/b,]{0,2}[^.,Xx:%](\s|$)
    private static final String ID_REGEX_WITH_SPELLCHECK = "(?<!" + CURRENCY_REGEX + ")(\\b|\\s|^)\\d([A-WYZa-wyz#"
        + Constants.SPELL_CHECK_CHARS.replace( ",", "" ) + "]{0,2}\\d{1,10}){2,5}[A-WYZa-wyz"
        + Constants.SPELL_CHECK_CHARS.trim() + "]{0,2}[^.,Xx:%](\\s|$)";

    //Date and qty like 200G
    private static final String ID_REGEX_TO_NEGLECT = "(\\s|^)(\\d{2}\\/\\d{2}\\/(\\d{4}|\\d{2})|(\\d{2,4}[gG])|(\\d{1,2}\\.\\d{1,4}))(\\s|$)";

    //Neglecting Ids like "X500 G"
    public static final String INVOICE_ID_REGEX = "( |^)(((([A-Z\\-]{1,50}|[A-Z]{2,50})\\d{1,50}){1,50}([A-Z]{1,10})?)|(([^.]|^)\\b\\d{3,50}[A-Za-z]{0,2}\\b)) ";

    private static final String QUANTITY_FILTER_REGEX = "\\s{2}\\d(\\s{1,2}|$)";

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


    /**
     *Validates and sets the value of the product ID. Removes it from the line item string
     */
    @Override
    public String setField(LineItem dummyLine, List<Integer> indexes, boolean setAllMatches, LineValidator lineValidator,
                           String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionHelper , Map<String,Object> configuration)
    {
        String inputLine = lineValidator.getRemainingLineString();
        LOG.info( "Validating id for scanReq  line: {}", inputLine );
        float fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE;
        LOG.trace( "Method: setField called." );
        List<String> idsMatched;
        if ( null != merchantSpecificSyntaxRegex && !merchantSpecificSyntaxRegex.isEmpty() ) {

            idsMatched = StringUtils.getMatchedTokens( inputLine.replace( ",", "" ), merchantSpecificSyntaxRegex );
        } else if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
            idsMatched = StringUtils.getMatchedTokens( inputLine, INVOICE_ID_REGEX );
        } else {
            String inputLineExcludingDate = StringUtils.replaceRegexMatchesWithSpaces( inputLine, ID_REGEX_TO_NEGLECT );
            idsMatched = StringUtils.getMatchedTokens( inputLineExcludingDate, ID_REGEX_WITH_SPELLCHECK );
        }
        if ( !idsMatched.isEmpty() ) {
            if ( !setAllMatches ) {
                String id = idsMatched.get( 0 );
                id = id.replaceAll( QUANTITY_FILTER_REGEX, "" );
                String spellCheckedId = id;
                for ( String spellCheckReplaceChar : Constants.SPELL_CHECK_REPLACEMENT_CHARS.split( "," ) ) {
                    String[] replacementChars = spellCheckReplaceChar.split( "\\|" );
                    fieldConfidence = reduceConfidenceForSpellCorrection( replacementChars[0], spellCheckedId,
                        fieldConfidence );
                    spellCheckedId = spellCheckedId.replace( replacementChars[0], replacementChars[1] );
                }
                for ( String spellCheckReplaceCharWithEmpty : Constants.SPELL_CHECK_REPLACE_WITH_EMPTY.split( "\\|" ) ) {
                    fieldConfidence = reduceConfidenceForSpellCorrection( spellCheckReplaceCharWithEmpty, spellCheckedId,
                        fieldConfidence );
                    spellCheckedId = spellCheckedId.replace( spellCheckReplaceCharWithEmpty, "" );
                }
                dummyLine.setProductId( spellCheckedId );
                inputLine = inputLine.substring( inputLine.indexOf( id ) + id.length() );
                try {
                    LOG.debug( "Id Field confidence: {}", fieldConfidence );
                    dummyLine.addToFieldConfidenceList( fieldConfidence );
                } catch ( LineExtractionException e ) {
                    LOG.error( "Exception while assigning ID field confidence: " + e );
                }
            } else {
                for ( String idMatched : idsMatched ) {
                    indexes.add( inputLine.indexOf( idMatched ) );
                    inputLine = StringUtils.replacePatternWithSpaces( inputLine, idMatched );
                }
            }
        }
        return inputLine;
    }


    private float reduceConfidenceForSpellCorrection( String replacementChar, String spellCheckedId, float fieldConfidence )
    {
        if ( spellCheckedId.contains( replacementChar ) ) {
            fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE - ConfidenceCalculator.MINIMUM_CONF_DIFF;
        }
        return fieldConfidence;
    }
}
