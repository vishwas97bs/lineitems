package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor;

import ai.infrrd.idc.commons.datastructures.Tuple2;
import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.ConfigService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.GimletConfigService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.ProductNameValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.UomValidator;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Component
public class ReceiptLineItemExtractor implements InitializingBean
{

    private ConfigService configService;

    private GimletConfigService gimletConfigService;

    private LineItemExtractorUtils lineItemExtractorUtils;

    private ReceiptDomain receiptDomain;

    private MetaDataExtractor metaDataExtractor;

    private Utils utils;


    @Autowired
    public void setConfigService( ConfigService configService )
    {
        this.configService = configService;
    }


    @Autowired
    public void setGimletConfigService( GimletConfigService gimletConfigService )
    {
        this.gimletConfigService = gimletConfigService;
    }


    @Autowired
    public void setLineItemExtractorUtils( LineItemExtractorUtils lineItemExtractorUtils )
    {
        this.lineItemExtractorUtils = lineItemExtractorUtils;
    }


    @Autowired
    public void setReceiptDomain( ReceiptDomain receiptDomain )
    {
        this.receiptDomain = receiptDomain;
    }


    @Autowired
    public void setMetaDataExtractor( MetaDataExtractor metaDataExtractor )
    {
        this.metaDataExtractor = metaDataExtractor;
    }


    @Autowired
    public void setUtils( Utils utils )
    {
        this.utils = utils;
    }

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( ReceiptLineItemExtractor.class );
    private static final int MAX_ACCEPTABLE_LINE_DIFFERENCE = 10;
    private static final int NO_OF_EMPTY_LINES_TO_STOP_LOOKING_LINE_ITEM_AFTER = 10;


    /**
     * Extracts Line objects for receipt from the text
     * @param             extractedInputData
     * @param helper
     * @return Extracted Line Objects
     */
    @SuppressWarnings ( "unchecked")
    public LineItemResponse getLineObjects( Map<String, Object> extractedInputData, FieldExtractionRequest helper,
        FieldConfiguration fieldConfiguration )
    {
        Map<String, Object> configuration = gimletConfigService.getGimletConfig();
        LineItemResponse lineItemResponse = new LineItemResponse();
        String scanRequestId = helper.getRequestId();
        boolean isAbsoluteConfidence = false;
        if ( fieldConfiguration != null ) {
            isAbsoluteConfidence = fieldConfiguration.isAbsConfidence();
        }
        String text = helper.getOcrData().getRawText();
        LOG.trace( "Text got for LI extraction: {}", text );
        if ( text == null || text.isEmpty() ) {
            return null;
        }
        final String originalExtractedText = text.replaceAll( "\\ufffd", "" ).replace( "?", " " ).replace( "\"", "" );

        ConfidenceCalculator confidenceCalculator = new ConfidenceCalculator();
        LOG.debug( "Base for confidence calculation: {}", ConfidenceCalculator.BASE_CONFIDENCE_VALUE );

        /**
         *  Get details about receipt
         */
        DocumentMetaData metaData = metaDataExtractor.getDocumentMetaDataFromTextForReceipts( helper,
            (Map<String, Object>) extractedInputData.get( Fields.MERCHANT_NAME.getValue() ), configuration );

        Tuple2<String, String> tuple = getShortenedTextForReceipts(
            StringUtils.replaceMultipleSpacesBySingleSpaceEscapingNextLine( originalExtractedText ), metaData, helper,
            configuration );
        String shortenedText = tuple.f1();
        String rawShortenedText = tuple.f2();
        /**
         *  Get lines from the receipt text as string array
         */
        String[] lines = shortenedText.split( Constants.LINE_SEPARATOR_REGEX );
        String[] rawLines = rawShortenedText.split( Constants.LINE_SEPARATOR_REGEX );
        LOG.debug( "Scanning {} lines from the shortened text for line items for ScanID: {}", lines.length, scanRequestId );

        /**
         *  Get syntax of receipt
         */
        metaData.setSyntax( lineItemExtractorUtils.getSyntax( metaData, lines, helper, confidenceCalculator, configuration ) );
        LOG.info( "Line Item Syntax got: {} with confidence: {} for ScanId: {}", metaData.getSyntax(),
            confidenceCalculator.getSyntaxConfidence(), scanRequestId );

        List<LineItem> lineItems = null;
        int noOfLineExtracted = 0;

        if ( metaData.getSyntax() != null ) {
            /**
             *  Use syntax to get fields in each line
             */
            lineItems = extractLinesUsingSyntax( lines, rawLines, metaData, helper, confidenceCalculator, configuration,
                fieldConfiguration );

            //Calculate the overall line items confidence
            LOG.trace( "Updating line with Line number, confidence, default quantityUnit, etc" );
            lineItemExtractorUtils.updateLineItems( lineItems, isAbsoluteConfidence );
            noOfLineExtracted = lineItems.size();
            LOG.info( "Found {} line items for scanId : {} ", lineItems.size(), scanRequestId );
        }
        lineItemResponse.setConfidence(
            calculateOverallLineItemConfidence( shortenedText, confidenceCalculator, metaData, noOfLineExtracted, lineItems ) );
        lineItemResponse.setLineItems( lineItems );
        return lineItemResponse;

    }


    private float calculateOverallLineItemConfidence( String shortenedText, ConfidenceCalculator confidenceCalculator,
        DocumentMetaData metaData, int noOfLineExtracted, List<LineItem> lineItems )
    {
        float overAllLineItemConfidence;
        float noOfLinesPresent = new PriceFormatwiseExtractor().getCountOfLinesWithPrice( shortenedText );
        LOG.trace( "Number of lines expected as according to price regex matches: {}", noOfLinesPresent );
        float lineCountConfidence = noOfLinesPresent == 0 ? 0 : noOfLineExtracted / noOfLinesPresent;
        LOG.trace( "Setting {} as line count confidence", lineCountConfidence );
        confidenceCalculator.setLineCountConfidence( lineCountConfidence );

        float noOfLinesWithMatchedSyntax = metaData.getLinesHavingMatchedSyntax().size();
        float matchedSyntaxLinesConfidence = noOfLinesPresent == 0 ? 0 : noOfLinesWithMatchedSyntax / noOfLinesPresent;
        LOG.trace( "Number of lines with perfect match for syntax: {}", noOfLinesWithMatchedSyntax );
        LOG.trace( "Setting {} as matched Syntax Lines Confidence", matchedSyntaxLinesConfidence );
        confidenceCalculator.setMatchedSyntaxLinesConfidence( matchedSyntaxLinesConfidence );

        List<Float> lineConfidences = new ArrayList<>();
        for ( LineItem lineItem : lineItems ) {
            lineConfidences.add( lineItem.getLineConfidence() );
        }
        float averageOfLineItemConfidences = utils.getAverageFromListValues( lineConfidences );
        LOG.trace( "Average of line item confidences: {}", averageOfLineItemConfidences );

        overAllLineItemConfidence = ( ConfidenceCalculator.LINES_COUNT_CONFIDENCE_WEIGHT
            * confidenceCalculator.getLineCountConfidence() )
            + ( ConfidenceCalculator.LINES_SYNTAX_MATCH_COUNT_CONFINENCE_WEIGHT
                * confidenceCalculator.getMatchedSyntaxLinesConfidence() )
            + ( ConfidenceCalculator.AVERAGE_LINES_CONFINEDCE_WEIGHT * averageOfLineItemConfidences );
        return utils.roundUptoNDecimals( ( overAllLineItemConfidence * ConfidenceCalculator.BASE_CONFIDENCE_VALUE ), 2 );
    }


