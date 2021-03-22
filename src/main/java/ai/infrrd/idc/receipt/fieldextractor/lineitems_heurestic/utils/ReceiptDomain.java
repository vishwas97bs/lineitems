package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor.ReceiptLineItemExtractor;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor.SyntaxExtractor;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.ConfigService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Component
public class ReceiptDomain extends Domain
{
    // Creating a singleton object since the values in this class will not change for a new object created
    private static ReceiptDomain receiptDomain;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( ReceiptDomain.class );

    private static final String QUANTITY_UNIT_LIST[] = { "stk", "st", "lb", "ib", "1b", "ml", "kg", "g", "l", "bt" };
    private static final List<String> UNIT_PRICE_UNITS = Arrays.asList( "eur/kg", "eur /kg", "euro/st" );

    private ConfigService configService;

    private LineItemExtractorUtils lineItemExtractorUtils;

    private ReceiptLineItemExtractor receiptLineItemExtractor;

    private Utils utils;

    @Autowired
    public void setUtils( Utils utils )
    {
        this.utils = utils;
    }


    @Autowired
    public void setConfigService( ConfigService configService )
    {
        this.configService = configService;
    }


    @Autowired
    public void setLineItemExtractorUtils( LineItemExtractorUtils lineItemExtractorUtils )
    {
        this.lineItemExtractorUtils = lineItemExtractorUtils;
    }


    @Autowired
    public void setReceiptLineItemExtractor( ReceiptLineItemExtractor receiptLineItemExtractor )
    {
        this.receiptLineItemExtractor = receiptLineItemExtractor;
    }


