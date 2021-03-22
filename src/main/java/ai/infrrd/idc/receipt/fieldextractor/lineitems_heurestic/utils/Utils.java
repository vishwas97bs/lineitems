package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.ConfigService;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class Utils
{

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( Utils.class );
    private static final int indexToLookUpto = 5;
    private static final int linesToLookUpto = 4;

    private ConfigService configService;

    @Autowired
    public void setConfigService( ConfigService configService )
    {
        this.configService = configService;
    }


    /**
     * This method is used to round up a float number according to 'n' decimal points
     * @param number - Float number to be rounded off
     * @param n - Number of decimal points
     * @return - The rounded off number
     */
    public float roundUptoNDecimals( float number, int n )
    {
        float multiplier = (float) Math.pow( 10, n );
        return Math.round( number * multiplier ) / ( multiplier );
    }


    /**
     * This method gets the various fields from the document starting from the same line where the keyword is found
     * @param responseForOtherFields - Json object which stores all the extracted fields of the document
     * @param extractedText - Text from ocr extraction
     * @param lines - Array of string containing all the lines
     * @param keywords - Array of string containing the keywords to be found for a field
     * @param lineNo - Line number where the keyword is found
     * @param key - Name of the field to be found
     * @param regex - Regex of the field to be validated
     * @return - String containing the field which is found
     */
    public String getFieldFromLineStartingFromSameLine( JsonObject responseForOtherFields, String extractedText, String[] lines,
        String[] keywords, int lineNo, String key, String regex )
    {
        String field = null;
        for ( String keyword : keywords ) {
            if ( !extractedText.toLowerCase().contains( keyword ) ) {
                continue;
            }
            int indexOfKeyword = lines[lineNo].toLowerCase().indexOf( keyword );
            if ( indexOfKeyword != -1 ) {
                List<String> linesToLook = new ArrayList<>();
                for ( int i = 0; i < linesToLookUpto; i++ ) {
                    if ( lineNo + i < lines.length ) {
                        linesToLook.add( lines[lineNo + i] );
                    }
                }
                field = findValueOfKeywordInVicinity( regex, indexOfKeyword, linesToLook, keyword );
                if ( field != null ) {
                    responseForOtherFields.addProperty( "name", key );
                    responseForOtherFields.addProperty( "values", field );
                    LOG.info( "{} : {}", key, field );
                    break;
                }
            }
        }
        return field;
    }


    /**
     * Gets the extracted merchant name
     * @param extractedData
     * @return
     */
    public String getMerchantName( Map<String, Object> extractedData )
    {
        String merchantName = null;
        Map<String, Object> merchantObj = (Map<String, Object>) extractedData.get( Fields.MERCHANT_NAME.getValue() );
        if ( merchantObj != null ) {
            merchantName = (String) merchantObj.get( "value" );
        }
        return merchantName;
    }


    /**
     * This method finds the field to be extracted around the vicinity of the keyword found
     * @param regex - Rules for matching the field
     * @param indexOfKeyword - Index at which keyword corresponding to the field is found
     * @param lines - List of string of the lines around the vicinity
     * @param keyword
     * @return The String containing the field found
     */

    private String findValueOfKeywordInVicinity( String regex, int indexOfKeyword, List<String> lines, String keyword )
    {
        LOG.trace( "Method: findValueOfKeywordInVicinity called." );
        List<String> matchedValues = new ArrayList<>();
        try {
            String firstAdjacentString = lines.remove( 0 ).substring( indexOfKeyword + keyword.length() );
            if ( firstAdjacentString.charAt( 0 ) == ' ' ) {
                firstAdjacentString = firstAdjacentString.substring( firstAdjacentString.indexOf( ' ' ) ).trim();
            }
            matchedValues = StringUtils.getMatchedTokens( firstAdjacentString, regex );
            if ( matchedValues.isEmpty() || firstAdjacentString
                .substring( matchedValues.get( 0 ).length(), firstAdjacentString.indexOf( "  " ) ).contains( ":" ) ) {
                for ( String line : lines ) {
                    String partialStringPresence = line
                        .substring( indexOfKeyword - indexToLookUpto > 0 ? indexOfKeyword - indexToLookUpto : 0 );
                    if ( partialStringPresence.replaceAll( "\\s", "" ).isEmpty() ) {
                        line = line.replaceAll( "\\s{2,}", " " ).trim();
                        partialStringPresence = line.substring( line.lastIndexOf( " " ) != -1 ? line.lastIndexOf( " " ) : 0 );
                    }
                    matchedValues = StringUtils.getMatchedTokens( partialStringPresence, regex );
                    if ( !matchedValues.isEmpty() ) {
                        break;
                    }
                }
            }

            LOG.trace( "Method: findValueOfKeywordInVicinity finished." );
        } catch ( Exception ex ) {
            LOG.error( "Error while finding Value Of Keyword In Vicinity", ex );
        }
        return matchedValues.isEmpty() ? null : matchedValues.get( 0 );
    }


    public int getNearestHeaderIndex( int valueStartIndex, List<Integer> keySet, Map<Integer, String> mainHeaderMap,
        String lineFieldValue, String previousKey )
    {
        int nearestIndex = -1;
        int minDifference = Integer.MAX_VALUE;
        int valueEndIndex = valueStartIndex + lineFieldValue.length();

        if ( keySet.contains( valueStartIndex ) ) {
            nearestIndex = valueStartIndex;
        } else {
            nearestIndex = checkIfValueIsUnderHeader( valueStartIndex, keySet, mainHeaderMap, lineFieldValue );
        }

        if ( nearestIndex == -1 ) {
            for ( int headerIndex : keySet ) {
                int headerStartIndexDiff = Math.abs( valueStartIndex - headerIndex );
                if ( headerStartIndexDiff <= minDifference ) {
                    nearestIndex = headerIndex;
                    minDifference = headerStartIndexDiff;
                }
                int headerEndIndexDiff = Math
                    .abs( valueStartIndex - ( headerIndex + mainHeaderMap.get( headerIndex ).length() ) );
                if ( headerEndIndexDiff <= minDifference ) {
                    nearestIndex = headerIndex;
                    minDifference = headerEndIndexDiff;
                }
                int valueEndIndexDiff = Math.abs( headerIndex - valueEndIndex );
                if ( valueEndIndexDiff < minDifference ) {
                    nearestIndex = headerIndex;
                    minDifference = valueEndIndexDiff;
                }
            }
        }
        if ( !( valueStartIndex >= nearestIndex && valueStartIndex <= nearestIndex + lineFieldValue.length() )
            && lineFieldValue.equals( previousKey ) && valueStartIndex > nearestIndex
            && keySet.indexOf( nearestIndex ) + 1 < keySet.size() ) {
            nearestIndex = keySet.get( keySet.indexOf( nearestIndex ) + 1 );
        }
        return nearestIndex;
    }


    private int checkIfValueIsUnderHeader( int valueStartIndex, List<Integer> keySet, Map<Integer, String> mainHeaderMap,
        String lineFieldValue )
    {
        int nearestHeader = -1;
        for ( int headerIndex : keySet ) {
            if ( headerIndex > valueStartIndex
                && headerIndex + mainHeaderMap.get( headerIndex ).length() < valueStartIndex + lineFieldValue.length() ) {
                nearestHeader = headerIndex;
            }
            if ( headerIndex < valueStartIndex && headerIndex + mainHeaderMap.get( headerIndex ).length() > valueStartIndex ) {
                nearestHeader = headerIndex;
            }
        }
        return nearestHeader;
    }


    public float getAverageFromListValues( List<Float> listOfFloatValues )
    {
        float average = 0.0f;
        float sumOfValues = 0.0f;
        for ( float value : listOfFloatValues ) {
            sumOfValues = sumOfValues + value;
        }
        if ( listOfFloatValues.size() > 0 ) {
            average = (float) round( ( sumOfValues / listOfFloatValues.size() ), 2 );
        }
        return average;
    }


    public String removeAmount( String text, String value )
    {
        text = StringUtils.replaceRegexMatchesWithSpaces( text, PriceValidator.PERFECT_PRICE_REGEX );
        text = StringUtils.replaceRegexMatchesWithSpaces( text, PriceValidator.PRICE_WITH_SPACES_REGEX );
        return text;
    }


    public List<String> getExtraFieldsToPickInNextLine( String merchantName, Map<String, Object> configuration )
    {
        List<String> fieldList = new ArrayList<>();
        List<String> merchantList = configService.getValueList( "lineitems_merchants_with_extra_info_in_next_line",
            configuration );
        for ( String listItem : merchantList ) {
            String merchantNameInConfig = listItem.substring( 0,
                ( listItem.contains( ":" ) ) ? listItem.indexOf( ":" ) : listItem.length() );
            if ( StringUtils.checkIfStringContainsRegexPattern( merchantName,
                Constants.WORD_START_REGEX + merchantNameInConfig + Constants.WORD_END_REGEX ) ) {
                //split headers by ';;' in config
                if ( listItem.contains( ":" ) ) {
                    fieldList = Arrays.asList( listItem.substring( listItem.indexOf( ":" ) + 1 ).trim().split( ";;" ) );
                    LOG.debug( "{} extraLine Headers found for merchantName: {}", fieldList.size(), merchantName );
                }
                break;
            }
        }
        return fieldList;
    }


    public double round( double value, int places )
    {
        if ( places < 0 )
            throw new IllegalArgumentException();

        long factor = (long) Math.pow( 10, places );
        value = value * factor;
        long tmp = Math.round( value );
        return (double) tmp / factor;
    }
}