    private List<LineItem> extractLinesUsingSyntax( String[] lines, String[] rawLines, DocumentMetaData metaData,
        FieldExtractionRequest helper, ConfidenceCalculator confidenceCalculator, Map<String, Object> configuration,
        FieldConfiguration fieldConfiguration )
    {

        List<LineItem> lineObjects = new LinkedList<>();
        String[] syntaxOrder = metaData.getSyntax().split( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED );
        ReceiptLineItem previousLineObj = null;
        ReceiptLineItem previousGroupObj = null;
        List<Integer> perfectMatchedSyntaxLines = new ArrayList<>();

        boolean isMerchantsWithExtraInfoInPreviousLine = receiptDomain.checkForMerchantsWithExtraInfoInPreviousLine( metaData,
            configuration );
        //valuable data such as product id in next line. selective data to be extracted from below.
        boolean isMerchantsWithExtraInfoInNextLine = checkForMerchantsWithExtraInfoInNextLine( metaData, helper,
            configuration );
        boolean isMerchantsWithMultilineDescription = checkForMerchantsWithMultilineProductDescription( metaData, helper,
            configuration );
        Map<String, String> merchantWithFieldInDescriptionRegex = getRegexForMerchantWithFieldInDescription( metaData, helper,
            configuration );
        //use when line item objects are spread over more than 2 continousline
        boolean isMerchantWithMultilineLineItem = checkForMerchantsWithMultilineLineItem( metaData, helper, configuration );
        String fieldsToLookForMerchant = "";
        if ( isMerchantWithMultilineLineItem ) {
            fieldsToLookForMerchant = getFieldsForMultilineLineitems( helper, metaData, configuration );
        }

        boolean doesAnyLineMatchFieldRegex = Arrays.stream( lines ).anyMatch( line -> {
            if ( merchantWithFieldInDescriptionRegex == null )
                return false;
            return merchantWithFieldInDescriptionRegex.keySet().stream().anyMatch( field -> {
                return ( null != StringUtils.getMatchedTokens( line, merchantWithFieldInDescriptionRegex.get( field ) ) );
            } );
        } );

        if ( doesAnyLineMatchFieldRegex ) {
            for ( int i = 0; i < lines.length; i++ ) {
                if ( !StringUtils
                    .getMatchedTokens( lines[i].toLowerCase(),
                        merchantWithFieldInDescriptionRegex.get( merchantWithFieldInDescriptionRegex.keySet().toArray()[0] ) )
                    .isEmpty() ) {
                    int j = i - 1;
                    while ( j > 0 ) {
                        if ( lines[j].toLowerCase().matches( "\\s*" ) )
                            j--;
                        else
                            break;
                    }
                    if ( j > 0 ) {
                        String temp = lines[j + 1];
                        lines[j + 1] = lines[i];
                        lines[i] = temp;
                    }
                }
            }
        }

        if ( isMerchantsWithMultilineDescription ) {
            for ( int linenumber = 0; linenumber < lines.length; linenumber++ ) {
                String doubleDecimalRegex = "[$]{1,2}[\\d]+[.]{2}[\\d]+";
                List<String> doubleDecimalMatches = StringUtils.getMatchedTokens( lines[linenumber], doubleDecimalRegex );

                for ( int i = 0; i < doubleDecimalMatches.size(); i++ ) {
                    String tobeReplaced = doubleDecimalMatches.get( i );
                    String toreplaceWith = tobeReplaced.replace( "[$]{2}", "$" );

                    String[] allSplit = toreplaceWith.split( "\\.\\." );
                    String decimalPart = allSplit[0];
                    String nondecimalPart = allSplit[1];
                    nondecimalPart = nondecimalPart.substring( nondecimalPart.length() / 2 );

                    decimalPart = decimalPart.substring( 1 )
                        .substring( decimalPart.substring( 1 ).length() - ( decimalPart.substring( 1 ).length() / 2 ) );
                    String tobeReplacedWith = "$" + decimalPart + "." + nondecimalPart;
                    lines[linenumber] = lines[linenumber].replace( tobeReplaced, tobeReplacedWith );

                }
            }
        }

        LOG.debug(
            "Line confidence will be calculated with syntaxConfidence weightage:{} , average fields confidence weightage:{} , and line syntax match confidence weightage: {}",
            ConfidenceCalculator.LINE_SYNTAX_CONFIDENCE_WEIGHTAGE, ConfidenceCalculator.LINE_AVG_FIELDS_CONFIDENCE_WEIGHTAGE,
            ConfidenceCalculator.LINE_SYNTAX_MATCH_CONFIDENCE_WEIGHTAGE );

        Tuple2<Boolean, Boolean> alignment = checkMultilineDescriptionAlignment( lines, syntaxOrder, metaData,
            perfectMatchedSyntaxLines, helper );

        boolean isPreviousLineEmpty = false, isMultilineDescriptionBelow = alignment.f1(),
            isMultilineDescriptionAbove = alignment.f2();
        int previousLineProcessed = 0, consecutiveEmptyLines = 0;
        int lineItemWidth = Arrays.stream( rawLines ).reduce( ( a, b ) -> a.length() > b.length() ? a : b ).orElse( "" )
            .length();
        int firstLineItemLine = -1;
        // Loop through each line in the text
        for ( int lineNo = 0; lineNo < lines.length; lineNo++ ) {
            //to get the line of first line-item
            if ( lineObjects.size() == 1 && previousLineProcessed != 0 && firstLineItemLine == -1
                && ( lineObjects.get( 0 ).getLineNumber() - previousLineProcessed ) < 3 ) {
                firstLineItemLine = previousLineProcessed;
            } else if ( firstLineItemLine == -1 && lineObjects.size() == 1 ) {
                firstLineItemLine = lineObjects.get( 0 ).getLineNumber();
            }

            if ( lineItemExtractorUtils.isLineContainsBannedWords( lines[lineNo],
                lineItemExtractorUtils.getMerchantSpecificBannedWords( metaData.getMerchantName(), configuration ) ) )
                continue;

            consecutiveEmptyLines = ( lines[lineNo].isEmpty() ) ? ++consecutiveEmptyLines : 0;
            if ( consecutiveEmptyLines > NO_OF_EMPTY_LINES_TO_STOP_LOOKING_LINE_ITEM_AFTER )
                break;
            if ( previousLineObj != null
                && ( lineNo - previousLineObj.getLineNumber() > 2 && lineNo - previousLineProcessed > 2 ) ) {
                previousLineObj = null;
            }
            // Initializations
            ReceiptLineItem currentLineObj = new ReceiptLineItem();
            currentLineObj.setLineNumber( lineNo );

            LineItemHelper lineItemHelper = new LineItemHelper( metaData, helper );
            lineItemHelper.setLineValidator( new LineValidator( lines[lineNo].replaceAll( "\\s{2,}", " " ) + " ",
                rawLines.length == lines.length ? rawLines[lineNo] : null, lineItemWidth ) );

            if ( Boolean.TRUE.equals( fieldConfiguration != null && fieldConfiguration.isText() ) ) {
                currentLineObj.setRawText( lines[lineNo] );
            }
            lineItemExtractorUtils.setLineObjectsFromSyntax( currentLineObj, lineItemHelper, syntaxOrder, metaData.getDomain(),
                perfectMatchedSyntaxLines );

            // TODO : validate check
            if ( currentLineObj.getProductName() == null && currentLineObj.getPrices().size() == 0
                && currentLineObj.getUnitPrice() == null && currentLineObj.getProductId() == null
                && isMerchantsWithMultilineDescription && !isMerchantWithMultilineLineItem ) {
                continue;
            }

            LOG.trace( "Is line valid: {} for Line no: {}", lineItemHelper.getLineValidator().isValidLine(), lineNo );

            if ( isMerchantWithMultilineLineItem ) {
                if ( currentLineObj.getProductName() == null && currentLineObj.getPrices().size() == 0
                    && currentLineObj.getUnitPrice() == null && currentLineObj.getProductId() == null ) {
                    if ( previousLineObj != null ) {
                        currentLineObj = (ReceiptLineItem) previousLineObj;
                        previousLineProcessed = previousLineObj.getLineNumber();
                    }
                    if ( validateCurrentLineItemForFields( currentLineObj, fieldsToLookForMerchant ) ) {
                        setLineConfidence( currentLineObj, confidenceCalculator );
                        lineObjects.add( currentLineObj );
                        previousLineObj = null;
                        LOG.info( "{} added to lineitems", currentLineObj );
                    } else if ( previousGroupObj != null ) {
                        if ( firstGroupValidation( currentLineObj, fieldsToLookForMerchant ) ) {
                            previousGroupObj = currentLineObj;
                        } else {
                            mergeCurrentAndPreviousGroup( currentLineObj, previousGroupObj, fieldsToLookForMerchant );
                            if ( validateCurrentLineItemForFields( previousGroupObj, fieldsToLookForMerchant ) ) {
                                setLineConfidence( previousGroupObj, confidenceCalculator );
                                lineObjects.add( previousGroupObj );
                                previousLineObj = null;
                                previousGroupObj = null;
                                LOG.info( "{} added to lineitems after merging with previous line", previousGroupObj );
                            }
                        }
                    } else if ( firstGroupValidation( currentLineObj, fieldsToLookForMerchant ) ) {
                        previousGroupObj = currentLineObj;
                        previousLineObj = null;
                    } else {
                        LOG.debug( "resetting lineitem {}", currentLineObj );
                        previousLineObj = null;
                        previousGroupObj = null;
                        continue;
                    }
                } else {
                    mergeCurrentAndPreviousLines( currentLineObj, previousLineObj );
                    previousLineProcessed = previousLineObj != null ? previousLineObj.getLineNumber() : previousLineProcessed;
                    if ( currentLineObj.getPrices().size() > 0 )
                        lineItemExtractorUtils.updateReceiptLIPrices( currentLineObj );
                    if ( validateCurrentLineItemForFields( currentLineObj, fieldsToLookForMerchant ) ) {
                        setLineConfidence( currentLineObj, confidenceCalculator );
                        lineObjects.add( currentLineObj );
                        previousLineObj = null;
                        LOG.info( "{} added to lineitems after merging", currentLineObj );
                    } else if ( currentLineObj.getIsCoupon() ) {
                        setLineConfidence( currentLineObj, confidenceCalculator );
                        lineObjects.add( currentLineObj );
                        previousLineObj = null;
                        LOG.info( "Coupon line {} added to lineitems", currentLineObj );
                    } else {
                        previousLineObj = currentLineObj;
                    }
                }
            } else if ( lineItemHelper.getLineValidator().isValidLine() ) {

                // if diff of current line no of previous line no is > 10 then clear the line Objs arr and previous line Obj and lineObjects size<5
                if ( !lineObjects.isEmpty() && null != lineObjects.get( lineObjects.size() - 1 )
                    && null != currentLineObj.getProductName()
                    && currentLineObj.getLineNumber()
                        - lineObjects.get( lineObjects.size() - 1 ).getLineNumber() > MAX_ACCEPTABLE_LINE_DIFFERENCE
                    && lineObjects.size() < 5 ) {
                    LOG.debug( "Refreshing all Line items" );
                    lineObjects = new ArrayList<>();
                }

                lineItemExtractorUtils.updateReceiptLIPrices( currentLineObj );

                if ( !isMerchantsWithMultilineDescription )
                    previousLineObj = checkForPreviousLineValues( previousLineObj, currentLineObj,
                        isMerchantsWithExtraInfoInPreviousLine, metaData, helper, isPreviousLineEmpty, configuration );

                //If line is valid, add line object to the list of line objects
                lineObjects.add( currentLineObj );
                if ( lineObjects.size() == 1 && firstLineItemLine == -1
                    && lineObjects.get( 0 ).getLineNumber() - previousLineProcessed < 3 ) {
                    firstLineItemLine = previousLineObj != null ? previousLineObj.getLineNumber() : firstLineItemLine;
                }
                LOG.trace( "Added Line item with product name {} to array", currentLineObj.getProductName() );

                // Set first line number
                if ( lineObjects.size() == 1 ) {
                    metaData.setFirstLineNumber( lineNo );
                }
                // Keep track of previous line objects
                setLineConfidence( currentLineObj, confidenceCalculator );

                if ( isMerchantsWithMultilineDescription && currentLineObj.getProductName() != null
                    && !currentLineObj.getProductName().isEmpty() && ( currentLineObj.getFinalPrice() == null
                        || currentLineObj.getFinalPrice() == 0 || ( !isMerchantsWithExtraInfoInNextLine ) )
                    && isMultilineDescriptionBelow ) {
                    if ( checkForMultilineProductDescription( currentLineObj, previousLineObj, isMultilineDescriptionBelow,
                        metaData, configuration ) ) {
                        lineObjects.remove( currentLineObj );
                        currentLineObj = (ReceiptLineItem) previousLineObj;
                    }
                }
                if ( isMerchantsWithMultilineDescription && isMultilineDescriptionAbove ) {
                    if ( checkForMultilineProductDescription( currentLineObj, previousLineObj, false, metaData,
                        configuration ) ) {
                        previousLineObj = currentLineObj;
                    }
                }
                if ( !isMerchantsWithExtraInfoInPreviousLine ) {
                    if ( currentLineObj.getQuantity() == 0 )
                        receiptDomain.checkForQuantity( metaData, lines[lineNo], currentLineObj, configuration, false,
                            fieldConfiguration );
                }

                if ( isMerchantsWithMultilineDescription || !isMerchantsWithExtraInfoInPreviousLine ) {
                    previousLineObj = currentLineObj;
                    previousLineProcessed = lineNo;
                } else {
                    previousLineObj = null;
                }

            } else if ( currentLineObj.getProductName() != null && !currentLineObj.getProductName().isEmpty()
                && ( currentLineObj.getFinalPrice() == null || currentLineObj.getFinalPrice() == 0 )
                && isMerchantsWithMultilineDescription ) {
                if ( isMultilineDescriptionBelow
                    && checkForMultilineProductDescription( currentLineObj, previousLineObj, true, metaData, configuration ) ) {
                    previousLineProcessed = lineNo;
                    currentLineObj = previousLineObj;
                } else if ( isMultilineDescriptionAbove && checkForMultilineProductDescription( currentLineObj, previousLineObj,
                    false, metaData, configuration ) ) {
                    previousLineProcessed = lineNo;
                    previousLineObj = currentLineObj;
                }

                if ( currentLineObj != null && currentLineObj.getProductName() != null
                    && !currentLineObj.getProductName().isEmpty() && currentLineObj.getFinalPrice() == null )
                    previousLineObj = currentLineObj;

            } else if ( ( currentLineObj.getProductName() != null && !currentLineObj.getProductName().isEmpty()
                && currentLineObj.getFinalPrice() == null ) || isMerchantsWithExtraInfoInPreviousLine ) {
                if ( currentLineObj.getQuantity() == 0 )
                    receiptDomain.checkForQuantity( metaData, lines[lineNo], currentLineObj, configuration, false,
                        fieldConfiguration );
                setIfPriceOrIdPresent( currentLineObj, lineObjects, metaData.getMerchantName() );
                if ( isMerchantsWithExtraInfoInPreviousLine && isMerchantsWithExtraInfoInNextLine && previousLineObj != null
                    && currentLineObj != null ) {
                    metaData.getDomain().performNextLineOperations( metaData, currentLineObj, previousLineObj, lineObjects,
                        lineItemHelper, configuration, fieldConfiguration );
                }
                previousLineObj = currentLineObj;
                previousLineProcessed = lineNo;

            } else if ( !lineItemHelper.getLineValidator().isLineToReject() && previousLineObj != null
                && lineItemExtractorUtils.isLineProximate( previousLineObj, currentLineObj ) ) {
                metaData.getDomain().performNextLineOperations( metaData, currentLineObj, previousLineObj, lineObjects,
                    lineItemHelper, configuration, fieldConfiguration );
                List<String> extraFieldsInNextLine = utils.getExtraFieldsToPickInNextLine( metaData.getMerchantName(),
                    configuration );
                setLineConfidence( previousLineObj, confidenceCalculator );
                if ( extraFieldsInNextLine != null && !extraFieldsInNextLine.isEmpty() ) {
                    if ( extraFieldsInNextLine.contains( "TEXT" ) && currentLineObj.getProductName() == null ) {
                        previousLineObj = null;
                    }
                }
            } else {
                LOG.debug( "Ignoring line..{}", lineNo );
            }


            if ( doesAnyLineMatchFieldRegex ) {
                String fieldName = (String) merchantWithFieldInDescriptionRegex.keySet().toArray()[0];
                if ( getFieldValue( currentLineObj, fieldName ) == null && currentLineObj.getProductName() != null ) {
                    String productName = currentLineObj.getProductName();
                    List<String> fieldMatches = StringUtils.getMatchedTokens( productName.toLowerCase(),
                        merchantWithFieldInDescriptionRegex.get( fieldName ) );
                    if ( fieldMatches != null & !fieldMatches.isEmpty() ) {
                        String fieldValue = fieldMatches.get( 0 );
                        productName = productName.replaceFirst( fieldValue, "" );
                        currentLineObj.setProductName( productName );
                        setFieldValue( currentLineObj, fieldValue, fieldName );
                        LOG.debug( "{} value for {} extracted from productName", fieldValue, fieldName );
                    }
                }
            }

            if ( isMerchantsWithMultilineDescription && isMultilineDescriptionAbove && !isMultilineDescriptionBelow
                && currentLineObj.getProductName() != null
                && ( currentLineObj.getPrices().size() > 1 || currentLineObj.getFinalPrice() != null )
                || consecutiveEmptyLines > 2 )
                previousLineObj = null;

            // Handling continuous invalid lineItems to avoid false positives
            if ( !currentLineObj.isLineObjectValid() && previousLineObj != null && !previousLineObj.isLineObjectValid()
                && !currentLineObj.equals( previousLineObj ) && !isMerchantsWithExtraInfoInPreviousLine
                && !isMerchantsWithExtraInfoInNextLine && !isMerchantsWithMultilineDescription
                && !isMerchantWithMultilineLineItem ) {
                previousLineObj = null;
            }


            isPreviousLineEmpty = lines[lineNo].replaceAll( "\\s+", "" ).isEmpty();

        }
        if ( null == metaData.getLinesHavingMatchedSyntax() || metaData.getLinesHavingMatchedSyntax().isEmpty() ) {
            metaData.setLinesHavingMatchedSyntax( perfectMatchedSyntaxLines );
        }

        if ( Boolean.TRUE.equals( fieldConfiguration != null && fieldConfiguration.isText() ) ) {
            lineObjects.forEach( l -> l.setRawText(
                l.getRawText().replaceAll( "\n", "@@@" ).replaceAll( "\\s{2,}", " " ).replaceAll( "@@@", "\n" ) ) );
        }

        if ( firstLineItemLine != -1 ) {
            lineObjects = setIfQuantityUnitPresentInHeader( lineObjects, lines, firstLineItemLine );
        }

        lineObjects = lineObjects.stream()
            .filter( l -> l.getProductName() != null && ( l.getFinalPrice() != null || l.getProductId() != null ) )
            .collect( Collectors.toList() );
        return lineObjects;
    }


