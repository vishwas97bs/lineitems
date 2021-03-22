package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.PriceValidator;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.StringUtils;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class LineObjectValidator {
    private static Map<String, String> fieldsRegexMap = new LinkedHashMap<>();

    private static String DATE_KEYWORDS = "|date|";
    private static String QUANTITY_KEYWORDS = "|qty|q'ty|weight|quantity|ord|ship|ordered|shipped|delivered|order|plu|del|return|each|total|line|uom qty|";
    private static String PRICE_KEYWORDS = "|debit|credit|balance|amount|total|amt|charges|rate|price|charge amt|value|cost|subtotal|tax|vat|unit price|uom price|extended|tutari|tutar|line total|pri|srp|";
    private static String ALPHANUMERIC_KEYWORDS = "|stock code|item no|item number|ref|item#|rev hist jartidej|jd|invoice details item|";
    private static String UOM_KEYWORDS = "|u/m|uom|";
    public static String WORD_KEYWORDS = "|description|details|employee|branch|gl code|remarks|line opcode tech type a/hrs s/hrs|line opcode tech type hours|";
    private static String GST_KEYWORDS = "|gst|";
    private static String SL_NUMBER_KEYWORDS = "|item no.|";
    public static final String WORDS_TO_NEGLECT = ".(total).";
    public static final String NUMERIC_HEADER = "|rev hist /article#|";
    public static final String ALPHANUMERIC_ID = "|id|";


    private static final String INTEGER_REGEX = "(([^$]|^)( )\\d{0,3}[,]?\\d{1,10})|(([^$]|^)( )-?[1-9]\\d*)";
    private static final String FLOAT_REGEX = "([^$]|^)( )\\d{0,3}[,.]?\\d{1,3}[.,]\\d{2,3}";
    public static final String WORD_REGEX = "[a-zA-Z]{2,500}";
    private static final String QUANTITY_REGEX = "(" + FLOAT_REGEX + "|" + INTEGER_REGEX + ")" + "( \\w{2}\\b)?";
    private static final String UOM_REGEX = "(^| )(B15|B25|B50|Bag|BBL|BKT|BOX|BRL|BSD|CAN|Carton|CM|CDM|CG|CHN|CL|CM|CMM|CRT|CS|CTN|Ctn|CUF|CUI|CUM|CUY|DAY|DG|DL|DM|DOZ|DRA|DRM|EACH|EAC|EA|Each|FOZ|FT|G|GAL|GRP|GRS|GRT|HUN|IN|INN|Kilo|kilo|KG|KGF|KL|KM|KWH|LBS|LBT|LNK|LOT|LT|L|M|MDY|MG|MHR|MIL|ML|MM|MMO|MT|MWK|OZT|PCS|PK|PKD|PL|PTD|PTL|Punnet|PWT|QTD|QTL|SCM|SDM|SF|SHT|SHW|SLV|SM|SMM|SQF|SQI|SQM|SQY|ST|TON|TRK|Tray|TUB|UNIT|UNT|YD|CAS|Quart)($| )";
    private static String GST_REGEX = PriceValidator.PERFECT_PRICE_REGEX_INVOICE + "|" + "(?i)( |^)(gst free|fre|f re)( |$)";
    private static final String SL_NUMBER_REGEX = "\\d{1,5}";
    private static final String NUMERIC_REGEX = "( |^)\\d{1,100}";
    private static final String ALPHANUMERIC_REGEX = "( |^)([A-Za-z]+[0-9]|[0-9]+[A-Za-z])[A-Za-z0-9]{0,100}";

    static {
        fieldsRegexMap.put( DATE_KEYWORDS, DateValidator.DATE_REGEX );
        // TODO Handle prices with space instead of .
        fieldsRegexMap.put( PRICE_KEYWORDS, PriceValidator.PERFECT_PRICE_REGEX_INVOICE );
        fieldsRegexMap.put( GST_KEYWORDS, GST_REGEX );
        fieldsRegexMap.put( ALPHANUMERIC_KEYWORDS, IdValidator.INVOICE_ID_REGEX.trim() );
        fieldsRegexMap.put( UOM_KEYWORDS, UOM_REGEX );

        //Should be assigned after Price and alphanumeric
        fieldsRegexMap.put( QUANTITY_KEYWORDS, QUANTITY_REGEX );
        fieldsRegexMap.put( SL_NUMBER_KEYWORDS, SL_NUMBER_REGEX );
        fieldsRegexMap.put( NUMERIC_HEADER, NUMERIC_REGEX );
        fieldsRegexMap.put( ALPHANUMERIC_ID, ALPHANUMERIC_REGEX );

    }


    //TODO: REFACTOR THIS
    public static void validateField(String currentKey, String value, JsonObject validatedTaggedLineObj,
                                     Map<Integer, String> headerMap, List<Integer> headerIndexes, int nearestHeaderIndex, boolean checkNext, String line,
                                     String headers, String originalLine )
    {
        int indexForPrevious = headerIndexes.indexOf( nearestHeaderIndex );
        String headerVal = "";
        if ( indexForPrevious > 0 ) {
            headerVal = headerMap.get( headerIndexes.get( indexForPrevious - 1 ) );
        }
        String finalValue = "";
        boolean isChecked = false;
        List<String> fieldKeys = new ArrayList<>();
        fieldKeys.add( 0, currentKey.toLowerCase() );
        if ( currentKey.toLowerCase().split( "\\s" ).length > 1 ) {
            String[] fieldKeysArr = currentKey.toLowerCase().split( "\\s" );
            fieldKeys.addAll( Arrays.asList( fieldKeysArr ) );
        }
        for ( String fieldKey : fieldKeys ) {
            Set<String> fieldKeywordsSet = fieldsRegexMap.keySet();
            boolean checked = false;

            for ( String fieldKeywords : fieldKeywordsSet ) {
                if ( !fieldKey.isEmpty()
                        && fieldKeywords.contains( "|" + fieldKey.replace( ":", "" ).replace( ".", "" ) + "|" ) ) {
                    List<String> values = StringUtils.getMatchedTokens( " " + value, fieldsRegexMap.get( fieldKeywords ) );
                    if ( fieldKeywords.equalsIgnoreCase( ALPHANUMERIC_KEYWORDS ) && values.isEmpty() && !line.trim().isEmpty()
                            && secondLargest( line ).equalsIgnoreCase( value ) ) {
                        finalValue = value;
                    }
                    if ( values.size() > 0 ) {
                        finalValue = values.get( 0 );
                    }
                    checked = true;
                    isChecked = true;
                }
            }
            if ( checked ) {
                break;
            } else {
                //If none of the fieldName match then check for description/name...
                if ( !fieldKey.isEmpty() && WORD_KEYWORDS.contains( "|" + fieldKey.replace( ":", "" ).replace( ".", "" ) + "|" )
                        && !StringUtils.checkIfStringContainsRegexPattern( value, WORD_REGEX ) ) {
                    finalValue = "";
                    isChecked = true;
                }
            }
        }

        if ( !finalValue.isEmpty() ) {
            assignValueToField( currentKey.trim(), validatedTaggedLineObj, headers, finalValue, headerVal, originalLine,
                    nearestHeaderIndex, headerIndexes, headerMap, checkNext, line );
        } else if ( !value.isEmpty() && !isChecked ) {
            //If the field name is not present in the headerList.txt
            assignValueToField( currentKey.trim(), validatedTaggedLineObj, headers, value, headerVal, originalLine,
                    nearestHeaderIndex, headerIndexes, headerMap, checkNext, line );
            finalValue = value;
        }

        //Check if the the previous field is not set and the index of value is less than the current header index
        if ( finalValue.isEmpty() && line.indexOf( value ) < nearestHeaderIndex
                && headerIndexes.indexOf( nearestHeaderIndex ) - 1 >= 0 && null == validatedTaggedLineObj
                .get( headerMap.get( headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) - 1 ) ) )
                && checkNext ) {
            int previousNearestHeaderIndex = headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) - 1 );
            currentKey = headerMap.get( previousNearestHeaderIndex );
            validateField( currentKey, value, validatedTaggedLineObj, headerMap, headerIndexes, previousNearestHeaderIndex,
                    false, line, headers, originalLine );
        }

        //Check if the the next field is not set and the index of value is equal to of greater than than the current header index
        if ( finalValue.isEmpty() && line.indexOf( value ) >= nearestHeaderIndex
                && headerIndexes.indexOf( nearestHeaderIndex ) + 1 < headerIndexes.size() && null == validatedTaggedLineObj
                .get( headerMap.get( headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) + 1 ) ) )
                && checkNext ) {
            int nextNearestHeaderIndex = headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) + 1 );
            currentKey = headerMap.get( nextNearestHeaderIndex );
            validateField( currentKey, value, validatedTaggedLineObj, headerMap, headerIndexes, nextNearestHeaderIndex, false,
                    line, headers, originalLine );
        }

        //If partial value is set as final price, then try matching the remaining string to either next or the previous header
        if ( !finalValue.isEmpty() && !value.replaceFirst( Pattern.quote( finalValue ), "" ).trim().isEmpty()
                && headerIndexes.indexOf( nearestHeaderIndex ) + 1 < headerIndexes.size() ) {
            int nextNearestHeaderIndex;
            if ( value.indexOf( finalValue ) != 0 && headerIndexes.indexOf( nearestHeaderIndex ) > 0 ) {
                nextNearestHeaderIndex = headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) - 1 );
            } else {
                nextNearestHeaderIndex = headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) + 1 );
            }
            currentKey = headerMap.get( nextNearestHeaderIndex );
            validateField( currentKey, value.replaceFirst( Pattern.quote( finalValue ), "" ).trim(), validatedTaggedLineObj,
                    headerMap, headerIndexes, nextNearestHeaderIndex, false, line, headers, originalLine );
        }

    }


    private static String assignValueToField( String finalKey, JsonObject validatedTaggedLineObj, String header, String value,
                                              String previousKey, String originalLine, int nearestHeaderIndex, List<Integer> headerIndexes,
                                              Map<Integer, String> headerMap, boolean checkNext, String line )
    {

        if ( validatedTaggedLineObj.get( finalKey ) != null
                && !LineObjectValidator.WORD_KEYWORDS.contains( finalKey.toLowerCase() )
                && StringUtils.checkIfStringContainsMultipleKeywords( header, finalKey ) ) {
            //TODO: instead of _1 append it with _i++
            finalKey = finalKey + "_1";
            validatedTaggedLineObj.addProperty( finalKey.trim(), value.trim() );
        } else if ( validatedTaggedLineObj.get( finalKey ) != null
                && !LineObjectValidator.WORD_KEYWORDS.contains( finalKey.toLowerCase() ) ) {
            //Append the value to the existing value with a space

            if ( validatedTaggedLineObj.get( previousKey ) == null && previousKey != ""
                    && originalLine.indexOf( validatedTaggedLineObj.get( finalKey ).getAsString() ) < nearestHeaderIndex
                    && headerIndexes.indexOf( nearestHeaderIndex ) - 1 >= 0
                    && null == validatedTaggedLineObj
                    .get( headerMap.get( headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) - 1 ) ) )
                    && checkNext ) {
                int previousNearestHeaderIndex = headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) - 1 );
                String previousValue = validatedTaggedLineObj.get( finalKey ).getAsString();
                validatedTaggedLineObj.remove( finalKey );
                validateField( previousKey, previousValue, validatedTaggedLineObj, headerMap, headerIndexes,
                        previousNearestHeaderIndex, false, line, header, originalLine );
                validateField( finalKey, value, validatedTaggedLineObj, headerMap, headerIndexes, nearestHeaderIndex, false,
                        line, header, originalLine );
            } else if ( originalLine.indexOf( value ) >= nearestHeaderIndex
                    && headerIndexes.indexOf( nearestHeaderIndex ) + 1 < headerIndexes.size()
                    && null == validatedTaggedLineObj
                    .get( headerMap.get( headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) + 1 ) ) )
                    && checkNext ) {
                int nextNearestHeaderIndex = headerIndexes.get( headerIndexes.indexOf( nearestHeaderIndex ) + 1 );
                finalKey = headerMap.get( nextNearestHeaderIndex );
                validateField( finalKey, value, validatedTaggedLineObj, headerMap, headerIndexes, nextNearestHeaderIndex, false,
                        line, header, originalLine );

            } else {
                validatedTaggedLineObj.addProperty( finalKey.trim(),
                        validatedTaggedLineObj.get( finalKey ).getAsString() + " " + value.trim() );
            }
        } else if ( validatedTaggedLineObj.get( finalKey ) != null ) {
            validatedTaggedLineObj.addProperty( finalKey.replace( ".", "" ).trim(),
                    validatedTaggedLineObj.get( finalKey ).getAsString() + " " + value.trim() );
        } else {
            validatedTaggedLineObj.addProperty( finalKey.trim(), value.trim() );
        }
        return finalKey;
    }


    public static String secondLargest( String lines )
    {
        String words[] = lines.split( "\\s{2,}" );
        String secondLargest = null;
        if ( words.length != 0 ) {
            String largest = words[0];
            secondLargest = words[0];
            for ( String word : words ) {
                if ( word.length() > largest.length() ) {
                    secondLargest = largest;
                    largest = word;
                } else if ( word.length() > secondLargest.length() ) {
                    secondLargest = word;
                }
            }
        }
        return secondLargest;
    }
}
