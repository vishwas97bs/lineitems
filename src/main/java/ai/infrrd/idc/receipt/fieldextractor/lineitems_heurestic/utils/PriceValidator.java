package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.DocumentMetaData;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.Domain;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.GimletConfigService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.DateValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.DiscountListValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator.FieldValidator;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Component
public class PriceValidator extends FieldValidator implements InitializingBean
{
    //    public PriceValidator( Domain domain )
    //    {
    //        super( domain );
    //    }
    public static StringBuilder CURRENCIES_STRING = new StringBuilder();

    @Override
    public void afterPropertiesSet() throws Exception
    {
        for ( String currency : CURRENCIES ) {
            CURRENCIES_STRING.append( "|" + currency );
        }
        CURRENCIES_STRING = new StringBuilder( CURRENCIES_STRING.toString().replaceFirst( "\\|", "" ).replace( "$", "\\$" ) );
        PERFECT_PRICE_REGEX = PERFECT_PRICE_REGEX.replace( "<currencies>", CURRENCIES_STRING );
        PRICE_WITH_SPACES_REGEX = PRICE_WITH_SPACES_REGEX.replace( "<currencies>", CURRENCIES_STRING );

        PERFECT_PRICE_REGEX_INVOICE = PERFECT_PRICE_REGEX_INVOICE.replace( "<currencies>", CURRENCIES_STRING );
        PRICE_WITH_SPACES_REGEX_INVOICE = PRICE_WITH_SPACES_REGEX_INVOICE.replace( "<currencies>", CURRENCIES_STRING );
    }

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( PriceValidator.class );
    /**
     * Should match :
     USD12.34
     USD 34.56
     skfjl EUR 56.78-
     skdfjsk 89.89
     ksajdf -7.90
     */
    //    public static String PERFECT_PRICE_REGEX = "((\\s|^)(<currencies>){0,4} ?((\\d{1,3})|(\\d{1,2}[, ]?\\d{3}))[.,]\\d{2}-?)((?!(\\d{2,20}|[.]|(\\d kg)|[A-Z]{3,10})))";

    public static final String PRICE_NEG_LOOKAFTER = "inches";

    public static String PERFECT_PRICE_REGEX = "\\s((-?(?>(<currencies>))?( |\\*)?(((\\d{1,3}[,.]?\\d{3})|\\d{1,3}))[.,]\\d{2}-?))(?!(\\d{2,10}|(\\s)?(?i)("
        + PRICE_NEG_LOOKAFTER + ")))";

    private DiscountListValidator discountListValidator;

    private GimletConfigService gimletConfigService;

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

    /* Matches:
                sjkf 7 .90
                jsdfh 3. 90
              */
    //should match   13 444 16  but not 15/08/2014 14 44 16 (Using negative look-behind assertion)
    //(?<!\d)\s((USD|US\$|EUR|GBP|SEK|CHF|€|\$|S|£|¥|₹|₩|₭|₮|₱|₵|₡|(\\u[a-zA-Z0-9]{4})|Â£|�|\*|«|Â«|-)? ?([,.] | [,.]| )\d{2}-?)(?=[^0-9.]|$)
    public static String PRICE_WITH_SPACES_REGEX = "\\s(?>(<currencies>|-)?) ?((\\d{1,3}[,. ]\\d{3})|(\\d{1,3}))([,.] | [,.]| |\\s?[.,]\\s?)\\d{2}0?-?(\\s|$)(?!(\\s)?(?i)("
        + PRICE_NEG_LOOKAFTER + "))";

    //should match 12,121,212.23  1.036,53  12,23  and not 1.8.2017
    public static String PERFECT_PRICE_REGEX_INVOICE = "\\s(?>(<currencies>|-)? ?([\\(]?)(((\\d{1,3}[,.])?(\\d{3,6}[.,]){1,5}\\d{2,4}-?)|(\\d{1,3}[.,]\\d{2,4}-?))([\\)]?)(?=[^0-9]|$))";
    public static String PRICE_WITH_SPACES_REGEX_INVOICE = "\\s(?>(<currencies>|-)? ?\\d{0,2}[ ]?\\d{1,3}(, | ,)\\d{2,4}-?)(?=[^0-9.]|$)";
    public static String PRICE_ALPHA_REGEX = "(?i)(free|freefree)";

    private static final List<String> CURRENCIES = Arrays.asList( "USD", "US$", "EUR", "GBP", "SEK", "CHF", "EUR/St", "ce", "e",
        "C", "(?>(\\\\u[a-zA-Z0-9]{4}))", "(?>([^0-9A-Za-z -.,/:]{1,4}))", "$" );