    private static List<LineItem> setIfQuantityUnitPresentInHeader( List<LineItem> lineObjects, String[] lines,
        int firstLineItemLine )
    {
        String quantityHeader = "";
        int firstLineItemLineNumber = firstLineItemLine;
        List<String> uomMatched;
        if ( !lineObjects.isEmpty() && lineObjects.get( 0 ).getLineNumber() > 0 && firstLineItemLineNumber > 0 ) {
            if ( !lines[firstLineItemLineNumber - 1].trim().isEmpty() ) {
                quantityHeader = lines[firstLineItemLineNumber - 1];
            } else {
                quantityHeader = ( firstLineItemLineNumber - 2 >= 0 ) ? lines[firstLineItemLineNumber - 2] : "";
            }
            uomMatched = StringUtils.getMatchedTokens( quantityHeader, "\\b(?i)" + UomValidator.getUomRegex() + "\\b" );
            //check 2 lines above
            if ( uomMatched.isEmpty() ) {
                quantityHeader = ( firstLineItemLineNumber - 2 >= 0 ) ? lines[firstLineItemLineNumber - 2] : "";
                uomMatched = StringUtils.getMatchedTokens( quantityHeader, "\\b(?i)" + UomValidator.getUomRegex() + "\\b" );
            }
            boolean headerContainsDigits = quantityHeader.chars().anyMatch( Character::isDigit );
            if ( !uomMatched.isEmpty() && !headerContainsDigits ) {
                String uom = uomMatched.get( 0 );
                lineObjects.stream().map( l -> (ReceiptLineItem) l )
                    .filter( l -> l != null && ( l.getQuantityUnit() == null
                        || ( l.getQuantityUnit() != null && l.getQuantityUnit().equals( "ea" ) ) ) )
                    .forEach( l -> l.setQuantityUnit( uom ) );
            }

        }

        return lineObjects;
    }