    public ReceiptDomain()
    {
        // TODO: Fill these with correct Strings
        this.defaultFields = new Constants.LINE_ITEM_FIELDS[] { Constants.LINE_ITEM_FIELDS.TEXT,
            Constants.LINE_ITEM_FIELDS.AMOUNT, Constants.LINE_ITEM_FIELDS.ID, Constants.LINE_ITEM_FIELDS.QUANTITY };
        this.defaultSyntaxesInOrder = new String[] {
            Constants.LINE_ITEM_FIELDS.ID + SyntaxExtractor.SYNTAX_FIELD_DELIMITER + Constants.LINE_ITEM_FIELDS.TEXT
                + SyntaxExtractor.SYNTAX_FIELD_DELIMITER + Constants.LINE_ITEM_FIELDS.AMOUNT,
            Constants.LINE_ITEM_FIELDS.TEXT + SyntaxExtractor.SYNTAX_FIELD_DELIMITER + Constants.LINE_ITEM_FIELDS.ID
                + SyntaxExtractor.SYNTAX_FIELD_DELIMITER + Constants.LINE_ITEM_FIELDS.AMOUNT,
            Constants.LINE_ITEM_FIELDS.TEXT + SyntaxExtractor.SYNTAX_FIELD_DELIMITER + Constants.LINE_ITEM_FIELDS.AMOUNT };
        this.name = Constants.DOMAIN_NAME.receipt;
    }


//    @Override
    public void performNextLineOperations( DocumentMetaData metaData, LineItem currentLine, LineItem previousLine,
        List<LineItem> lineObjects, LineItemHelper lineItemHelper, Map<String, Object> configuration,
        FieldConfiguration fieldConfiguration )
    {
        LOG.trace( "Method: performNextLineOperations called." );
        ReceiptLineItem receiptCurrentLine = (ReceiptLineItem) currentLine;
        ReceiptLineItem receiptPreviousLine = (ReceiptLineItem) previousLine;
        try {
            List<String> extraFieldsInNextLine = utils.getExtraFieldsToPickInNextLine( metaData.getMerchantName(),
                configuration );
            if ( extraFieldsInNextLine != null && !extraFieldsInNextLine.isEmpty() ) {
                //perform specific header operations.
                for ( String header : extraFieldsInNextLine ) {
                    LOG.debug( "Performing next line operation for header {}", header );
                    switch ( header ) {
                        case "ID":
                            previousLine.setProductId( ( previousLine.getProductId() == null ) ? currentLine.getProductId()
                                : currentLine.getProductId() + " " + previousLine.getProductId() );
                            break;
                        case "AMOUNT":
                            previousLine.setFinalPrice( ( previousLine.getFinalPrice() == null ) ? currentLine.getFinalPrice()
                                : currentLine.getFinalPrice() + previousLine.getFinalPrice() );
                            break;
                        case "TEXT":
                            previousLine
                                .setProductName( ( previousLine.getProductName() == null ) ? currentLine.getProductName()
                                    : ( currentLine.getProductName() != null )
                                        ? currentLine.getProductName() + " " + previousLine.getProductName()
                                        : previousLine.getProductName() );
                            break;
                    }
                }
                currentLine = previousLine;
            } else {
                try {
                    lineItemExtractorUtils.updateReceiptLIPrices( receiptCurrentLine );
                    //If previous line has name and other details and not price and the current line had price and not name  then set the price in the current line to the previous line item
                    if ( null != receiptCurrentLine.getFinalPrice() && receiptCurrentLine.getFinalPrice() != 0f
                        && !receiptPreviousLine.isAccurate() && receiptPreviousLine.getPrices().isEmpty() ) {

                        LOG.debug( "Adding extra fields from next line" );
                        receiptPreviousLine.addToPriceList( String.valueOf( receiptCurrentLine.getFinalPrice() ) );
                        receiptPreviousLine.setFinalPrice();
                        receiptPreviousLine.setAccurate( true );
                        //This is because we would have already added the previous line item even if only ID is present and not price
                        if ( null != previousLine.getProductId() && !previousLine.getProductId().isEmpty()
                            && !lineObjects.isEmpty() )
                            lineObjects.remove( lineObjects.size() - 1 );
                        if ( checkForMerchantsWithExtraInfoInNextLine( metaData, configuration ) ) {
                            if ( receiptCurrentLine.getProductId() != null ) {
                                receiptPreviousLine.setProductId( receiptCurrentLine.getProductId() );
                            } else {
                                String[] fieldsToMatch = new String[] { "ID", "QUANTITY" };
                                LineItem newLineItem = new ReceiptLineItem();
                                LineItemHelper newLineItemHelper = new LineItemHelper();

                                newLineItemHelper.setExtractionHelper( lineItemHelper.getExtractionHelper() );
                                newLineItemHelper.setLineValidator( new LineValidator( currentLine.getRawText() ) );
                                newLineItemHelper.setMetaData( lineItemHelper.getMetaData() );

                                lineItemExtractorUtils.setLineObjectsFromSyntax( newLineItem, newLineItemHelper, fieldsToMatch,
                                    receiptDomain, new ArrayList<>() );
                                ReceiptLineItem receiptLineItem = (ReceiptLineItem) newLineItem;
                                if ( receiptLineItem.getProductId() != null ) {
                                    ( (ReceiptLineItem) currentLine ).setQuantity( receiptLineItem.getQuantity() );
                                    currentLine.setProductId( receiptLineItem.getProductId() );
                                    receiptPreviousLine.setQuantity( receiptLineItem.getQuantity() );
                                    receiptPreviousLine.setProductId( receiptLineItem.getProductId() );
                                    receiptPreviousLine
                                        .setRawText( receiptPreviousLine.getRawText() + " " + receiptLineItem.getRawText() );

                                }
                                if ( ( (ReceiptLineItem) currentLine ).getUnitPrice() != null
                                    && ( (ReceiptLineItem) currentLine ).getUnitPrice() != 0 ) {
                                    receiptPreviousLine.setUnitPrice( ( (ReceiptLineItem) currentLine ).getUnitPrice() );
                                }
                            }
                        }
                        lineObjects.add( receiptPreviousLine );
                        recalculateLineConfidence( receiptCurrentLine, receiptPreviousLine );

                    } else if ( receiptPreviousLine.getProductName() != null && !receiptPreviousLine.getProductName().isEmpty()
                        && receiptPreviousLine.getProductId() != null && !receiptPreviousLine.getProductId().isEmpty()
                        && receiptCurrentLine.getProductName() != null && !receiptCurrentLine.getProductName().isEmpty() ) {
                        LOG.debug( "Adding previous Line item" );
                        lineObjects.add( receiptPreviousLine );
                    }
                } catch ( LineExtractionException e ) {
                    LOG.error( "Error wile adding price from next line", e );
                }

                if ( ( ( (ReceiptLineItem) previousLine ).getQuantity() == 0
                    && ( (ReceiptLineItem) previousLine ).getQuantityUnit() == null )
                    || ( null != ( (ReceiptLineItem) previousLine ).getQuantityUnit()
                        && ( ( (ReceiptLineItem) previousLine ).getQuantityUnit().equals( "ml" )
                            || ( (ReceiptLineItem) previousLine ).getQuantityUnit().equals( "g" ) ) ) ) {
                    checkForQuantity( metaData, lineItemHelper.getLineValidator().getLineStr(), receiptPreviousLine,
                        configuration, true, fieldConfiguration );

                    if ( null != receiptCurrentLine.getFinalPrice() && receiptCurrentLine.getFinalPrice() != 0f
                        && !receiptPreviousLine.isAccurate() && receiptPreviousLine.isCoupon() ) {
                        try {
                            receiptPreviousLine.addToPriceList( String.valueOf( receiptCurrentLine.getFinalPrice() ) );
                        } catch ( LineExtractionException e ) {
                            LOG.error( "LineExtractionException occurred", e );
                        }
                        receiptPreviousLine.setFinalPrice();
                        receiptPreviousLine.setAccurate( true );
                    }
                }
            }

        } catch ( Exception e ) {
            LOG.warn( "an exception occurred while performing next line operations.", e );
        }
        LOG.trace( "Method: performNextLineOperations finished." );
    }