    private static final String DATE_REGEX_TO_NEGLECT = DateValidator.DATE_REGEX_4 + Constants.REGEX_SEPARATOR
        + DateValidator.DATE_REGEX_5 + Constants.REGEX_SEPARATOR + DateValidator.DATE_REGEX_7 + Constants.REGEX_SEPARATOR
        + "\\d{2} \\d{2} \\d{2}(?!(\\.\\d{2}))";

    private static final String NEGATIVE_PRICE_REGEX = DATE_REGEX_TO_NEGLECT + Constants.REGEX_SEPARATOR + "\\d[A-Z]{3,10}"
        + Constants.REGEX_SEPARATOR + "\\d{2} kg";

    //    private static final List<String> CURRENCIES = Arrays.asList( "(\\\\u[a-zA-Z0-9]{4})", "\\*", "«", "Â«", ":",
    //        "[^0-9A-Za-z]{1,100}", "@£", "@", "C", "ce", "e" );
    private float fieldConfidence = 0.0f;

    @Override
    public String setField( LineItem dummyLine, List<Integer> indexes, boolean setAllMatches, LineValidator lineValidator,
        String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionHelper,
        Map<String, Object> configuration )
    {
        String inputLine = lineValidator.getRemainingLineString();
        LOG.info( "Validating price for scanReq  line: {}", inputLine );
        // Get matched prices from perfect match regex
        List<String> priceMatch = getMatchedPrices( inputLine, metaData, merchantSpecificSyntaxRegex );
        if ( !metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.bank ) ) {
            cleanupAndAddToPriceList( dummyLine, priceMatch, metaData );
        }
        if ( !priceMatch.isEmpty() && !setAllMatches ) {

            //Get the last price matched if only one amount is there in the syntax else get the first one.
            if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.receipt ) ) {

                //TODO: take it out to updateLineItems method in LineItemExtractorUtils
                if ( dummyLine.getIsCoupon() ) {
                    priceMatch = getMatchedPrices( lineValidator.getLineStr(), metaData, merchantSpecificSyntaxRegex );
                    if ( !priceMatch.isEmpty() ) {
                        List<Float> convertPrices = new ArrayList<>();
                        for ( String pricesToConvert : priceMatch ) {
                            pricesToConvert = cleanupPrice( pricesToConvert );
                            convertPrices.add( Float.parseFloat( pricesToConvert ) );
                        }
                        dummyLine.setFinalPrice( discountListValidator.getMin( convertPrices ) );
                    }
                }
            } else if ( metaData.getDomain().getName().equals( Constants.DOMAIN_NAME.bank ) ) {
                try {
                    dummyLine.addToPriceList( cleanupPrice( priceMatch.get( 0 ) ) );
                } catch ( LineExtractionException e ) {
                    LOG.error( "Exception while cleanup Price", e );
                }
            } else {
                dummyLine.setFinalPrice( dummyLine.getPrices().get( 0 ) );
            }

            inputLine = inputLine.substring( inputLine.indexOf( priceMatch.get( 0 ) ) + priceMatch.get( 0 ).length() );
            try {
                LOG.debug( "Price Field Confidence: {}", fieldConfidence );
                dummyLine.addToFieldConfidenceList( fieldConfidence );
            } catch ( LineExtractionException e ) {
                LOG.error( "Exception while assigning Price field confidence: " + e );
            }
        } else if ( !priceMatch.isEmpty() && setAllMatches ) {
            for ( String priceMatched : priceMatch ) {
                indexes.add( inputLine.indexOf( priceMatched ) );
                inputLine = StringUtils.replaceStringWithSpace( inputLine, priceMatched );
            }
        }
        LOG.trace( "Method: getIndexesOfField finished." );
        return inputLine;
    }


    public String cleanupPrice( String price )
    {
        if ( StringUtils.getMatchedTokens( price, PRICE_ALPHA_REGEX ).size() > 0 )
            price = "0.00";
        // Sometimes there is negative sign succeeding the price eg. walmart
        if ( price.length() > 0
            && ( price.charAt( price.length() - 1 ) == '-' || price.charAt( price.length() - 2 ) == '-' ) ) {
            price = "-" + price.replace( "-", "" );
        } else if ( price.contains( "-" ) && price.indexOf( "-" ) == price.length() - 3 ) {
            price = price.replace( "-", "." );
            fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE - ConfidenceCalculator.MINIMUM_CONF_DIFF;
        }
        // For prices like 1.234,56
        if ( price.contains( "," ) && price.contains( "." ) && price.indexOf( ',' ) > price.indexOf( '.' ) ) {
            price = price.replace( ".", "" ).replace( ',', '.' );
        }
        // For prices like 1,234 56 converts to 1234.56
        if ( price.contains( "," ) && price.contains( " " ) && price.indexOf( ' ' ) > price.indexOf( ',' ) ) {
            price = price.replace( ",", "" ).replace( ' ', '.' );
        }

        // For prices like 1 234 56 converts to 1234.56
        if ( !price.contains( "." ) && StringUtils.checkIfStringContainsMultipleKeywords( price, " " )
            && price.charAt( price.length() - 3 ) == ' ' ) {
            price = price.replace( " ", "" );
            price = price.substring( 0, price.length() - 2 ) + "." + price.substring( price.length() - 2 );
        }

        if ( !price.contains( "." ) ) {
            // For prices like 12,34
            if ( price.contains( "," ) ) {
                price = price.replace( ",", "." );
            }
            // For prices like 12 34
            else if ( price.contains( " " ) ) {
                price = price.replace( " ", "." );
            }
        }
        if ( price.contains( "ï¿½" ) ) {
            price = price.replace( "ï¿½", "" );
        }
        price = price.replace( ",", "" ).replace( " ", "" );
        price = removeCurrencyChars( price );
        price = convertToFloat( price );
        return price;
    }


    private String removeCurrencyChars( String price )
    {
        String finalPrice = StringUtils.getMatchedTokens( price, "\\d{1,20}[.]?\\d{1,2}" ).get( 0 );
        if ( price.contains( "-" ) ) {
            finalPrice = "-" + finalPrice;
        }
        return finalPrice;
    }


    public List<String> getMatchedPrices( String inputLine, DocumentMetaData metaData, String merchantSpecificSyntaxRegex )
    {
        String priceRegex;
        String priceSpaceRegex = "";
        //TODO handle language specific code

        inputLine = StringUtils.replaceRegexMatchesWithSpaces( inputLine, NEGATIVE_PRICE_REGEX );
        // Get matched prices from perfect match regex
        List<String> priceMatch = new ArrayList<>();
        // Currency symbol to be removed from price
        fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE;
        if ( null != merchantSpecificSyntaxRegex && !merchantSpecificSyntaxRegex.isEmpty() ) {
            priceRegex = merchantSpecificSyntaxRegex;
        } else if ( metaData.getDomain().getName().toString().equals( Constants.DOMAIN_NAME.invoice.toString() ) ) {
            priceRegex = PERFECT_PRICE_REGEX_INVOICE;
            priceSpaceRegex = PRICE_WITH_SPACES_REGEX_INVOICE;
            priceMatch = StringUtils.getMatchedTokens( inputLine, priceRegex );
        } else {
            priceRegex = PERFECT_PRICE_REGEX;
            priceSpaceRegex = PRICE_WITH_SPACES_REGEX;
        }

        if ( priceMatch.isEmpty() ) {
            priceMatch = StringUtils.getMatchedTokens( inputLine, priceRegex );
        }
        if ( priceMatch.isEmpty() ) {
            priceMatch = StringUtils.getMatchedTokens( inputLine, PRICE_ALPHA_REGEX );
        }
        //To validate prices like $.99
        //        if ( matchedCurrencySymbols.isEmpty() ) {
        //            matchedCurrencySymbols = StringUtils.getMatchedTokensFromSpecifiedRegexGroup( inputLine, PERFECT_PRICE_REGEX, 4 );
        //        }

        //Prices with spaces
        if ( priceMatch.isEmpty() ) {
            if ( priceSpaceRegex != "" ) {
                priceMatch = StringUtils.getMatchedTokens( inputLine, priceSpaceRegex );
                fieldConfidence = ConfidenceCalculator.BASE_CONFIDENCE_VALUE - ConfidenceCalculator.MINIMUM_CONF_DIFF;
            }
        }
        return priceMatch;
    }


    private void cleanupAndAddToPriceList( LineItem dummyLine, List<String> priceMatch, DocumentMetaData metaData )
    {
        if ( ( priceMatch != null && !priceMatch.isEmpty() ) ) {
            if ( dummyLine.getProductName() != null && !dummyLine.getProductName().isEmpty() ) {
                String price;
                if ( StringUtils.checkIfStringContainsMultipleKeywords( metaData.getSyntax(),
                    Constants.LINE_ITEM_FIELDS.AMOUNT.name() ) ) {
                    price = priceMatch.get( 0 );
                } else {
                    price = priceMatch.get( priceMatch.size() - 1 );
                }
                price = cleanupPrice( price );
                try {
                    dummyLine.addToPriceList( price );
                } catch ( LineExtractionException e ) {
                    LOG.error( "Error while adding price to priceList", e );
                }
            } else {
                for ( String price : priceMatch ) {
                    price = cleanupPrice( price );
                    try {
                        dummyLine.addToPriceList( price );
                    } catch ( LineExtractionException e ) {
                        LOG.error( "Error while adding price to priceList", e );
                    }
                }
            }
        }
    }


    private String convertToFloat( String price )
    {
        if ( !price.contains( "." ) ) {
            price = price.substring( 0, price.length() - 2 ) + "." + price.substring( price.length() - 2 );
        }
        return price;
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
}