    private void mergeCurrentAndPreviousGroup( ReceiptLineItem currentLineObj, ReceiptLineItem previousGroupObj,
        String fieldsToLookFor )
    {
        List<String> fields = Arrays.stream( fieldsToLookFor.split( ",," ) ).map( String::trim ).collect( Collectors.toList() );
        fields.stream().filter( field -> !field.endsWith( "_first" ) ).forEach( field -> {
            setFieldValue( previousGroupObj, getFieldValue( currentLineObj, field ), field );
            if ( field.equals( "AMOUNT" ) ) {
                previousGroupObj.setPrices( currentLineObj.getPrices() );
                lineItemExtractorUtils.updateReceiptLIPrices( currentLineObj );
            }
        } );
    }


    private static boolean firstGroupValidation( ReceiptLineItem currentLineObj, String fieldsToLookFor )
    {
        List<String> fields = Arrays.stream( fieldsToLookFor.split( ",," ) ).map( String::trim ).collect( Collectors.toList() );

        if ( fields.stream().anyMatch( field -> field.endsWith( "_first" ) ) ) {
            return fields.stream().filter( field -> field.endsWith( "_first" ) )
                .map( field -> field.substring( 0, field.indexOf( "_first" ) ) ).allMatch( field -> {
                    return getFieldValue( currentLineObj, field ) != null;
                } );
        }
        return false;
    }


