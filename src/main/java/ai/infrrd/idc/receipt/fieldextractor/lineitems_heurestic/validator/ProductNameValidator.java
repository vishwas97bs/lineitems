package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.commons.datastructures.Tuple2;
import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.*;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor.ReceiptLineItemExtractor;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.ConfigService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.GimletConfigService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class ProductNameValidator extends FieldValidator
{
    private static final String DESCRIPTION_AND_EXTRA_DATA_SEPARATOR_REGEX = "(?<=(^\\s{20,}))[^\\s]+";
    private static final List<String> CURRENCY_REGEX = Arrays.asList( "USD", "US$", "EUR", "GBP", "SEK", "CHF", "AUD", "CNY",
        "$", "£", "AED", "CHF", "GBP", "INR", "JPY", "SEK", "SGD" );

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( ProductNameValidator.class );
    private static final String STRICT_QUANTITY_REGEX = "(?i)(\\d{1,2}[.,]?\\d{0,3}(\\s)?"
        + UomValidator.getUomRegex().substring( 0, UomValidator.getUomRegex().length() - 1 ) + "|x))";

    private static final String COMMON_NAME_REGEX = "((\\b| )[a-zA-ZÀ-žÀ-џ():.!+\"]{2,100}|[a-zA-ZÀ-žᎠ-Ᏼ&.\\-():\"]{3,100})((\\s){1,100}(\\d{1,3})([\\.,]\\d{1})?(\\s{0,2})(%))?|.*[\\d]{2}[a-zA-Z]+.*[/!].*[a-zA-Z]{2}";

    private static final String RECEIPT_NAME_SHOULD_NOT_HAVE_REGEX = "( |^|T)(([\\$\\-\\.]?((\\d{1,40}([.,/ ]|$)){1,4})|((\\d{4,40}\\w{1,20})(\\s|$|\\)))|("
        + PriceValidator.CURRENCIES_STRING.toString().replace( "|:|[^0-9A-Za-z]{1,100}|$\\s?\\d{2-3}\\.?\\d{0,2}", "" )
        + ")([ ]?(\\d|$)))|(\\d{1,100} ))(?!%[a-zA-Z])";

    private static final String QUANTITY_OR_JUNK_IN_PRODUCT_NAME_BEG = "( |^)((\\d{1,3}(\\s)?[*x](\\s))|(\\+\\+)(\\s))";

    private static final String[] JUNK_CHARACTERS = { "?" };
    public static final String[] IGNORE_WORDS = { "sku" };
    private static final String[] FILTER_WORDS = { "a\\*" };
    private static final String[] JUNK_CHARS_IN_PROD_NAME = { "^(?i)(((\\s)?\\d?(\\s)?of:))", "(,)$" };

    private Domain domain;

    private ReceiptLineItemExtractor receiptLineItemExtractor;

    private LineItemExtractorUtils lineItemExtractorUtils;

    private ConfigService configService;

    private DiscountListValidator discountListValidator;

    private GimletConfigService gimletConfigService;


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


    @Autowired
    public void setConfigService( ConfigService configService )
    {
        this.configService = configService;
    }


    @Autowired
    public void setDiscountListValidator( DiscountListValidator discountListValidator )
    {
        this.discountListValidator = discountListValidator;
    }


    @Autowired
    public void setGimletConfigService( GimletConfigService gimletConfigService )
    {
        this.gimletConfigService = gimletConfigService;
    }


    //    public ProductNameValidator( Domain domain )
    //    {
    //        super( domain );
    //    }
    public void setDomain( Domain domain )
    {
        this.domain = domain;
    }


    @Override
    public String setField( LineItem dummyLine, List<Integer> indexes, boolean setAllMatches, LineValidator lineValidator,
        String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionData,
        Map<String, Object> configuration )
    {
        //TODO: put this after validating for junk values
        String inputLine = lineValidator.getRemainingLineString();
        LOG.info( "Validating name for scanReq : line: {}", inputLine );
        if ( configuration == null ) {
            configuration = gimletConfigService.getGimletConfig();
        }
        String probableName = "";
        for ( String val : FILTER_WORDS ) {
            String toReplace = "(?i)( " + val + " )";
            inputLine = inputLine.replaceAll( toReplace, " " );
        }
        String[] lineElements = inputLine.split( "\\s{2,}" );
        if ( lineValidator.getRawLineString() != null && !lineValidator.getRemainingLineString().isEmpty()
            && inputLine.equals( lineValidator.getRemainingLineString() )
            && metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.receipt ) && receiptLineItemExtractor
                .checkForMerchantsWithMultilineProductDescription( metaData, extractionData, configuration ) ) {
            List<Integer> tokens = StringUtils.getMatchedTokensWithIndex( lineValidator.getRawLineString(),
                DESCRIPTION_AND_EXTRA_DATA_SEPARATOR_REGEX );
            if ( tokens.size() > 0 && tokens.get( 0 ) > 0.5 * lineValidator.getDocWidth() ) {
                lineElements = new String[] {};
            } else {
                lineElements = lineValidator.getRawLineString().trim().split( "\\s{20,}" )[0].replaceAll( "\\s{1,2}", " " )
                    .split( "\\s{4,}" );
            }
        }

        List<String> listOfStrings = StringUtils.convertStringArrayToList( lineElements );
        probableName = getLongestValidName( listOfStrings ).trim();

        boolean isValid = !probableName.isEmpty();
        boolean isLineToReject = false;
        if ( !metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
            Set<String> lineBannedNames = lineItemExtractorUtils.getLineBannedNames( metaData.getMerchantName(),
                configuration );
            isLineToReject = lineItemExtractorUtils.isLineContainsBannedWords(
                lineValidator.getLineStr().replaceAll( "[()]", "" ), new ArrayList<>( lineBannedNames ) );
            if ( isLineToReject ) {
                lineValidator.setLineToReject( true );
                isValid = false;
            } else if ( isValid ) {
                isValid = !lineItemExtractorUtils.isLineContainsBannedWords( inputLine,
                    configService.getValueList( Constants.RECEIPT_NAME_SHOULD_NOT_CONTAIN, configuration ) );
            }
        }
        float fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE;

        if ( isValid ) {
            if ( containsBadWords( lineValidator ) ) {
                fieldConfidence -= ConfidenceCalculator.MINIMUM_CONF_DIFF;
            }
            if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.receipt ) ) {
                List<Tuple2<String, Integer>> matchedTokens = StringUtils.getMatchedTokensWithTheirIndex( probableName,
                    RECEIPT_NAME_SHOULD_NOT_HAVE_REGEX );
                if ( !matchedTokens.isEmpty() && probableName.indexOf( matchedTokens.get( 0 ).f1() ) != -1 ) {
                    for ( Tuple2<String, Integer> token : matchedTokens ) {
                        String beforeTokenIndex = probableName.substring( 0, token.f2() );
                        String afterTokenIndex = probableName.substring( token.f2() + token.f1().length() );
                        probableName = beforeTokenIndex + StringUtils.getSpacesFromInputCount( token.f1().length() )
                            + afterTokenIndex;
                    }
                    String[] probableNameArr = probableName.trim().split( "\\s{2,}" );
                    probableName = probableNameArr[0];
                    if ( !setAllMatches ) {
                        probableName = considerNumbersAtStartAndInBetweenName( probableName, inputLine, probableNameArr );
                    }
                }

                List<Tuple2<String, Integer>> matchedQuantity = StringUtils.getMatchedTokensWithTheirIndex( probableName,
                    QUANTITY_OR_JUNK_IN_PRODUCT_NAME_BEG );
                if ( !matchedQuantity.isEmpty() && probableName.indexOf( matchedQuantity.get( 0 ).f1() ) != -1 ) {
                    for ( Tuple2<String, Integer> token : matchedQuantity ) {
                        LOG.info( "probable name: {}, token f2: {}, f1: {}, length: {}", probableName, token.f2(), token.f1(),
                            probableName.length() );
                        if ( ( token.f2() + token.f1().length() ) < probableName.length() ) {
                            probableName = probableName.substring( token.f2() + token.f1().length() );
                        }
                    }
                }
                if ( probableName.isEmpty() || probableName.length() <= 2
                    || isNameContainsOnlyBannedWord( probableName,
                        configService.getValueList( Constants.RECEIPT_NAME_SHOULD_NOT_CONTAIN_PERFECT_MATCH, configuration ) )
                    || isNameContainsOnlyQunatity( probableName ) || isNameContainsOnlyPrice( probableName ) ) {
                    isValid = false;
                }
            }
        }

        if ( isValid ) {
            if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.bank ) ) {
                probableName = splitText( probableName, metaData, (BankLineItem) dummyLine );
            }
            probableName = removeUnneccessaryCharactersFromName( probableName );
            dummyLine.setProductName( probableName );
            /**
             * Setting isCoupon value
             */
            if ( domain.getName().equals( Constants.DOMAIN_NAME.receipt ) ) {
                discountListValidator.setDiscountList( dummyLine, lineValidator.getLineStr(), probableName, metaData,
                    extractionData );
            }

            /**
             * Splitting by space & getting the first element
             * Since the matched tokens doesn't match completely with the inputline string
             */
            int nameIndex = inputLine.indexOf( probableName.split( " " )[0] );
            indexes.add( nameIndex );
            if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.bank ) ) {
                ( (BankLineItem) dummyLine ).setNameIndex( nameIndex );
            } else if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
                ( (InvoiceLineItem) dummyLine ).setNameIndex( nameIndex );
            }
            int nameIndexStart = inputLine.indexOf( probableName.split( "\\s" )[0] ) + probableName.split( "\\s" )[0].length();
            if ( setAllMatches ) {
                if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.invoice ) ) {
                    inputLine = StringUtils.replaceGivenStringWithSpaces( inputLine, probableName );
                } else {
                    inputLine = StringUtils.replacePatternWithSpaces( inputLine, probableName );
                }
            } else {
                if ( nameIndexStart < inputLine.length() )
                    inputLine = inputLine.substring( nameIndexStart );
                try {
                    LOG.debug( "Name field confidence: {}", fieldConfidence );
                    dummyLine.addToFieldConfidenceList( fieldConfidence );
                } catch ( LineExtractionException e ) {
                    LOG.error( "Exception while assigning Name field confidence: " + e );
                }
            }
        }
        return inputLine;
    }


    private boolean isNameContainsOnlyPrice( String probableName )
    {
        boolean status = false;
        List<String> matchedValues = StringUtils.getMatchedTokens( probableName, PriceValidator.PERFECT_PRICE_REGEX );
        for ( String val : matchedValues ) {
            probableName = probableName.replace( val, "" );
        }
        for ( String val : CURRENCY_REGEX ) {
            probableName = probableName.replace( val, "" );
        }
        probableName = probableName.trim();
        if ( probableName.isEmpty() || probableName.replaceAll( " [a-z] ", "" ).isEmpty() ) {
            status = true;
        }
        return status;
    }


    private String removeUnneccessaryCharactersFromName( String probableName )
    {
        for ( String word : JUNK_CHARS_IN_PROD_NAME ) {
            probableName = probableName.replaceAll( word, "" );
        }
        return probableName.trim();
    }


    private boolean isNameContainsOnlyQunatity( String probableName )
    {
        boolean status = false;
        List<String> matchedValues = StringUtils.getMatchedTokens( probableName, STRICT_QUANTITY_REGEX );
        for ( String val : matchedValues ) {
            probableName = probableName.replace( val, "" );
        }
        if ( probableName.isEmpty() || probableName.replaceAll( " [a-z] ", "" ).isEmpty() ) {
            status = true;
        }
        return status;
    }


    private String splitText( String probableName, DocumentMetaData metaData, BankLineItem dummyLine )
    {
        List<Tuple2> matchCurrency = new CurrencyValidator().matchCurrency( probableName );
        for ( Tuple2 tuple : matchCurrency ) {
            dummyLine.addToCurrency( (String) tuple.f1() );
            probableName = probableName.replace( (String) tuple.f1(), "" ); //StringUtils.replacePatternWithSpaces( probableName, ( (String) tuple.f1() ) );
        }

        return probableName;
    }


    private String considerNumbersAtStartAndInBetweenName( String probableName, String inputLine, String[] probableNameArr )
    {

        if ( inputLine.trim().indexOf( probableName ) != 0 ) {
            probableName = inputLine.trim().substring( 0, inputLine.trim().indexOf( probableName ) ) + probableName;
        }
        if ( probableNameArr.length > 1 && probableNameArr[1].length() >= 2
            && inputLine.trim().indexOf( probableNameArr[1] ) < 0.7 * inputLine.trim().length()
            && inputLine.trim().indexOf( " " + probableNameArr[1] ) != -1
            && !StringUtils.getMatchedTokens( " " + probableNameArr[1], COMMON_NAME_REGEX ).isEmpty()
            && inputLine.trim().indexOf( probableName ) + probableName.length() < inputLine.trim()
                .indexOf( " " + probableNameArr[1] ) + probableNameArr[1].length() + 1 ) {
            probableName = probableName
                + inputLine.trim().substring( inputLine.trim().indexOf( probableName ) + probableName.length(),
                    inputLine.trim().indexOf( " " + probableNameArr[1] ) + probableNameArr[1].length() + 1 );
        }
        return probableName;
    }


    private boolean isNameContainsOnlyBannedWord( String probableName, List<String> nameBannedWords )
    {
        boolean isNameContainsOnlyBannedWord = false;
        //RECEIPT_NAME_SHOULD_NOT_CONTAIN_PERFECT_MATCH
        for ( String nameBannedWord : nameBannedWords ) {
            if ( probableName.trim().equalsIgnoreCase( nameBannedWord.trim() ) ) {
                isNameContainsOnlyBannedWord = true;
                break;
            }
        }
        return isNameContainsOnlyBannedWord;
    }


    private static String getLongestValidName( List<String> lineElements )
    {
        LOG.trace( "Method: getLongestValidName called." );

        String probableName = StringUtils.getLongestAlphabetString( lineElements );
        if ( !probableName.isEmpty() && !StringUtils.checkIfStringContainsRegexPattern( probableName, COMMON_NAME_REGEX ) ) {
            lineElements.remove( probableName );
            probableName = getLongestValidName( lineElements );
        }

        LOG.trace( "Method: getLongestValidName finished." );
        return probableName;
    }


    @Override
    public boolean isCompulsory()
    {
        return true;
    }


    @Override
    public boolean isOptional()
    {
        return false;
    }


    private boolean containsBadWords( LineValidator lineValidator )
    {
        String line = lineValidator.getLineStr();
        for ( String junkCharacter : JUNK_CHARACTERS ) {
            if ( line.contains( junkCharacter ) ) {
                return true;
            }
        }
        return false;
    }


    public static boolean isValidProductName( String productName )
    {
        return StringUtils.getMatchedTokens( productName, "[^a-zA-Z0-9\\s\\-\\_]{5,}|#\\s?\\d{2,}" ).size() == 0;
    }
}