    private void recalculateLineConfidence( ReceiptLineItem receiptCurrentLine, ReceiptLineItem receiptPreviousLine )
    {
        if ( null != receiptCurrentLine.getFieldConfidences().get( 0 ) ) {
            try {
                receiptPreviousLine.addToFieldConfidenceList( receiptCurrentLine.getFieldConfidences().get( 0 ) );
                receiptLineItemExtractor.setAverageOfFieldConfidences( receiptPreviousLine );
                receiptPreviousLine.setLineSyntaxMatchConfidence(
                    ( ( ( ( receiptPreviousLine.getLineSyntaxMatchConfidence() / ConfidenceCalculator.BASE_CONFIDENCE_VALUE )
                        * receiptPreviousLine.getNonOptionalFieldsCount() ) + 1 )
                        / receiptPreviousLine.getNonOptionalFieldsCount() ) * ConfidenceCalculator.BASE_CONFIDENCE_VALUE );
            } catch ( LineExtractionException e ) {
                LOG.error( "Error while recalculating line confidence for multiline lineItem", e );
            }
        }
    }


    //This is mainly used for walmart receipts
    public void checkForQuantity( DocumentMetaData metaData, String lineStr, ReceiptLineItem receiptPreviousLine,
        Map<String, Object> configuration, boolean isNextLine, FieldConfiguration fieldConfiguration )
    {
        String text = lineStr;
        lineStr = " " + lineStr.toLowerCase();
        LOG.debug( "Checking for quantity" );
        boolean alreadySet = false;
        List<String> quantityRegexes = configService.getValueList( Constants.QUANTITY_REGEX, configuration );
        for ( String quantityRegex : quantityRegexes ) {
            List<String> matchedQtyStr = StringUtils.getMatchedTokens( lineStr, quantityRegex );
            if ( !matchedQtyStr.isEmpty() ) {
                LOG.trace( "Matched Quantity Regex: {}, quantity: {}", quantityRegex, matchedQtyStr );
                String matchedQuantityUnit = null;
                for ( String quantityUnit : QUANTITY_UNIT_LIST ) {
                    if ( matchedQtyStr.get( 0 ).toLowerCase().contains( quantityUnit ) ) {
                        matchedQuantityUnit = quantityUnit;
                        break;
                    }
                }
                if ( matchedQuantityUnit == null
                    || ( matchedQuantityUnit.equals( "l" ) && matchedQtyStr.get( 0 ).contains( "al" ) ) ) {
                    matchedQuantityUnit = "ea";
                }
                if ( matchedQuantityUnit.equals( "ib" ) || matchedQuantityUnit.equals( "1b" )
                    || matchedQuantityUnit.equals( "16" ) ) {
                    matchedQuantityUnit = "lb";
                }
                if ( quantityRegex.equals( "\\d{1,3} at \\d{1,2} for \\d{1,2}[.]\\d{2}" ) && !matchedQtyStr.isEmpty() ) {
                    setCalculatedUnitPrice( matchedQtyStr.get( 0 ), receiptPreviousLine );
                    alreadySet = true;
                }

                setQuantity( matchedQtyStr.get( 0 ), receiptPreviousLine );

                boolean isLineContainsUnitPrice = UNIT_PRICE_UNITS.stream().anyMatch( lineStr::contains );
                if ( ( isNextLine || Arrays.asList( QUANTITY_UNIT_LIST ).contains( matchedQuantityUnit )
                    || matchedQuantityUnit.equals( "ea" ) || isLineContainsUnitPrice ) && !alreadySet
                    && receiptPreviousLine.getUnitPrice() == null ) {
                    receiptPreviousLine.setUnitPrice(
                        getPricePerUnit( metaData, StringUtils.replaceGivenStringWithSpaces( lineStr, matchedQtyStr.get( 0 ) ),
                            receiptPreviousLine, configuration ) );
                }
                receiptPreviousLine.setQuantityUnit( matchedQuantityUnit );
                break;
            }
        }
        if ( isNextLine && receiptPreviousLine.getQuantityUnit() == null ) {
            for ( String quantityUnit : QUANTITY_UNIT_LIST ) {
                if ( lineStr.contains( quantityUnit ) ) {
                    receiptPreviousLine.setQuantityUnit( quantityUnit );
                    String quantityText = setQuantity( lineStr, receiptPreviousLine );
                    if ( quantityText == null )
                        quantityText = Double.toString( receiptPreviousLine.getQuantity() );
                    receiptPreviousLine.setUnitPrice(
                        getPricePerUnit( metaData, StringUtils.replaceGivenStringWithSpaces( lineStr, quantityText ),
                            receiptPreviousLine, configuration ) );
                    break;
                }
            }
        }
        if ( isNextLine && Boolean.TRUE.equals( fieldConfiguration != null && fieldConfiguration.isText() )
            && receiptPreviousLine.getQuantityUnit() != null ) {
            String previousLineText = receiptPreviousLine.getRawText();
            text = previousLineText + "\n" + text;
            receiptPreviousLine.setRawText( text );
        }

    }