    private String getFieldsForMultilineLineitems( FieldExtractionRequest helper, DocumentMetaData metaData,
        Map<String, Object> configuration )
    {
        List<String> merchantNames = configService.getValueList( "lineitems_merchants_with_multiline_lineitem", configuration );
        return merchantNames.stream().filter( name -> name.startsWith( metaData.getMerchantName() ) ).map( name -> {
            if ( name.contains( "[" ) )
                return name.substring( name.indexOf( "[" ) + 1, name.indexOf( "]" ) ).trim();
            return "TEXT ,, ID ,, AMOUNT";
        } ).findFirst().orElse( "TEXT ,, ID ,, AMOUNT" );
    }


    private static boolean validateCurrentLineItemForFields( ReceiptLineItem currentLineObj, String fieldsToLookFor )
    {
        LOG.info( "validating Line {}", currentLineObj );

        if ( currentLineObj == null )
            return false;
        //TODO make fields to validate configurable via properties
        List<String> fields = Arrays.stream( fieldsToLookFor.split( ",," ) ).map( String::trim ).map( field -> {
            if ( field.endsWith( "_first" ) )
                return field.substring( 0, field.indexOf( "_first" ) );
            return field;
        } ).collect( Collectors.toList() );

        return fields.stream().allMatch( field -> {
            if ( field.equalsIgnoreCase( "TEXT" ) ) {
                Object obj = getFieldValue( currentLineObj, field );
                if ( obj != null ) {
                    String text = (String) obj;
                    return text.trim().isEmpty() ? false : true;
                }
                return false;
            }
            return getFieldValue( currentLineObj, field ) != null;
        } );
    }


    private static void mergeCurrentAndPreviousLines( ReceiptLineItem currentLineObj, LineItem previousLineObj )
    {
        LOG.debug( "merging {} and {}", currentLineObj, previousLineObj );
        if ( previousLineObj == null )
            return;
        if ( currentLineObj.getProductId() == null )
            currentLineObj.setProductId( previousLineObj.getProductId() );
        if ( currentLineObj.getPrices().size() == 0 ) {
            currentLineObj.setPrices( previousLineObj.getPrices() );
            currentLineObj.setFinalPrice( previousLineObj.getFinalPrice() );
        }
        currentLineObj.setProductName( ( previousLineObj.getProductName() == null ? "" : previousLineObj.getProductName() )
            + ( currentLineObj.getProductName() == null ? "" : currentLineObj.getProductName() ) );
        LOG.trace( "merged output {}", currentLineObj );
    }


    private boolean checkForMerchantsWithMultilineLineItem( DocumentMetaData metaData, FieldExtractionRequest helper,
        Map<String, Object> configuration )
    {
        List<String> merchantNames = configService.getValueList( "lineitems_merchants_with_multiline_lineitem", configuration );
        merchantNames = merchantNames.stream().filter( name -> name.startsWith( metaData.getMerchantName() ) ).map( name -> {
            if ( name.contains( "[" ) )
                return name.substring( 0, name.indexOf( "[" ) ).trim();
            return name;
        } ).collect( Collectors.toList() );
        for ( String merchantName : merchantNames ) {
            if ( StringUtils.checkIfStringContainsRegexPattern( metaData.getMerchantName(),
                Constants.WORD_START_REGEX + merchantName + Constants.WORD_END_REGEX ) ) {
                LOG.info( "{} is a merchant with lineitem spread over multiple Line", merchantName );
                return true;
            }
        }
        return false;
    }


    private static void setFieldValue( ReceiptLineItem lineObject, Object fieldValue, String text )
    {
        if ( lineObject == null || fieldValue == null )
            return;
        switch ( text ) {
            case "ID":
                lineObject.setProductId( (String) fieldValue );
                break;
            case "AMOUNT":
                lineObject.setFinalPrice( (Float) fieldValue );
                break;
            case "QUANTITY":
                lineObject.setQuantity( (Float) fieldValue );
                break;
            default:
                return;
        }
    }


    private static Object getFieldValue( ReceiptLineItem lineObject, String text )
    {
        if ( text == null || lineObject == null )
            return null;

        switch ( text ) {
            case "TEXT":
                return lineObject.getProductName();
            case "ID":
                return lineObject.getProductId();
            case "AMOUNT":
                return lineObject.getFinalPrice();
            case "QUANTITY":
                return lineObject.getQuantity();
            default:
                return null;
        }
    }


