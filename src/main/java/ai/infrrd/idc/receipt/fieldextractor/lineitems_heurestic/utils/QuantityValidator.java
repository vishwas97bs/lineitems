package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.FieldValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.UomValidator;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/*
This class extends the validator abstract class and contains methods to validate & set Quantity
 */
@Component
public class QuantityValidator extends FieldValidator
{
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( QuantityValidator.class );
    //Regex for quantity
    public static final String QUANTITY_REGEX_INTEGER = "(^| )\\d{1,3}(\\s)(?![.]\\d)";
    public static final String QUANTITY_REGEX_DECIMAL = "\\d{1,3}[., ]?(\\d{2,3})?";
    static final String QUANTITY_UNIT_REGEX = "(\\d{1,3}\\s{0,1}[., ]?\\s{0,1}(\\d{2,3})?)|(^| )\\d{1,3}(\\s|ML|x)";
    private static final String QUANTITY_UOM_REGEX = "(\\d{1,3}(\\.\\d{1,3})?\\s?)(?=(?i)(" + UomValidator.getUomRegex() + "))";


    @Override
    public boolean isCompulsory()
    {
        return false;

    }


    @Override
    public boolean isOptional()
    {
        return true;
    }


    @Override
    public String setField( LineItem dummyLine, List<Integer> indexes, boolean setAllMatches, LineValidator lineValidator,
        String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionHelper,
        Map<String, Object> configuration )
    {
        String[] split = new String[10];
        if ( !setAllMatches ) {
            String syntax = metaData.getSyntax();
            split = syntax.split( "\\|" );
        }
        boolean shouldAdd = false;
        List<String> quantitiesMatched = new ArrayList<>();
        String inputLine = lineValidator.getRemainingLineString();
        int indexOfSecondSpace = 0;
        if ( inputLine.contains( " " ) ) {
            int indexOfFirstSpace = inputLine.indexOf( " " );
            String stringForSecondSpace = inputLine.substring( indexOfFirstSpace + 1 );
            if ( stringForSecondSpace.contains( " " ) ) {
                indexOfSecondSpace = stringForSecondSpace.indexOf( " " );
            }
        }
        LOG.info( "Validating quantity for scanReq line: {}", inputLine );
        /*
         * Converting list of Strings to list of integers
         */
        float fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE;
        List<String> tokens;
        if ( null != merchantSpecificSyntaxRegex && !merchantSpecificSyntaxRegex.isEmpty() ) {
            tokens = StringUtils.getMatchedTokens( inputLine, merchantSpecificSyntaxRegex );
        } else {
            tokens = StringUtils.getMatchedTokens( inputLine, QUANTITY_REGEX_INTEGER );
            if ( tokens.isEmpty() ) {
                tokens = StringUtils.getMatchedTokens( inputLine, QUANTITY_UOM_REGEX );
            }
        }

        for ( String token : tokens ) {
            quantitiesMatched.add( token );
        }

        if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.receipt ) ) {
            ReceiptLineItem receiptLineItem = new ReceiptLineItem();
            if ( dummyLine instanceof ReceiptLineItem ) {
                receiptLineItem = (ReceiptLineItem) dummyLine;
            }
            if ( !quantitiesMatched.isEmpty() ) {
                String quantity = quantitiesMatched.get( 0 ).trim();
                int quantityIndex = inputLine.indexOf( " " + quantity + " " );
                int spaceLength = quantityIndex != -1 ? 1 : 0;
                if ( quantityIndex == -1 ) {
                    quantityIndex = inputLine.indexOf( quantity + " " );
                }
                if ( quantityIndex == -1 ) {
                    quantityIndex = inputLine.indexOf( " " + quantity );
                    spaceLength = quantityIndex != -1 ? 1 : 0;
                }

                if ( quantityIndex != -1 ) {
                    if ( !setAllMatches && split[0].trim().equals( "QUANTITY" ) && indexOfSecondSpace != 0
                        && indexOfSecondSpace < quantityIndex ) {
                    } else {
                        shouldAdd = true;
                        indexes.add( quantityIndex );
                    }
                }
                if ( setAllMatches ) {
                    inputLine = StringUtils.replacePatternWithSpaces( inputLine, quantity );
                } else if ( !lineValidator.isOptional() && quantityIndex != -1 && shouldAdd ) {
                    String remainingLine = inputLine.substring( quantityIndex + spaceLength ).trim();
                    List<String> uom = StringUtils.getMatchedTokens( remainingLine, quantity + UomValidator.getUomRegex() );

                    if ( inputLine.toLowerCase().indexOf( quantity + " x " ) != -1 ) {
                        if ( inputLine.indexOf( quantity + " X " ) != -1 ) {
                            inputLine = StringUtils.replacePatternWithSpaces( inputLine, quantity + " X " );
                        } else {
                            inputLine = StringUtils.replacePatternWithSpaces( inputLine, quantity + " x " );
                        }
                    } else if ( inputLine.toLowerCase().indexOf( quantity + "x " ) != -1 ) {
                        if ( inputLine.indexOf( quantity + "X " ) != -1 ) {
                            inputLine = StringUtils.replacePatternWithSpaces( inputLine, quantity + "X " );
                        } else {
                            inputLine = StringUtils.replacePatternWithSpaces( inputLine, quantity + "x " );
                        }
                    } else if ( !uom.isEmpty() ) {
                        inputLine = StringUtils.replacePatternWithSpaces( inputLine, uom.get( 0 ) );
                    } else {
                        inputLine = StringUtils.replacePatternWithSpaces( inputLine, quantity );
                    }

                    try {
                        LOG.debug( "Quantity field confidence: {}", fieldConfidence );
                        dummyLine.addToFieldConfidenceList( fieldConfidence );
                    } catch ( LineExtractionException e ) {
                        LOG.error( "Exception while assigning Quantity field confidence: " + e );
                    }
                }
                quantity = quantity.replace( ",", "." );
                if ( Double.parseDouble( quantity ) != 0.0 && shouldAdd ) {
                    receiptLineItem.setQuantity( Double.parseDouble( quantity ) );
                }
            }
        }
        if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
            InvoiceLineItem invoiceLineItem = new InvoiceLineItem();
            if ( dummyLine instanceof InvoiceLineItem ) {
                invoiceLineItem = (InvoiceLineItem) dummyLine;
            }
            if ( !quantitiesMatched.isEmpty() ) {
                if ( setAllMatches ) {
                    for ( String quantity : quantitiesMatched ) {
                        indexes.add( inputLine.indexOf( quantity ) );
                        inputLine = StringUtils.replacePatternWithSpaces( inputLine, quantity );
                    }
                } else {
                    indexes.add( inputLine.indexOf( quantitiesMatched.get( 0 ) ) );
                    inputLine = inputLine
                        .substring( inputLine.indexOf( quantitiesMatched.get( 0 ) + quantitiesMatched.get( 0 ).length() ) );

                    invoiceLineItem.addToQuantities( quantitiesMatched.get( 0 ).trim() );
                    try {
                        LOG.debug( "Quantity field confidence: {}", ConfidenceCalculator.BASE_CONFIDENCE_VALUE );
                        dummyLine.addToFieldConfidenceList( ConfidenceCalculator.BASE_CONFIDENCE_VALUE );
                    } catch ( LineExtractionException e ) {
                        LOG.error( "Exception while assigning Quantity field confidence: " + e );
                    }
                }
            }
        }
        return inputLine;
    }
}
