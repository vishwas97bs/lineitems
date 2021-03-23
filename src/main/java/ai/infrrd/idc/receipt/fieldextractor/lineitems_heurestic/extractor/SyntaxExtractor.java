package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.DocumentMetaData;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.Domain;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItemHelper;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.ConfidenceCalculator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Constants;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.LineItemExtractorUtils;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.FieldValidator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SyntaxExtractor
{

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( SyntaxExtractor.class );
    public static final String SYNTAX_FIELD_DELIMITER = " | ";
    private static final int MINIMUM_PERCENTAGE_OF_LINES = 50;
    private static final int MAX_DIFFERENCE_BETWEEN_SYNTAXES = 5;


    LineItemExtractorUtils lineItemExtractorUtils;

    @Autowired
    public void setLineItemExtractorUtils(LineItemExtractorUtils lineItemExtractorUtils) {
        this.lineItemExtractorUtils = lineItemExtractorUtils;
    }

    /**
     * This method extracts one syntax for the array of lines provided, based on the fields found in them.
     * It needs the list of default fields to be searched for, in the lines
     * @param lines The array of lines of which, syntax has to be found
     * @param metaData
     * @param confidenceCalculator
     * @return string syntax with fields separated by |
     */
    public  String extractSyntax(final String[] lines, DocumentMetaData metaData, FieldExtractionRequest helper,
                                       ConfidenceCalculator confidenceCalculator,Map<String,Object> configuration )
    {
        LOG.debug( "Scanning through all the lines to get the syntax for domain: {} and scanRequestId {}", metaData.getDomain(),
            helper.getRequestId() );

        // Map that contains syntax - as key and list of line numbers with that syntax - as value
        Map<String, List<Integer>> syntaxInLinesMap = new LinkedHashMap<>();

        // Final syntax of the document
        String finalSyntaxOfDocument = null;

        for ( int lineCounter = 0; lineCounter < lines.length; lineCounter++ ) {

            // Check if any syntax has reached the max threshold
            if ( lineCounter >= lines.length * MINIMUM_PERCENTAGE_OF_LINES / 100 ) {
                finalSyntaxOfDocument = getSyntaxMajorityOfDocument( syntaxInLinesMap,
                    lines.length * MINIMUM_PERCENTAGE_OF_LINES / 100 );
                confidenceCalculator.setSyntaxConfidence( ConfidenceCalculator.BASE_CONFIDENCE_VALUE );
            }
            if ( finalSyntaxOfDocument == null ) {

                // Map that contains index - as key and the field identified to be at that index - as value
                HashMap<Integer, Constants.LINE_ITEM_FIELDS> indexOfFieldMap = new HashMap<>();
                FieldValidator fieldValidator = null;
                String originalLine = lines[lineCounter];

                LineItemHelper lineItemHelper = new LineItemHelper( metaData, helper );

                for ( Constants.LINE_ITEM_FIELDS fieldToBeFound : metaData.getDomain().getDefaultFields() ) {
                    List<Integer> indexes = new ArrayList<>();
                    fieldValidator = lineItemExtractorUtils.getFieldsValidatorFromFieldName( fieldToBeFound,
                        metaData.getDomain() );
                    // Update line after all values of a field are extracted from line
                    lineItemHelper.setLineValidator( new LineValidator( originalLine ) );
                    LOG.trace( "Checking original line : {} for field {} ", originalLine, fieldToBeFound );
                    originalLine = fieldValidator.setField( lineItemExtractorUtils.initializeLineObject( metaData ), indexes,
                        true, lineItemHelper.getLineValidator(), "", metaData,helper ,configuration );

                    // Update map of syntaxes
                    for ( int index : indexes ) {
                        if ( indexOfFieldMap.get( index ) == null ) {
                            LOG.trace( "Found field {} at index {}", fieldToBeFound, index );
                            indexOfFieldMap.put( index, fieldToBeFound );
                        } else {
                            LOG.warn( "Something wrong with syntax detection. field {} also found at index {}", fieldToBeFound,
                                index );
                        }
                    }
                    if ( indexes.isEmpty() && fieldValidator.isCompulsory() ) {
                        LOG.trace( "Compulsory field {} missing, skipping line {}", fieldToBeFound, originalLine );
                        indexOfFieldMap.clear();
                        break;
                    }
                }
                //  Update map of syntax and line numbers
                if ( !indexOfFieldMap.isEmpty() ) {
                    String syntaxOfCurrentLine = createSyntaxOfLine( indexOfFieldMap );
                    if ( syntaxInLinesMap.containsKey( syntaxOfCurrentLine ) ) {
                        syntaxInLinesMap.get( syntaxOfCurrentLine ).add( lineCounter );
                    } else {
                        LOG.trace( "Syntax {} found : {}", syntaxInLinesMap.size() + 1, syntaxOfCurrentLine );
                        List<Integer> lineNumbers = new ArrayList<>();
                        lineNumbers.add( lineCounter );
                        syntaxInLinesMap.put( syntaxOfCurrentLine, lineNumbers );
                    }
                } else {
                    LOG.debug( "Ignoring syntax for line number {}", lineCounter );
                }
            }
        }
        if ( finalSyntaxOfDocument == null ) {
            LOG.debug( "Falling back to other ways of getting syntax..since 50% of lines do not have same syntax." );
            finalSyntaxOfDocument = getProbableSyntaxOfDocument( syntaxInLinesMap, metaData, confidenceCalculator );
        }
        if ( finalSyntaxOfDocument == null ) {
            if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.bank ) ) {
                finalSyntaxOfDocument = Constants.LINE_ITEM_FIELDS.DATE + SyntaxExtractor.SYNTAX_FIELD_DELIMITER + Constants.LINE_ITEM_FIELDS.DATE
                    + SyntaxExtractor.SYNTAX_FIELD_DELIMITER + Constants.LINE_ITEM_FIELDS.TEXT + SyntaxExtractor.SYNTAX_FIELD_DELIMITER
                    + Constants.LINE_ITEM_FIELDS.AMOUNT;
            } else {
                finalSyntaxOfDocument = Constants.LINE_ITEM_FIELDS.TEXT + SyntaxExtractor.SYNTAX_FIELD_DELIMITER
                    + Constants.LINE_ITEM_FIELDS.AMOUNT;
            }
        }
        LOG.debug( "Found syntax : {}", finalSyntaxOfDocument );
        return finalSyntaxOfDocument;
    }


    /**
     * This method gets the most probable syntax of a document based on the syntaxes collected for the whole receipt
     * @param syntaxInLinesMap map of syntaxes and its line numbers
     * @param metaData the documentMetaData
     * @return the most probable syntax
     */
    private String getProbableSyntaxOfDocument( Map<String, List<Integer>> syntaxInLinesMap, DocumentMetaData metaData,
        ConfidenceCalculator confidenceCalculator )
    {
        LOG.trace( "Method: getProbableSyntaxOfDocument called." );
        Domain domain = metaData.getDomain();
        if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
            LOG.trace( "For invoice domain, removing syntax containing only one amount if multiple amount syntax is present" );
            syntaxInLinesMap = removeSyntaxesContainingOnlyOneAmountIfMultipleAmountSyntaxPresent( syntaxInLinesMap );
        }
        LOG.trace( "syntaxInLinesMap: {}", syntaxInLinesMap );
        if ( syntaxInLinesMap.size() == 0 ) {
            return null;
        }
        String finalSyntax = null;


        Set<String> syntaxesWithSameMax = getSyntaxesWithMaxLines( syntaxInLinesMap );
        LOG.trace( "syntaxesWithSameMax: {}", syntaxesWithSameMax );
        String maxCountSyntax = syntaxesWithSameMax.iterator().next();

        if ( syntaxInLinesMap.size() == 1 ) {
            confidenceCalculator.setSyntaxConfidence( ConfidenceCalculator.BASE_CONFIDENCE_VALUE );
            LOG.debug( "Only one syntax is present in the syntax map, hence setting it as final syntax {}", maxCountSyntax );
            finalSyntax = maxCountSyntax;
        }
        /*
         *  First try : Get syntax with max count if the difference between the second max and the first max is > MAX_DIFFERENCE_BETWEEN_SYNTAXES
         */
        LOG.trace( "First try : Get syntax with max count if the difference between the second max and the first max is > {}",
            MAX_DIFFERENCE_BETWEEN_SYNTAXES );
        if ( null == finalSyntax && syntaxesWithSameMax.size() == 1 ) {
            List<Integer> linesWithMaxSyntax = syntaxInLinesMap.remove( maxCountSyntax );
            Set<String> syntaxesWithSameSecondMax = getSyntaxesWithMaxLines( syntaxInLinesMap );
            //Adding it back to the map
            syntaxInLinesMap.put( maxCountSyntax, linesWithMaxSyntax );
            if ( syntaxesWithSameSecondMax.size() == 1 ) {
                String secondMaxCountSyntax = syntaxesWithSameSecondMax.iterator().next();
                List<Integer> linesWithSecondMaxSyntax = syntaxInLinesMap.get( secondMaxCountSyntax );
                if ( linesWithMaxSyntax.size() - linesWithSecondMaxSyntax.size() >= MAX_DIFFERENCE_BETWEEN_SYNTAXES ) {
                    confidenceCalculator.setSyntaxConfidence( ConfidenceCalculator.BASE_CONFIDENCE_VALUE );
                    LOG.debug(
                        "Lines with max syntax {}, is greater than lines with second max syntax: {} by Max difference: {} , Hence setting {} as syntax",
                        linesWithMaxSyntax, linesWithSecondMaxSyntax, MAX_DIFFERENCE_BETWEEN_SYNTAXES, maxCountSyntax );
                    finalSyntax = maxCountSyntax;
                }
            }
        }
        Set<String> syntaxesWithMaxFields = getSyntaxesWithMaxFields( syntaxInLinesMap );
        if ( syntaxesWithMaxFields.size() == 1 && ( syntaxInLinesMap.get( syntaxesWithMaxFields.iterator().next() ).size() != 1
            || syntaxInLinesMap.get( syntaxesWithMaxFields.iterator().next() ).size() == syntaxInLinesMap
                .get( syntaxesWithSameMax.iterator().next() ).size() ) ) {
            /*
             * Second try : get the single syntax with max number of fields present in it and has atleast 2 lines with this syntax
             */
            finalSyntax = syntaxesWithMaxFields.iterator().next();
            LOG.debug(
                "Second try : Setting the single syntax with max number of fields present in it and has atleast 2 lines with this syntax as final syntax :{}",
                finalSyntax );
            confidenceCalculator
                .setSyntaxConfidence( ConfidenceCalculator.BASE_CONFIDENCE_VALUE - ConfidenceCalculator.MINIMUM_CONF_DIFF );
        } else if ( null == finalSyntax && syntaxesWithMaxFields.size() > 1 ) {
            int max = 0;
            String maxSyntax = null;
            boolean maxRepeat = false;
            /*
             * Third try : get the max - field syntax that has max number of lines
             */
            for ( String syntaxWithMaxFields : syntaxesWithMaxFields ) {
                if ( syntaxInLinesMap.get( syntaxWithMaxFields ).size() > max ) {
                    max = syntaxInLinesMap.get( syntaxWithMaxFields ).size();
                    maxSyntax = syntaxWithMaxFields;
                    maxRepeat = false;
                } else if ( syntaxInLinesMap.get( syntaxWithMaxFields ).size() == max ) {
                    maxRepeat = true;
                }
            }
            if ( maxSyntax != null && !maxRepeat ) {
                LOG.debug( "Third try : Getting the max - field syntax that has max number of lines as final syntax: {}",
                    maxSyntax );
                confidenceCalculator.setSyntaxConfidence(
                    ConfidenceCalculator.BASE_CONFIDENCE_VALUE - 2 * ConfidenceCalculator.MINIMUM_CONF_DIFF );
                finalSyntax = maxSyntax;
            }

            /*
             * Fourth try : get the syntax that follows the default syntax with the highest priority
             */
            if ( finalSyntax == null ) {
                for ( String defaultSyntax : domain.getSyntaxesInOrder() ) {
                    for ( String syntaxWithSameMax : syntaxesWithMaxFields ) {
                        if ( defaultSyntax.equals( syntaxWithSameMax ) ) {
                            finalSyntax = syntaxWithSameMax;
                            LOG.debug(
                                "Fourth try : Getting the syntax that follows the default syntax with the highest priority as final syntax: {}",
                                finalSyntax );
                            confidenceCalculator.setSyntaxConfidence(
                                ConfidenceCalculator.BASE_CONFIDENCE_VALUE - 3 * ConfidenceCalculator.MINIMUM_CONF_DIFF );
                            break;
                        }
                    }
                }
            }

        }
        if ( finalSyntax == null && maxCountSyntax != null ) {
            /*
             * Fifth try : just pick the first syntax with max number of lines
             */
            finalSyntax = maxCountSyntax;
            LOG.debug( "Fifth try : just picking the first syntax with max number of lines as final syntax: {}", finalSyntax );
            confidenceCalculator
                .setSyntaxConfidence( ConfidenceCalculator.BASE_CONFIDENCE_VALUE - 4 * ConfidenceCalculator.MINIMUM_CONF_DIFF );
        }

        if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.receipt ) ) {
            finalSyntax = considerOptionalFields( finalSyntax, syntaxInLinesMap.keySet(), metaData.getDomain() );
        }
        LOG.debug( "Obtained syntax after considering optional fields: {}", finalSyntax );

        LOG.trace( "Method: getProbableSyntaxOfDocument finished." );

        if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
            metaData.setLinesHavingMatchedSyntax(
                setMetadataWithLinesHavingMatchedSyntaxes( syntaxInLinesMap, finalSyntax, syntaxInLinesMap.keySet() ) );
        }
        return finalSyntax;
    }


    private  List<Integer> setMetadataWithLinesHavingMatchedSyntaxes( Map<String, List<Integer>> syntaxInLinesMap,
        String finalSyntax, Set<String> syntaxes )
    {
        List<Integer> linesWithMatchedSyntaxes = new ArrayList<>();
        linesWithMatchedSyntaxes.addAll( syntaxInLinesMap.get( finalSyntax ) );
        for ( String syntax : syntaxes ) {
            if ( !finalSyntax.equals( syntax ) && checkIfFinalSyntaxSubstringOfSyntax( syntax, finalSyntax ) ) {
                linesWithMatchedSyntaxes.addAll( syntaxInLinesMap.get( syntax ) );
            }
        }
        return linesWithMatchedSyntaxes;
    }


    public  String considerOptionalFields( String finalSyntax, Set<String> syntaxes, Domain domain )
    {
        LOG.trace( "Considering optional fields in syntax" );
        for ( String syntax : syntaxes ) {
            if ( !finalSyntax.equals( syntax )
                && syntax.split( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED ).length > finalSyntax
                    .split( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED ).length
                && checkIfFinalSyntaxSubstringOfSyntax( finalSyntax, syntax ) ) {
                List<String> finalSyntaxList = new LinkedList<>();
                String[] finalSyntaxFields = finalSyntax.split( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED );
                String syntaxSubString = syntax.substring( syntax.indexOf( finalSyntaxFields[0] ) );
                String[] syntaxFields = syntaxSubString.split( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED );
                //Checks if the optional fields are in between the final syntax
                LOG.trace( "Checking if the optional field is in between final syntax fields" );
                for ( int index = 0; index < finalSyntaxFields.length; index++ ) {
                    if ( finalSyntaxFields[index].equals( syntaxFields[index] ) && finalSyntaxFields.length > index + 1
                        && syntaxFields.length > index + 2 && finalSyntaxFields[index + 1].equals( syntaxFields[index + 2] )
                        && checkIfFieldIsOptional( syntaxFields[index + 1], domain ) ) {
                        finalSyntaxList.add( syntaxFields[index] );
                        LOG.trace( "Adding Optional fields {}", syntaxFields[index + 1] );
                        finalSyntaxList.add( syntaxFields[index + 1] + Constants.OPTIONAL_FIELD_SUFFIX );
                    } else {
                        finalSyntaxList.add( finalSyntaxFields[index] );
                    }
                }
                finalSyntax = createSyntaxLineFromList( finalSyntaxList );
                //Checks is optional fields are at the beginning of the final syntax
                LOG.trace( "Checking if the optional field is at the beginning of the final syntax" );
                String initialSyntaxStr = syntax.substring( 0, syntax.indexOf( finalSyntaxFields[0] ) );
                String optionalField = initialSyntaxStr.replaceFirst( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED, "" ).trim();
                if ( !initialSyntaxStr.isEmpty()
                    && !initialSyntaxStr.replaceFirst( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED, "" ).contains( "|" )
                    && checkIfFieldIsOptional( optionalField, domain ) ) {
                    finalSyntaxList.add( 0, optionalField + Constants.OPTIONAL_FIELD_SUFFIX );
                    LOG.trace( "Adding Optional fields {}", optionalField );
                    finalSyntax = createSyntaxLineFromList( finalSyntaxList );
                }

                //Checks is optional fields are at the end of the final syntax
                LOG.trace( "Checking if the optional field is at the end of the final syntax" );
                initialSyntaxStr = syntax.substring( syntax.lastIndexOf( " | " ) );
                optionalField = initialSyntaxStr.replaceFirst( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED, "" ).trim();
                if ( !initialSyntaxStr.isEmpty() && checkIfFieldIsOptional( optionalField, domain ) ) {
                    finalSyntaxList.add( optionalField + Constants.OPTIONAL_FIELD_SUFFIX );
                    LOG.trace( "Adding Optional fields {}", optionalField );
                    finalSyntax = createSyntaxLineFromList( finalSyntaxList );
                }
            }
        }
        finalSyntax = makeQuantityOptionalIfOccursBeforePrice( finalSyntax );
        return finalSyntax;
    }


    private  String makeQuantityOptionalIfOccursBeforePrice( String finalSyntax )
    {
        String finalSyntaxWithQtyOptional = finalSyntax;
        if ( finalSyntax.contains(
            Constants.LINE_ITEM_FIELDS.QUANTITY.name() + SyntaxExtractor.SYNTAX_FIELD_DELIMITER + Constants.LINE_ITEM_FIELDS.AMOUNT.name() ) ) {
            LOG.trace( "Making Quantity Optional as it Occurs Before Price" );
            finalSyntaxWithQtyOptional = finalSyntax.replace( Constants.LINE_ITEM_FIELDS.QUANTITY.name(),
                Constants.LINE_ITEM_FIELDS.QUANTITY.name() + Constants.OPTIONAL_FIELD_SUFFIX );
        }
        return finalSyntaxWithQtyOptional;
    }


    private  String createSyntaxLineFromList( List<String> finalSyntaxList )
    {
        StringBuilder finalSyntax = new StringBuilder();
        for ( String finalSyntaxField : finalSyntaxList ) {
            finalSyntax.append( " | " + finalSyntaxField );
        }
        return String.valueOf( finalSyntax ).replaceFirst( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED, "" );
    }


    private  boolean checkIfFieldIsOptional( String syntaxField, Domain domain )
    {
        LOG.trace( "Checking if {} field is defined as optional for the domain :{}", syntaxField, domain.getName() );
        return lineItemExtractorUtils
            .getFieldsValidatorFromFieldName( Constants.LINE_ITEM_FIELDS.valueOf( syntaxField.trim() ), domain ).isOptional();
    }


    private  boolean checkIfFinalSyntaxSubstringOfSyntax( String finalSyntax, String syntax )
    {
        boolean ifFinalSyntaxSubstring = true;
        String[] finalSyntaxFields = finalSyntax.split( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED );
        for ( String finalSyntaxField : finalSyntaxFields ) {
            if ( syntax.contains( finalSyntaxField ) ) {
                syntax = syntax.substring( syntax.indexOf( finalSyntaxField ) + finalSyntaxField.length() );
            } else {
                ifFinalSyntaxSubstring = false;
                break;
            }
        }
        LOG.trace( "Final Syntax: {} is substring of {} :{}", finalSyntax, syntax, ifFinalSyntaxSubstring );
        return ifFinalSyntaxSubstring;
    }


    private  Map<String, List<Integer>> removeSyntaxesContainingOnlyOneAmountIfMultipleAmountSyntaxPresent(
        Map<String, List<Integer>> syntaxInLinesMap )
    {
        Map<String, List<Integer>> newSyntaxInLinesMap = new LinkedHashMap<>( syntaxInLinesMap );
        for ( String syntax : syntaxInLinesMap.keySet() ) {
            if ( !syntax.replaceFirst( Constants.LINE_ITEM_FIELDS.AMOUNT.name(), "" ).contains( Constants.LINE_ITEM_FIELDS.AMOUNT.name() ) ) {
                newSyntaxInLinesMap.remove( syntax );
            }
        }
        if ( newSyntaxInLinesMap.isEmpty() ) {
            newSyntaxInLinesMap = syntaxInLinesMap;
        }
        return newSyntaxInLinesMap;
    }


    private  Set<String> getSyntaxesWithMaxFields( Map<String, List<Integer>> syntaxInLinesMap )
    {
        LOG.trace( "Method: getSyntaxesWithMaxFields called." );
        int max = 0;
        Set<String> syntaxesWithSameFields = new HashSet<>();
        for ( String syntaxEntry : syntaxInLinesMap.keySet() ) {
            int count = syntaxEntry.split( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED ).length;
            if ( count > max ) {
                syntaxesWithSameFields.clear();
                max = count;
                syntaxesWithSameFields.add( syntaxEntry );
            } else if ( count == max ) {
                syntaxesWithSameFields.add( syntaxEntry );
            }
        }

        LOG.trace( "Method: getSyntaxesWithMaxFields finished." );
        return syntaxesWithSameFields;
    }


    /**
     * This method gets Syntaxes with max number of line numbers present in it
     * @param syntaxInLinesMap Map of syntax and list of line numbers
     * @return set of syntaxes that have the max no of lines
     */
    private  Set<String> getSyntaxesWithMaxLines( Map<String, List<Integer>> syntaxInLinesMap )
    {
        LOG.trace( "Method: getSyntaxesWithMaxLines called." );

        int max = 0;
        Set<String> syntaxesWithSameMax = new LinkedHashSet<>();
        for ( Map.Entry<String, List<Integer>> syntaxEntry : syntaxInLinesMap.entrySet() ) {
            if ( syntaxEntry.getValue().size() > max ) {
                syntaxesWithSameMax.clear();
                max = syntaxEntry.getValue().size();
                syntaxesWithSameMax.add( syntaxEntry.getKey() );
            } else if ( syntaxEntry.getValue().size() == max ) {
                syntaxesWithSameMax.add( syntaxEntry.getKey() );
            }
        }
        LOG.trace( "Method: getSyntaxesWithMaxLines finished." );

        return syntaxesWithSameMax;
    }


    /**
     * This method creates syntax string containing fields in order, separated by a delimiter by sorting the indexes of the fields found in line
     * @param indexOfFieldMap the map containing indexes of fields as keys and corresponding fields found, as values
     * @return the syntax string created
     */
    private  String createSyntaxOfLine( HashMap<Integer, Constants.LINE_ITEM_FIELDS> indexOfFieldMap )
    {
        LOG.trace( "Method: createSyntaxOfLine called." );

        if ( indexOfFieldMap.size() == 0 ) {
            return null;
        }

        List<Integer> indexes = new ArrayList<>( indexOfFieldMap.keySet() );
        Collections.sort( indexes );
        StringBuilder syntax = new StringBuilder();
        for ( int index : indexes ) {
            syntax.append( SYNTAX_FIELD_DELIMITER ).append( indexOfFieldMap.get( index ) );
        }

        LOG.trace( "Method: createSyntaxOfLine finished." );
        return syntax.toString().replaceFirst( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED, "" );
    }


    /**
     * This method checks if the size of the line numbers following a particular syntax has reached the max limit or not
     * @param identifiedSyntaxes map of syntaxes and lines
     * @param max the threshold value for lines (the max limit)
     * @return the syntax that has reached the threshold value
     */
    private String getSyntaxMajorityOfDocument( Map<String, List<Integer>> identifiedSyntaxes, int max )
    {
        for ( Map.Entry<String, List<Integer>> syntaxEntry : identifiedSyntaxes.entrySet() ) {
            if ( syntaxEntry.getValue().size() == max ) {
                LOG.debug( "Found {} lines which is the max count with syntax {}", max, syntaxEntry.getKey() );
                return syntaxEntry.getKey();
            }
        }
        return null;
    }
}