    private void setCalculatedUnitPrice( String qtyLine, ReceiptLineItem receiptPreviousLine )
    {
        qtyLine = qtyLine.substring( qtyLine.indexOf( "at" ) + 2 );
        String[] qtyEle = qtyLine.split( "for" );
        double qty = Double.parseDouble( qtyEle[1] ) / Double.parseDouble( qtyEle[0] );
        receiptPreviousLine.setUnitPrice( qty );
    }


    private String setQuantity( String line, ReceiptLineItem receiptPreviousLine )
    {
        List<String> matchedQuantity = StringUtils.getMatchedTokens( line, QuantityValidator.QUANTITY_UNIT_REGEX );
        if ( !matchedQuantity.isEmpty() ) {
            String quantity = removeAlpha( matchedQuantity.get( 0 ).trim() );

            //IC-1803, quantity can not be of length 5 or 6. This happens when productID is picked as quantity.
            if ( StringUtils.checkIfStringContainsRegexPattern( quantity, "\\d{5,6}" ) ) {
                receiptPreviousLine.setQuantity( Double.parseDouble( "1" ) );
            } else {
                if ( quantity.contains( " " ) && quantity.contains( "." ) ) {
                    quantity = quantity.replace( " ", "" );
                }
                if ( quantity.contains( " " ) ) {
                    quantity = quantity.replace( " ", "." );
                }
                if ( quantity.contains( "," ) ) {
                    quantity = quantity.replace( ",", "." );
                }
                try {
                    receiptPreviousLine.setQuantity( Double.parseDouble( quantity ) );
                    return removeAlpha( matchedQuantity.get( 0 ).trim() );
                } catch ( NumberFormatException e ) {
                    LOG.warn( "NumberFormatException while setting quantity :{} in line: {}", quantity, line );
                }
            }
        }
        return null;
    }