    public Tuple2<Boolean, Boolean> checkMultilineDescriptionAlignment( String[] lines, String[] syntaxOrder,
        DocumentMetaData metaData, List<Integer> perfectMatchedSyntaxLines, FieldExtractionRequest extractionData )
    {

        Tuple2<Boolean, Boolean> alignment = new Tuple2<>( false, false );
        LineItem line = new LineItem(), previousLine;
        LineItemHelper helper;
        List<LineItem> validLines = new ArrayList<>(), extractedLines = new ArrayList<>();
        int cursor = 0,
            docWidth = Arrays.stream( lines ).reduce( ( a, b ) -> a.length() > b.length() ? a : b ).orElse( "" ).length();
        do {
            previousLine = line;
            line = new LineItem();
            line.setLineNumber( cursor );
            line.setRawText( lines[cursor] );

            helper = new LineItemHelper( metaData, extractionData );
            helper.setLineValidator( new LineValidator( lines[cursor].replaceAll( "\\s{2,}", " " ) + " ", null, docWidth ) );

            lineItemExtractorUtils.setLineObjectsFromSyntax( line, helper, syntaxOrder, metaData.getDomain(),
                perfectMatchedSyntaxLines );

            cursor++;
            if ( line.isLineObjectValid()
                || ( line.getProductName() != null && ( line.getPrices().size() > 1
                    || previousLine.getPrices().size() > 1 && previousLine.getProductName() == null ) )
                || ( ( line.getProductId() != null || previousLine.getProductName() != null )
                    && line.getPrices().size() > 1 ) ) {
                // check for product descriptions occurring above first valid lineItem
                if ( validLines.size() == 0 && previousLine.getProductName() != null && previousLine.getPrices().size() == 0 )
                    alignment.f2( true );
                validLines.add( line );
            }
            extractedLines.add( line );
        } while ( cursor < lines.length && validLines.size() < 2 );

        // check for product descriptions occurring after valid lineItem
        if ( validLines.size() > 1 ) {
            LineItem nextLine = extractedLines.stream().filter( l -> l.getLineNumber() > validLines.get( 0 ).getLineNumber() )
                .findFirst().orElse( null );
            AtomicBoolean hasEmptyLine = new AtomicBoolean( false );
            IntStream
                .range( validLines.get( 0 ).getLineNumber(),
                    validLines.size() > 1 ? validLines.get( 1 ).getLineNumber() : lines.length )
                .filter( i -> lines[i].isEmpty() ).findFirst().ifPresent( l -> hasEmptyLine.set( true ) );
            if ( hasEmptyLine.get() && nextLine != null
                && ( nextLine.getLineNumber() - validLines.get( 0 ).getLineNumber() ) < 2 && nextLine.getProductName() != null
                && nextLine.getPrices().size() == 0 ) {
                alignment.f1( true );
            }
        }

        if ( !alignment.f1() && !alignment.f2() )
            alignment.f1( true );
        return alignment;
    }


    public boolean checkForMultilineProductDescription( ReceiptLineItem currentLine, ReceiptLineItem previousLine,
        boolean isMultilineDescriptionBelow, DocumentMetaData metaData, Map<String, Object> configuration )
    {

        try {
            if ( isMultilineDescriptionBelow && previousLine != null
                && ( previousLine.getPrices().size() > 1 || previousLine.getFinalPrice() != null ) ) {
                if ( currentLine.getProductName() != null ) {
                    if ( previousLine.getProductName() == null )
                        previousLine.setProductName( currentLine.getProductName() );
                    else {
                        List<String> extraFieldsToPickInNextLine = utils
                            .getExtraFieldsToPickInNextLine( metaData.getMerchantName(), configuration );
                        //append product name only when specified for merchant name in the list or no value is available in the list.
                        if ( extraFieldsToPickInNextLine == null || extraFieldsToPickInNextLine.isEmpty()
                            || extraFieldsToPickInNextLine.contains( "TEXT" ) ) {
                            previousLine.setProductName( previousLine.getProductName() + " " + currentLine.getProductName() );
                        }
                    }
                    if ( currentLine.getProductId() != null && previousLine.getProductId() == null ) {
                        previousLine.setProductId( currentLine.getProductId() );
                    }
                    return true;
                }
            } else if ( !isMultilineDescriptionBelow && previousLine != null && previousLine.getProductName() != null ) {
                currentLine.setProductName( previousLine.getProductName()
                    + ( currentLine.getProductName() == null ? "" : " " + currentLine.getProductName() ) );
                return true;
            } else if ( previousLine != null && previousLine.getFinalPrice() == null && !previousLine.getPrices().isEmpty() ) {
                if ( currentLine.getPrices().isEmpty() && currentLine.getProductName() != null
                    && !currentLine.getProductName().isEmpty() ) {
                    currentLine.setFinalPrice( currentLine.getPrices().get( currentLine.getPrices().size() - 1 ) );
                }
            }
        } catch ( Exception err ) {
            LOG.error( "error occurred while extracting multiline desc : {}", err );
        }
        return false;
    }


    private static void setIfPriceOrIdPresent( LineItem currentLineObj, List<LineItem> lineObjects, String merchantname )
    {
        if ( ( ( currentLineObj.getProductId() != null && !currentLineObj.getProductId().isEmpty() )
            || ( !currentLineObj.getPrices().isEmpty() ) ) && currentLineObj.getProductName() != null
            && !currentLineObj.getProductName().isEmpty() ) {
            if ( !lineObjects.isEmpty()
                || ( merchantname.equalsIgnoreCase( "walmart" ) && !currentLineObj.getProductId().isEmpty() ) ) {
                LOG.debug( "Adding line item as price or id is present" );
                lineObjects.add( currentLineObj );
            }
        }
    }


    /**
     * Calculates and sets the value of line confidence for a line item
     * @param currentLineObj Line item object
     * @param confidenceCalculator
     */
    private void setLineConfidence( LineItem currentLineObj, ConfidenceCalculator confidenceCalculator )
    {
        setAverageOfFieldConfidences( currentLineObj );
        currentLineObj.setLineConfidence( ( ConfidenceCalculator.LINE_SYNTAX_CONFIDENCE_WEIGHTAGE
            * confidenceCalculator.getSyntaxConfidence() )
            + ( ConfidenceCalculator.LINE_AVG_FIELDS_CONFIDENCE_WEIGHTAGE * currentLineObj.getAverageOfFieldConfidence() )
            + ( ConfidenceCalculator.LINE_SYNTAX_MATCH_CONFIDENCE_WEIGHTAGE * currentLineObj.getLineSyntaxMatchConfidence() ) );

        LOG.trace( "Line confidence: {}", currentLineObj.getLineConfidence() );
    }


    public void setAverageOfFieldConfidences( LineItem currentLineObj )
    {
        List<Float> fieldConfidences = currentLineObj.getFieldConfidences();
        LOG.trace( "Field confidences got: {}", fieldConfidences );
        currentLineObj.setAverageOfFieldConfidence( utils.getAverageFromListValues( fieldConfidences ) );
        LOG.trace( "Average fields confidence: {}", currentLineObj.getAverageOfFieldConfidence() );
    }


