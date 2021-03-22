package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.commons.datastructures.Tuple2;
import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.DocumentMetaData;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.Domain;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.BankLineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.ConfidenceCalculator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.StringUtils;
import ai.infrrd.idc.utils.entity.RegexMatchInfo;
import ai.infrrd.idc.utils.extractors.PatternExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Component
public class CurrencyValidator extends FieldValidator
{

    private static Logger LOG = LoggerFactory.getLogger( CurrencyValidator.class );
    private static List<Tuple2<PatternExtractor, Double>> patternSet;
    private Domain domain;

    // Currency notations
    private static final List<String> CURRENCIES = Arrays.asList( "USD", "US$", "EUR", "GBP", "SEK", "CHF", "CAD", "AUD", "NZD",
        " C ", "ZAR", "CNY", "CRC", "BND", "$", "£", "AED", "AFN", "ALL", "AMD", "AOA", "ARS", "AWG", "AZN", "BAM", "BBD",
        "BDT", "BGN", "BHD", "BIF", "BMD", "BOB", "BRL", "BSD", "BTN", "BWP", "BYR", "BZD", "CDF", "CLP", "COP", "CUP", "CVE",
        "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN", "ETB", "FJD", "FKP", "GEL", "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD",
        "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "INR", "IQD", "IRR", "ISK", "JMD", "JOD", "JPY", "KES", "KGS", "KHR",
        "KPW", "KRW", "KWD", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT",
        "MOP", "MRO", "MUR", "MVR", "MWK", "MXN", "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "OMR", "PAB", "PEN", "PGK",
        "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR", "SBD", "SCR", "SDG", "SGD", "SHP", "SLL", "SOS",
        "SRD", "STD", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY", "TTD", "TWD", "TZS", "UAH", "UGX", "UYU", "UZS",
        "VEF", "VND", "VUV", "WST", "XAF", "XCD", "XPF", "YER", "ZMW", "ZWL" );

    static {
        StringBuilder regexBuilder = new StringBuilder();
        for ( String currency : CURRENCIES ) {
            regexBuilder.append( "|" + currency );
        }
        regexBuilder = new StringBuilder( regexBuilder.toString().replaceFirst( "\\|", "" ) );
        String regex = regexBuilder.toString();
        patternSet = new ArrayList<>();
        //SPACE_PADDED_UPPERCASE_CURRENCY_REGEX
        patternSet.add( new Tuple2<>( new PatternExtractor( "[ ](" + regex + ")[ ]" ), 1.0 ) );
        //ERROR_PADDED_UPPERCASE_CURRENCY_REGEX
        patternSet.add( new Tuple2<>( new PatternExtractor( "[^a-zA-Z](" + regex + ")[^a-zA-Z]" ), 0.9 ) );
        //SPACE_PADDED_LOWERCASE_CURRENCY_REGEX
        patternSet.add( new Tuple2<>( new PatternExtractor( "[ ](" + regexBuilder.toString().toLowerCase() + ")[ ]" ), 0.9 ) );
        //ERROR_PADDED_LOWERCASE_CURRENCY_REGEX
        patternSet.add(
            new Tuple2<>( new PatternExtractor( "[^a-zA-Z](" + regexBuilder.toString().toLowerCase() + ")[^a-zA-Z]" ), 0.8 ) );
    }

    public void setDomain( Domain domain )
    {
        this.domain = domain;
    }

    /**
     * Return true if field is compulsory, else return false
     * @return boolean that defines whether the field is compulsory or not
     */
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


    /**
     * currencies fetched from line string
     * @param lineString
     * @return list of currencies matched with confidence
     */
    public List<Tuple2> matchCurrency( String lineString )
    {
        List<Tuple2> resultSet = new ArrayList<>();
        lineString = " " + lineString + " ";

        for ( Tuple2 tuple : patternSet ) {

            List<RegexMatchInfo> regexMatchInfoList = ( (PatternExtractor) tuple.f1() ).matchedPatterns( lineString );
            for ( RegexMatchInfo regexMatchInfo : regexMatchInfoList ) {
                String matchedCurrency = regexMatchInfo.getMatchedString();
                matchedCurrency = matchedCurrency.substring( 1, matchedCurrency.length() - 1 );

                resultSet.add( new Tuple2<>( matchedCurrency, (Double) tuple.f2() ) );
                lineString = lineString.replaceAll( matchedCurrency, " " );
            }
        }

        return resultSet;
    }


    @Override
    public String setField( LineItem line, List<Integer> indexes, boolean setAllMatches, LineValidator helper,
        String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionHelper,
        Map<String, Object> configuration )
    {
        String inputLine = helper.getRemainingLineString();
        List<Tuple2> currenciesMatched = matchCurrency( inputLine );

        LOG.info( "Validating currency for scanReq  line: {}. values matched : {}", inputLine, currenciesMatched );

        if ( setAllMatches ) {
            if ( !currenciesMatched.isEmpty() ) {

                for ( Tuple2 tuple : currenciesMatched ) {
                    indexes.add( inputLine.indexOf( ( (String) tuple.f1() ) ) );
                    inputLine = StringUtils.replacePatternWithSpaces( inputLine, ( (String) tuple.f1() ) );
                }
            }
        }
        try {
            // confidence added to line items
            if ( !currenciesMatched.isEmpty() )
                line.addToFieldConfidenceList( ConfidenceCalculator.BASE_CONFIDENCE_VALUE );
        } catch ( LineExtractionException e ) {
            LOG.error( "Exceptions caught while assigning currency feild confidence : {}", e );
        }

        // currencies extracted added to line items
        if ( !currenciesMatched.isEmpty() ) {
            BankLineItem bankLineItem = line instanceof BankLineItem ? (BankLineItem) line : null;
            if ( bankLineItem != null ) {
                for ( Tuple2 tuple : currenciesMatched )
                    bankLineItem.addToCurrency( ( (String) tuple.f1() ).toUpperCase() );
            }
        }

        return inputLine;
    }
}