    private String removeAlpha( String line )
    {
        String out = "";
        if ( line != null ) {
            for ( int i = 0; i < line.length(); i++ ) {
                if ( !Character.isLetter( line.charAt( i ) ) ) {
                    out = out + line.charAt( i );
                }
            }
        }
        return out;
    }


    private Double getPricePerUnit( DocumentMetaData metaData, String line, ReceiptLineItem receiptPreviousLine,
        Map<String, Object> configuration )
    {
        Double value = null;
        String priceRegex = "(\\d{1,3}\\s?[,.]\\s?\\d{2,3})";
        List<String> matchedQtyStr = StringUtils.getMatchedTokens( line, priceRegex );
        if ( !matchedQtyStr.isEmpty() ) {
            if ( receiptPreviousLine.getPrices() != null && !receiptPreviousLine.getPrices().isEmpty() ) {
                String val = new PriceValidator().cleanupPrice( matchedQtyStr.get( 0 ) );
                if ( matchedQtyStr.size() == 1 && !checkForMerchantsWithExtraInfoInPreviousLine( metaData, configuration ) ) {
                    if ( Float.parseFloat( val ) != Math.abs( receiptPreviousLine.getFinalPrice() ) ) {
                        value = Double.parseDouble( val );
                    }
                } else {
                    value = Double.parseDouble( val );
                }
            } else {
                if ( matchedQtyStr.size() > 1 ) {
                    String val = new PriceValidator( ).cleanupPrice( matchedQtyStr.get( 0 ) );
                    value = Double.parseDouble( val );
                }
            }
        }
        return value;
    }


    public boolean checkForMerchantsWithExtraInfoInPreviousLine( DocumentMetaData metaData, Map<String, Object> configuration )
    {
        List<String> merchantNames = configService.getValueList( "lineitems_merchants_with_extra_info_in_previous_line",
            configuration );
        for ( String merchantName : merchantNames ) {
            if ( StringUtils.checkIfStringContainsRegexPattern( metaData.getMerchantName(),
                Constants.WORD_START_REGEX + merchantName + Constants.WORD_END_REGEX ) ) {
                LOG.debug( "{} is a merchant with extra info in previous line", merchantName );
                return true;
            }
        }
        return false;
    }


    private boolean checkForMerchantsWithExtraInfoInNextLine( DocumentMetaData metaData, Map<String, Object> configuration )
    {
        List<String> merchantNames = configService.getValueList( "lineitems_merchants_with_extra_info_in_next_line",
            configuration );
        for ( String merchantName : merchantNames ) {
            if ( StringUtils.checkIfStringContainsRegexPattern( metaData.getMerchantName(),
                Constants.WORD_START_REGEX + merchantName + Constants.WORD_END_REGEX ) ) {
                return true;
            }
        }
        return false;
    }
}