    private Tuple2<String, String> getShortenedTextForReceipts( String extractedText, DocumentMetaData metaData,
        FieldExtractionRequest helper, Map<String, Object> configuration )
    {
        String shortenedText = extractedText;
        String rawText = helper.getOcrData().getRawText();
        String rawShortenedText = helper.getOcrData().getRawText();
        String startAndEndPoints = "";
        // Get merchant specific start and end points
        if ( null != metaData.getMerchantName() && !metaData.getMerchantName().isEmpty() ) {
            String merchantName = StringUtils.getKeyFromMapByMatchingRegex(
                configService.getExtractionConfigurationMap( "startAndEndKeyWords", configuration ),
                metaData.getMerchantName() );
            if ( merchantName != null && !merchantName.isEmpty() ) {
                startAndEndPoints = configService.getExtractionConfigurationMap( "startAndEndKeyWords", configuration )
                    .get( merchantName ).toString();
            }
        }

        // Shorten text based on start and end point regexes configured for the identified merchant name
        if ( null != startAndEndPoints && !startAndEndPoints.isEmpty() ) {
            String[] startAndEndRegexes = startAndEndPoints.split( "\\|" );
            int matchedIndex;
            if ( startAndEndRegexes.length != 0 && !startAndEndRegexes[0].isEmpty() ) {
                String[] startRegexes = startAndEndRegexes[0].split( ",," );
                LOG.debug( "Start Regexes: {}", Arrays.asList( startRegexes ) );
                matchedIndex = getMatchedWordIndex( extractedText.toLowerCase(), Arrays.asList( startRegexes ), false );
                if ( matchedIndex != -1 ) {
                    shortenedText = extractedText.substring( matchedIndex );
                    String[] shortenedTextLines = shortenedText.split( Constants.LINE_SEPARATOR_REGEX );
                    String[] rawTextLines = rawText.split( Constants.LINE_SEPARATOR_REGEX );
                    if ( shortenedTextLines.length != rawTextLines.length ) {
                        StringBuilder rawShortenedTextBuilder = new StringBuilder();
                        //start from the actual line where shortened text starts.
                        for ( int loopCounter = rawTextLines.length
                            - shortenedTextLines.length; loopCounter < rawTextLines.length; loopCounter++ ) {
                            rawShortenedTextBuilder.append( rawTextLines[loopCounter] );
                            if ( loopCounter != rawTextLines.length - 1 ) {
                                rawShortenedTextBuilder.append( "\r\n" );
                            }
                        }
                        rawShortenedText = rawShortenedTextBuilder.toString();
                    }
                }
            }

            if ( startAndEndRegexes.length > 1 ) {
                String[] endRegexes = startAndEndRegexes[1].split( ",," );
                LOG.debug( "End Regexes: {}", Arrays.asList( endRegexes ) );
                matchedIndex = getMatchedWordIndex( shortenedText, Arrays.asList( endRegexes ), true );

                if ( matchedIndex != -1 ) {
                    String matchedWord = StringUtils.getMatchedTokens( shortenedText.substring( matchedIndex ), "\\w+" )
                        .get( 0 );
                    shortenedText = shortenedText.substring( 0, matchedIndex );
                    String[] shortenedTextLines = shortenedText.split( Constants.LINE_SEPARATOR_REGEX );
                    String[] rawShortenedTextLines = rawShortenedText.split( Constants.LINE_SEPARATOR_REGEX );
                    StringBuilder rawShortenedTextBuilder = new StringBuilder();
                    for ( int loopCounter = 0; loopCounter < shortenedTextLines.length; loopCounter++ ) {
                        if ( loopCounter != shortenedTextLines.length - 1 ) {
                            rawShortenedTextBuilder.append( rawShortenedTextLines[loopCounter] );
                            rawShortenedTextBuilder.append( "\r\n" );
                        } else {
                            int endIndex = rawShortenedTextLines[loopCounter].toLowerCase()
                                .indexOf( matchedWord.toLowerCase() );
                            rawShortenedTextBuilder.append( rawShortenedTextLines[loopCounter], 0,
                                ( endIndex != -1 ) ? endIndex : rawShortenedTextLines[loopCounter].length() );
                        }
                    }
                    rawShortenedText = rawShortenedTextBuilder.toString();
                }
            }
        } else {
            // If no configuration found, try to look for default end points of receipts.
            List<String> endPoints = configService.getValueList( Constants.END_KEYWORDS, configuration );
            LOG.debug( "End point words according to domain: {}", endPoints );
            int index = getMatchedWordIndex( extractedText.toLowerCase(), endPoints, true );
            if ( index != -1 ) {
                String matchedWord = StringUtils.getMatchedTokens( shortenedText.substring( index ), "\\w+" ).get( 0 );
                shortenedText = extractedText.substring( 0, index );
                shortenedText = shortenedText.substring( 0, shortenedText.lastIndexOf( "\r\n" ) );
                String[] shortenedTextLines = shortenedText.split( Constants.LINE_SEPARATOR_REGEX );
                String[] rawTextLines = rawText.split( Constants.LINE_SEPARATOR_REGEX );
                StringBuilder rawShortenedTextBuilder = new StringBuilder();
                for ( int loopCounter = 0; loopCounter < shortenedTextLines.length; loopCounter++ ) {
                    if ( loopCounter != shortenedTextLines.length - 1 ) {
                        rawShortenedTextBuilder.append( rawTextLines[loopCounter] );
                        rawShortenedTextBuilder.append( "\r\n" );
                    } else {
                        int endIndex = rawTextLines[loopCounter].toLowerCase().indexOf( matchedWord.toLowerCase() );
                        rawShortenedTextBuilder.append( rawTextLines[loopCounter], 0,
                            ( endIndex != -1 ) ? endIndex : rawTextLines[loopCounter].length() );
                    }
                }
                rawShortenedText = rawShortenedTextBuilder.toString();
                rawShortenedText = rawShortenedText.substring( 0, rawShortenedText.lastIndexOf( "\r\n" ) );
            }
        }

        shortenedText = removeBasketDiscountLines( shortenedText, helper, configuration );
        rawShortenedText = removeBasketDiscountLines( rawShortenedText, helper, configuration );

        //This text could be the original text if no start or end points were found in the text.
        return new Tuple2<>( shortenedText, rawShortenedText );
    }


    private String removeBasketDiscountLines( String shortenedText, FieldExtractionRequest helper,
        Map<String, Object> configuration )
    {
        List<String> endPoints = configService.getValueList( Constants.END_KEYWORDS, configuration );
        LOG.debug( "End point words according to domain: {}", endPoints );
        int index = getMatchedWordIndex( shortenedText.toLowerCase(), endPoints, false );
        if ( index != -1
            && checkIfTextHasOnlyBasketDiscountBetweenSubtotals( shortenedText.substring( index ).toLowerCase() ) ) {
            shortenedText = shortenedText.substring( 0, index );
        }
        return shortenedText;
    }


    private boolean checkIfTextHasOnlyBasketDiscountBetweenSubtotals( String shortenedText )
    {
        boolean isTextHasOnlyBasketDiscountBetweenSubtotals = true;
        String[] subStrLines = shortenedText.split( Constants.LINE_SEPARATOR_REGEX );
        if ( subStrLines.length > 1 ) {
            for ( int subStrLineNo = 1; subStrLineNo < subStrLines.length; subStrLineNo++ ) {
                if ( !subStrLines[subStrLineNo].trim().isEmpty() && !subStrLines[subStrLineNo].contains( "coupon" ) ) {
                    isTextHasOnlyBasketDiscountBetweenSubtotals = false;
                    break;
                }
            }
        }
        return isTextHasOnlyBasketDiscountBetweenSubtotals;
    }


