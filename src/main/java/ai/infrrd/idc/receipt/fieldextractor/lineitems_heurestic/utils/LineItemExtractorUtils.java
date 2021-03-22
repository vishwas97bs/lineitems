package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor.SyntaxExtractor;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.ConfigService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.*;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class LineItemExtractorUtils
{
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( LineItemExtractorUtils.class );
    private static final int MINIMUM_LINE_DIFFERENCE = 2;

    private ConfigService configService;

    private SyntaxExtractor syntaxExtractor;

    private ProductNameValidator productNameValidator;

    private PriceValidator priceValidator;

    private IdValidator idValidator;

    private DateValidator dateValidator;

    private QuantityValidator quantityValidator;

    private TransactionTypeValidator transactionTypeValidator;

    private UomValidator uomValidator;

    private CurrencyValidator currencyValidator;

    @Autowired
    public void setConfigService( ConfigService configService )
    {
        this.configService = configService;
    }


    @Autowired
    public void setSyntaxExtractor( SyntaxExtractor syntaxExtractor )
    {
        this.syntaxExtractor = syntaxExtractor;
    }


    @Autowired
    public void setProductNameValidator( ProductNameValidator productNameValidator )
    {
        this.productNameValidator = productNameValidator;
    }


    @Autowired
    public void setPriceValidator( PriceValidator priceValidator )
    {
        this.priceValidator = priceValidator;
    }


    @Autowired
    public void setIdValidator( IdValidator idValidator )
    {
        this.idValidator = idValidator;
    }


    @Autowired
    public void setDateValidator( DateValidator dateValidator )
    {
        this.dateValidator = dateValidator;
    }


    @Autowired
    public void setQuantityValidator( QuantityValidator quantityValidator )
    {
        this.quantityValidator = quantityValidator;
    }


    @Autowired
    public void setTransactionTypeValidator( TransactionTypeValidator transactionTypeValidator )
    {
        this.transactionTypeValidator = transactionTypeValidator;
    }


    @Autowired
    public void setUomValidator( UomValidator uomValidator )
    {
        this.uomValidator = uomValidator;
    }


    @Autowired
    public void setCurrencyValidator( CurrencyValidator currencyValidator )
    {
        this.currencyValidator = currencyValidator;
    }


    /**
     *
     *
     * @param value
     * @param helper
     * @return
     */
    public String getMerchantName( Map<String, Object> value, FieldExtractionRequest helper, Map<String, Object> configuration )
    {
        String merchantName = null;
        if ( value != null ) {
            merchantName = value.get( "value" ) != null ? value.get( "value" ).toString().toLowerCase() : "";
        }

        List<String> validMerchantNames = configService.getValueList( "validMerchantNames", configuration );

        // If not found, find the names in the text
        if ( merchantName == null || merchantName.isEmpty() ) {
            String originalExtractedText = helper.getOcrData().getRawText();
            for ( String validMerchantName : validMerchantNames ) {
                if ( originalExtractedText.toLowerCase().contains( " " + validMerchantName + " " ) ) {
                    merchantName = validMerchantName;
                    break;
                }
            }
        } else if ( !validMerchantNames.contains( merchantName ) ) {
            for ( String validMerchantName : validMerchantNames ) {
                if ( merchantName.contains( validMerchantName ) ) {
                    merchantName = validMerchantName;
                    break;
                }
            }
        }


        LOG.debug( "Found merchant name: {}", merchantName );
        return merchantName;
    }


    public String getSyntax( DocumentMetaData metaData, String[] lines, FieldExtractionRequest helper,
        ConfidenceCalculator confidenceCalculator, Map<String, Object> configuration )
    {
        String syntax = "";
        // Get syntax of configured merchants
        Map<String, Object> syntaxMap = configService.getExtractionConfigurationMap( "line_item_syntax", configuration );
        if ( syntaxMap != null ) {
            String merchantName = StringUtils.getKeyFromMapByMatchingRegex( syntaxMap, metaData.getMerchantName() );
            if ( merchantName != null && !merchantName.isEmpty() ) {
                syntax = (String) syntaxMap.get( merchantName );
                confidenceCalculator.setSyntaxConfidence( confidenceCalculator.BASE_CONFIDENCE_VALUE );
            }
        }
        if ( null == syntax || syntax.isEmpty() ) {
            // Scan through the lines and get syntax
            syntax = syntaxExtractor.extractSyntax( lines, metaData, helper, confidenceCalculator, configuration );
        }
        return syntax;
    }


    public void setLineObjectsFromSyntax( LineItem currentLineObj, LineItemHelper lineItemHelper, String[] syntaxOrder,
        Domain domain, List<Integer> perfectMatchedSyntaxLines )
    {
        float numberOfNonOptionalFields = syntaxOrder.length - org.apache.commons.lang3.StringUtils
            .countMatches( lineItemHelper.getMetaData().getSyntax(), Constants.OPTIONAL_FIELD_SUFFIX );
        float numberOfFieldsExtracted = 0.0f;
        // Loop through each field in the extracted syntax
        for ( String syntax : syntaxOrder ) {
            boolean isOptional = false;
            String syntaxRegex = "";
            if ( syntax.contains( "[" ) ) {
                syntaxRegex = syntax.substring( syntax.indexOf( "[" ) + 1, syntax.lastIndexOf( "]" ) );
                LOG.trace( "Got Merchant specific regex {}", syntaxRegex );
                syntax = syntax.substring( 0, syntax.indexOf( "[" ) );
            }
            if ( syntax.contains( Constants.OPTIONAL_FIELD_SUFFIX ) ) {
                syntax = syntax.replace( Constants.OPTIONAL_FIELD_SUFFIX, "" );
                isOptional = true;
                lineItemHelper.getLineValidator().setOptional( true );
                LOG.trace( "{} is an optional field", syntax );
            }
            Constants.LINE_ITEM_FIELDS fieldName = Constants.LINE_ITEM_FIELDS.valueOf( syntax );
            LOG.trace( "Checking line for field: {}", fieldName );

            FieldValidator fieldValidator = getFieldsValidatorFromFieldName( fieldName, domain );

            // Perform field operation on line and validate it
            if ( fieldName == Constants.LINE_ITEM_FIELDS.TEXT && currentLineObj.getProductName() != null
                && !currentLineObj.getProductName().isEmpty() )
                continue;
            boolean isFieldSet = isFieldSet( currentLineObj, lineItemHelper, fieldValidator, syntaxRegex );
            LOG.trace( "{} field is set: {}", fieldName, isFieldSet );
            if ( !isFieldSet && ( !fieldValidator.isCompulsory()
                || ( fieldName.equals( Constants.LINE_ITEM_FIELDS.AMOUNT ) && !currentLineObj.getPrices().isEmpty() ) ) ) {
                currentLineObj.setAccurate( false );
                LOG.trace( "Current line item is not accurate" );
            } else if ( !isFieldSet && !isOptional ) {
                lineItemHelper.getLineValidator().setValidLine( false );
                currentLineObj.setAccurate( false );
                LOG.trace( "Current line item is not accurate and not valid" );
            } else if ( !isOptional ) {
                numberOfFieldsExtracted++;
            }
        }

        LOG.debug( "Number of fields extracted: {} and number of Non optional fields: {}", numberOfFieldsExtracted,
            numberOfNonOptionalFields );

        if ( numberOfNonOptionalFields > 0 ) {
            currentLineObj.setLineSyntaxMatchConfidence(
                ( numberOfFieldsExtracted / numberOfNonOptionalFields ) * ConfidenceCalculator.BASE_CONFIDENCE_VALUE );
            currentLineObj.setNonOptionalFieldsCount( numberOfNonOptionalFields );
            if ( numberOfFieldsExtracted == numberOfNonOptionalFields ) {
                perfectMatchedSyntaxLines.add( currentLineObj.getLineNumber() );
            }
        }
        LOG.trace( "Line Syntax Match confidence: {}", currentLineObj.getLineSyntaxMatchConfidence() );
    }


    public FieldValidator getFieldsValidatorFromFieldName( Constants.LINE_ITEM_FIELDS fieldName, Domain domain )
    {
        FieldValidator fieldValidator = null;
        switch ( fieldName ) {
            case TEXT:
                productNameValidator.setDomain( domain );
                fieldValidator = productNameValidator;
                break;
            case AMOUNT:
                fieldValidator = priceValidator;
                break;
            case ID:
                fieldValidator = idValidator;
                break;
            case DATE:
                fieldValidator = dateValidator;
                dateValidator.setDomain(domain);
                break;
            case QUANTITY:
                fieldValidator = quantityValidator;
                break;
            case TRANSACTION_TYPE:
                fieldValidator = transactionTypeValidator;
                break;
            case UOM:
                fieldValidator = uomValidator;
                break;
            case CURRENCY:
                currencyValidator.setDomain(domain);
                fieldValidator = currencyValidator;
        }
        return fieldValidator;
    }


    /**
     * This method extracts field from string and returns true if a value was extracted
     * @param currentLineObj - the line object to which the field is updated
     * @param fieldValidator - the field validator object that extracts the field from the line
     * @param syntaxRegex
     * @return a flag that tells whether the field was set or not
     */
    public boolean isFieldSet( LineItem currentLineObj, LineItemHelper helper, FieldValidator fieldValidator,
        String syntaxRegex )
    {
        boolean isField = false;
        LineValidator lineValidator = helper.getLineValidator();
        DocumentMetaData metaData = helper.getMetaData();
        List<Integer> indexOfSetField = new ArrayList<>();
        String inputStr = lineValidator.getRemainingLineString();
        // set remaining string as the substring from the extracted value in the original line
        lineValidator.setRemainingLineString( fieldValidator.setField( currentLineObj, indexOfSetField, false, lineValidator,
            syntaxRegex, metaData, null, null ) );
        if ( !inputStr.equals( lineValidator.getRemainingLineString() ) ) {
            isField = true;
        }
        return isField;
    }


    /**
     * Creates line item object according to the domain
     * @param metaData - the meta data of the document that contains domain detail
     * @return
     */
    public LineItem initializeLineObject( DocumentMetaData metaData )
    {
        LineItem line;
        switch ( metaData.getDomain().getName() ) {
            case bank:
                line = new BankLineItem();
                break;
            case receipt:
                line = new ReceiptLineItem();
                break;
            case invoice:
                line = new InvoiceLineItem();
                break;
            default:
                line = new LineItem();
                break;
        }
        return line;
    }


    /**
     * Returns true if the lines are present adjacent or atleast close to each other upto a certain threshold
     * @param previousLineObj - the previous line object
     * @param currentLineObj- the current line object
     * @return flag that tells if the lines are adjacent
     */
    public boolean isLineProximate( LineItem previousLineObj, LineItem currentLineObj )
    {
        return ( currentLineObj.getLineNumber() - previousLineObj.getLineNumber() ) <= MINIMUM_LINE_DIFFERENCE;
    }


    public void updateLineItems( List<LineItem> lineItems, boolean isAbsoluteConfidence )
    {
        int lineNo = 1;
        for ( LineItem lineItem : lineItems ) {
            ReceiptLineItem receiptLineItem = (ReceiptLineItem) lineItem;

            //Set the line number is series
            lineItem.setLineNumber( lineNo++ );

            if ( receiptLineItem.getQuantity() == 0 ) {
                receiptLineItem.setQuantity( 1 );
            }
            if ( receiptLineItem.getQuantityUnit() == null ) {
                receiptLineItem.setQuantityUnit( "ea" );
            }
            //Set the final line confidence
            receiptLineItem
                .setLineConfidence( ConfidenceValueHelper.getFinalConfidence( receiptLineItem, isAbsoluteConfidence ) );

            //Add negative sign for the final price of coupons
            if ( receiptLineItem.getProductName() != null && receiptLineItem.getProductName().toLowerCase().contains( "coupon" )
                && null != receiptLineItem.getFinalPrice() && receiptLineItem.getFinalPrice() > 0 ) {
                receiptLineItem.setFinalPrice( 0 - receiptLineItem.getFinalPrice() );
            }
        }
    }


    public void updateReceiptLIPrices( ReceiptLineItem receiptLineItem )
    {
        LOG.trace( "Setting unit price and final price" );
        // Set the unit price
        if ( receiptLineItem.getUnitPrice() == null && receiptLineItem.getPrices().size() > 1 && !receiptLineItem.isCoupon() ) {
            if ( receiptLineItem.getPrices().size() == 3 ) {
                receiptLineItem.setUnitPrice( receiptLineItem.getPrices().get( 1 ).doubleValue() );
            } else {
                receiptLineItem.setUnitPrice( receiptLineItem.getPrices().get( 0 ).doubleValue() );
            }
        }
        if ( receiptLineItem.getFinalPrice() == null && receiptLineItem.getPrices().size() > 1
            && !receiptLineItem.isCoupon() ) {
            receiptLineItem.setFinalPrice( receiptLineItem.getPrices().get( receiptLineItem.getPrices().size() - 1 ) );
        } else if ( !receiptLineItem.getPrices().isEmpty() && !receiptLineItem.isCoupon() ) {
            receiptLineItem.setFinalPrice( receiptLineItem.getPrices().get( 0 ) );
        }

    }


    public boolean areCompulsoryFieldsPresent( String lines, Map<String, Object> configurations, JsonObject taggedLineObj )
    {
        boolean compulsoryFieldsPresent = false;
        int isCompulsoryCounter = 0;
        List<String> compulsory = (List) configurations.get( Constants.COMPULSORY_FIELDS );
        LOG.debug( "Compulsory fields to be present: {}", compulsory );
        if ( taggedLineObj.size() >= compulsory.size() ) {
            for ( String lif : compulsory ) {
                switch ( Constants.LINE_ITEM_FIELDS.valueOf( lif ) ) {
                    case TEXT:
                        if ( StringUtils.checkIfStringContainsRegexPattern( lines, LineObjectValidator.WORD_REGEX ) ) {
                            isCompulsoryCounter++;
                        }
                        break;
                    case DATE:
                        if ( StringUtils.checkIfStringContainsRegexPattern( lines, DateValidator.DATE_REGEX ) ) {
                            isCompulsoryCounter++;
                        }
                        break;
                    case AMOUNT:
                        if ( StringUtils.checkIfStringContainsRegexPattern( lines,
                            PriceValidator.PERFECT_PRICE_REGEX_INVOICE ) ) {
                            isCompulsoryCounter++;
                        }
                        break;
                }
            }
            if ( isCompulsoryCounter == compulsory.size() )
                compulsoryFieldsPresent = true;
        }
        LOG.debug( "Compulsory fields present: {}", compulsoryFieldsPresent );
        return compulsoryFieldsPresent;
    }


    public boolean isLineContainsBannedWords( String line, List<String> bannedWords )
    {
        boolean isLineContainsBannedWords = false;
        for ( String shouldNotContain : bannedWords ) {
            if ( !StringUtils.getMatchedTokens( line.toLowerCase(), "\\b(?i)" + shouldNotContain + "\\b" ).isEmpty() ) {
                isLineContainsBannedWords = true;
                LOG.trace( "Line contains banned word" );
                break;
            }
        }
        return isLineContainsBannedWords;
    }


    public static boolean isLineContainsBannedWordsExactMatch( String line, List<String> bannedWords )
    {
        boolean isLineContainsBannedWords = false;
        for ( String shouldNotContain : bannedWords ) {
            if ( line.toLowerCase().equals( shouldNotContain ) ) {
                isLineContainsBannedWords = true;
                break;
            }
        }
        return isLineContainsBannedWords;
    }


    public Set<String> getLineBannedNames( String merchantName, Map<String, Object> configuration )
    {
        Set<String> lineBannedNames = new HashSet<>();
        lineBannedNames.addAll( configService.getValueList( Constants.RECEIPT_LINE_BANNEDWORDS_REGEX, configuration ) );
        List<String> endPoints = configService.getValueList( Constants.END_KEYWORDS, configuration );
        lineBannedNames.addAll( endPoints );
        lineBannedNames.addAll( getMerchantSpecificBannedWords( merchantName, configuration ) );
        return lineBannedNames;
    }


    public List<String> getMerchantSpecificBannedWords( String merchantName, Map<String, Object> configuration )
    {
        if ( null != merchantName && !merchantName.isEmpty() ) {
            String configuredMerchantName = StringUtils.getKeyFromMapByMatchingRegex(
                configService.getExtractionConfigurationMap( "bannedWords", configuration ), merchantName );
            if ( configuredMerchantName != null && !configuredMerchantName.isEmpty() )
                return (List<String>) configService.getExtractionConfigurationMap( "bannedWords", configuration )
                    .get( configuredMerchantName );
        }
        return new ArrayList<>();
    }


    /**
     * filters out unwanted items ( quantity, uom etc ) from product name
     * @param lineItem
     * @param metaData
     */
    public void filterProductName( LineItem lineItem, DocumentMetaData metaData, FieldExtractionRequest extractionData )
    {

        if ( lineItem instanceof ReceiptLineItem ) {
            ReceiptLineItem receiptLineItem = (ReceiptLineItem) lineItem;
            if ( receiptLineItem.getProductName() != null && !receiptLineItem.getProductName().isEmpty() ) {

                String productName = receiptLineItem.getProductName();

                // filter quantity and uom
                LineItemHelper helper = new LineItemHelper();
                helper.setLineValidator( new LineValidator( productName ) );
                helper.setMetaData( metaData );
                helper.setExtractionHelper( extractionData );
                if ( isFieldSet( lineItem.getCopy(), helper,
                    getFieldsValidatorFromFieldName( Constants.LINE_ITEM_FIELDS.UOM, metaData.getDomain() ), "" ) ) {
                    productName = helper.getLineValidator().getRemainingLineString();
                } else {
                    List<String> uomMatched = StringUtils.getMatchedTokens( productName,
                        "^((?i)" + UomValidator.getUomRegex() + ")\\s" );
                    if ( uomMatched.size() > 0 )
                        productName = productName.replace( uomMatched.get( 0 ), "" );
                }

                if ( isFieldSet( lineItem.getCopy(), helper,
                    getFieldsValidatorFromFieldName( Constants.LINE_ITEM_FIELDS.QUANTITY, metaData.getDomain() ), "" ) ) {
                    productName = helper.getLineValidator().getRemainingLineString();
                }

                for ( String w : ProductNameValidator.IGNORE_WORDS ) {
                    if ( ( " " + productName + " " ).contains( w ) ) {
                        productName = ( " " + productName + " " ).replace( w, "" ).trim();
                        break;
                    }
                }
                receiptLineItem.setProductName( productName );
            }
        }
    }


    public boolean previousLineProductNameContainsBannedWord( String previousLineItemProductName,
        Map<String, Object> configuration )
    {
        Set<String> previousLineProductNameBannedWords = new HashSet<>();
        previousLineProductNameBannedWords.addAll( configService
            .getValueList( Constants.RECEIPT_LINEITEM_PRIVOUS_LINE_PRODUCT_NAME_SHOULD_NOT_CONTAIN, configuration ) );
        for ( String previousLineProductNameShouldNotContain : previousLineProductNameBannedWords ) {
            if ( previousLineItemProductName.contains( previousLineProductNameShouldNotContain ) ) {
                return true;
            }
        }
        return false;
    }
}
