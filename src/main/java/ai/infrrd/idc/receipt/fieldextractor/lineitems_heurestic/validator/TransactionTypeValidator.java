package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.DocumentMetaData;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.BankLineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.ConfidenceCalculator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
public class TransactionTypeValidator extends FieldValidator
{

    private static final String TRANSACTION_TYPE_REGEX = "(?i)\\b(CR|DB)\\b";

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( TransactionTypeValidator.class );


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


    @Override
    public String setField( LineItem dummyLine, List<Integer> indexes, boolean setAllMatches, LineValidator lineValidator,
        String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionHelper,
        Map<String, Object> configuration )
    {
        String inputLine = lineValidator.getRemainingLineString();
        LOG.info( "Validating transaction type for scanReq line: {}", inputLine );
        LOG.trace( "Method: setField called." );
        if ( dummyLine instanceof BankLineItem ) {
            BankLineItem bLine = (BankLineItem) dummyLine;
            float fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE;
            List<String> transactionTypes;
            if ( null != merchantSpecificSyntaxRegex && !merchantSpecificSyntaxRegex.isEmpty() ) {
                transactionTypes = StringUtils.getMatchedTokens( inputLine, merchantSpecificSyntaxRegex );
            } else {
                transactionTypes = StringUtils.getMatchedTokens( inputLine, TRANSACTION_TYPE_REGEX );
            }

            // Assuming that there will be only one of this in a bank document, and it will be in the last few columns:
            if ( !transactionTypes.isEmpty() ) {
                String tTypeInLine = transactionTypes.get( transactionTypes.size() - 1 );
                indexes.add( inputLine.indexOf( tTypeInLine ) );
                if ( setAllMatches ) {
                    inputLine = StringUtils.replacePatternWithSpaces( inputLine, tTypeInLine );
                } else {
                    inputLine = inputLine.substring( inputLine.indexOf( tTypeInLine ) + tTypeInLine.length() );
                    try {
                        LOG.debug( "Transaction type field confidence: {} ", fieldConfidence );
                        dummyLine.addToFieldConfidenceList( fieldConfidence );
                    } catch ( LineExtractionException e ) {
                        LOG.error( "Exception while assigning Transaction type field confidence: " + e );
                    }
                }
                bLine.setTransactionType( tTypeInLine );
            }
        }
        LOG.trace( "Method: setField finished." );
        return inputLine;
    }
}