    private int getMatchedWordIndex( String extractedText, List<String> regexes, boolean isLastIndex )
    {
        int index = -1;
        for ( String regex : regexes ) {
            LOG.trace( "Matched regex : {} and isLastIndex match is : {}", regex, isLastIndex );
            if ( isLastIndex ) {
                List<Integer> matchedStartWords = StringUtils.getMatchedTokensWithIndex( extractedText.toLowerCase(),
                    "\\b" + regex + "\\b" );
                if ( !matchedStartWords.isEmpty() ) {
                    index = matchedStartWords.get( matchedStartWords.size() - 1 );
                }
            } else {
                List<String> matchedStartWords = StringUtils.getMatchedTokens( extractedText.toLowerCase(),
                    "\\b" + regex + "\\b" );
                if ( !matchedStartWords.isEmpty() ) {
                    index = extractedText.toLowerCase().indexOf( matchedStartWords.get( matchedStartWords.size() - 1 ) );
                }
            }
            if ( index != -1 ) {
                break;
            }
        }
        return index;
    }


    private boolean checkForMerchantsWithExtraInfoInNextLine( DocumentMetaData metaData, FieldExtractionRequest helper,
        Map<String, Object> configuration )
    {
        List<String> merchantList = configService.getValueList( "lineitems_merchants_with_extra_info_in_next_line",
            configuration );
        for ( String listItem : merchantList ) {
            String merchantName = listItem.substring( 0,
                ( listItem.contains( ":" ) ) ? listItem.indexOf( ":" ) : listItem.length() );
            if ( StringUtils.checkIfStringContainsRegexPattern( metaData.getMerchantName(),
                Constants.WORD_START_REGEX + merchantName + Constants.WORD_END_REGEX ) ) {
                LOG.debug( "{} is a merchant with extra info in next line", merchantName );
                return true;
            }
        }
        return false;
    }


    public boolean checkForMerchantsWithMultilineProductDescription( DocumentMetaData metaData, FieldExtractionRequest helper,
        Map<String, Object> configuration )
    {
        List<String> merchantNames = configService.getValueList( "lineitems_merchants_with_multiline_product_description",
            configuration );
        for ( String merchantName : merchantNames ) {
            if ( metaData.getMerchantName() != null && StringUtils.checkIfStringContainsRegexPattern(
                metaData.getMerchantName(), Constants.WORD_START_REGEX + merchantName + Constants.WORD_END_REGEX ) ) {
                LOG.debug( "{} is a merchant with multiline description", merchantName );
                return true;
            }
        }
        return false;
    }


    private Map<String, String> getRegexForMerchantWithFieldInDescription( DocumentMetaData metaData,
        FieldExtractionRequest helper, Map<String, Object> configuration )
    {
        Map<String, Object> config = (Map<String, Object>) configuration.get( LineItemConstants.CONFIGURATION );
        Map<String, Object> merchantNameFieldRegexMap = (Map<String, Object>) config
            .get( "lineitems_merchant_with_field_in_description" );
        for ( String merchantName : merchantNameFieldRegexMap.keySet() ) {
            if ( StringUtils.checkIfStringContainsRegexPattern( metaData.getMerchantName(),
                Constants.WORD_START_REGEX + merchantName + Constants.WORD_END_REGEX ) ) {
                LOG.debug( "{} is a merchant with field description", merchantName );
                HashMap<String, String> fieldAndRegexMap = new HashMap<>();
                fieldAndRegexMap.put(
                    ( (String) merchantNameFieldRegexMap.get( merchantName ) ).substring( 0,
                        ( (String) merchantNameFieldRegexMap.get( merchantName ) ).indexOf( "[" ) ),
                    ( (String) merchantNameFieldRegexMap.get( merchantName ) ).substring(
                        ( (String) merchantNameFieldRegexMap.get( merchantName ) ).indexOf( "[" ) + 1,
                        ( (String) merchantNameFieldRegexMap.get( merchantName ) ).lastIndexOf( "]" ) ) );
                return fieldAndRegexMap;
            }
        }
        return null;
    }


    private ReceiptLineItem checkForPreviousLineValues( ReceiptLineItem previousItem, ReceiptLineItem currentItem,
        boolean isMerchantsWithExtraInfoInPreviousLine, DocumentMetaData metaData, FieldExtractionRequest extractionData,
        boolean isPreviousLineEmpty, Map<String, Object> configuration )
    {
        if ( previousItem != null && previousItem.getRawText() != null && !previousItem.getRawText().isEmpty() ) {
            if ( isMerchantsWithExtraInfoInPreviousLine ) {
                if ( previousItem.getUnitPrice() != null ) {
                    currentItem.setUnitPrice( previousItem.getUnitPrice() );
                }
                if ( ( previousItem.getQuantity() != 0 && currentItem.getQuantity() == 0 ) || ( previousItem.getQuantity() != 0
                    && previousItem.getQuantityUnit() != null && currentItem.getQuantityUnit() == null ) ) {
                    currentItem.setQuantity( previousItem.getQuantity() );
                }
                if ( previousItem.getQuantityUnit() != null ) {
                    currentItem.setQuantityUnit( previousItem.getQuantityUnit() );
                }
                if ( previousItem.getProductId() != null ) {
                    currentItem.setProductId( previousItem.getProductId() );
                }
                previousItem = null;
            } else if ( ( currentItem.getLineNumber() - previousItem.getLineNumber() < 2
                || ( currentItem.getLineNumber() - previousItem.getLineNumber() <= 2 && isPreviousLineEmpty ) ) ) {

                String rawText = currentItem.getRawText();
                if ( previousItem.getPrices().isEmpty() ) {

                    if ( !previousItem.getProductName().isEmpty()
                        && ProductNameValidator.isValidProductName( previousItem.getProductName() )
                        && !lineItemExtractorUtils.previousLineProductNameContainsBannedWord(
                            previousItem.getProductName().toLowerCase(), configuration )
                        && ( currentItem.getProductName() == null || currentItem.getProductName().split( "\\s+" ).length < 3 )
                        && previousItem.getRawText() != null ) {

                        List<String> productNames = Arrays.asList( previousItem.getRawText().split( "  " ) );
                        AtomicReference<String> productNameWithMaxLen = new AtomicReference<>( productNames.get( 0 ) );
                        productNames.stream().filter( s -> s.length() > productNameWithMaxLen.get().length() )
                            .forEach( productNameWithMaxLen::set );
                        if ( productNameWithMaxLen.get().contains( previousItem.getProductName() ) )
                            productNameWithMaxLen.set( previousItem.getProductName() );

                        currentItem.setProductName( productNameWithMaxLen.get() + " "
                            + ( currentItem.getProductName() == null ? "" : currentItem.getProductName() ) );
                        lineItemExtractorUtils.filterProductName( currentItem, metaData, extractionData );
                        rawText = previousItem.getRawText() + "\n" + currentItem.getRawText();
                    }
                }
                currentItem.setRawText( rawText );
            }
        }
        return previousItem;
    }


    @Override
    public void afterPropertiesSet() throws Exception
    {
       LOG.info(";;;");
    }
}
