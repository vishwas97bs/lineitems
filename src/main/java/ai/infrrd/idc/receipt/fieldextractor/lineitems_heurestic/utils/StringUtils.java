package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.commons.datastructures.Tuple2;
import ai.infrrd.idc.utils.entity.RegexMatchInfo;
import ai.infrrd.idc.utils.extractors.PatternExtractor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StringUtils {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( StringUtils.class );


    /**
     * Returns the matched tokens in the string according to the regex
     *
     * @param input Input String
     * @param regex Regex to be matched
     * @return List of Matched tokens from the input String
     */
    public static List<String> getMatchedTokens(String input, String regex )
    {
        List<String> tokens = new ArrayList<>();
        PatternExtractor patternExtractor = new PatternExtractor( regex );


        List<RegexMatchInfo> patterRegexMatchInfoList = patternExtractor.matchedPatterns( input );
        for ( RegexMatchInfo regexMatchInfo : patterRegexMatchInfoList ) {
            tokens.add( regexMatchInfo.getMatchedString().trim() );
        }
        return tokens;
    }


    /**
     * Returns the matched tokens in the string according to the regex for Quantity
     *
     * @param input Input String
     * @param regex Regex to be matched
     * @return List of Matched tokens from the input String
     */
    public static List<String> getMatchedTokensForQuantity( String input, String regex )
    {
        List<String> tokens = new ArrayList<>();

        PatternExtractor patternExtractor = new PatternExtractor( regex );
        List<RegexMatchInfo> regexMatchInfoList = patternExtractor.matchedPatterns( input );
        for ( RegexMatchInfo regexMatchInfo : regexMatchInfoList ) {
            tokens.add( regexMatchInfo.getMatchedString() );
        }
        return tokens;
    }


    /**
     * Returns the List if strings which match the regex in the group mentioned
     *
     * @param input Input string
     * @param regex Regex to match
     * @param groupIndex The index of the group in the regex to be matched
     * @return Strings matched which to the specified regex group
     */
    public static List<String> getMatchedTokensFromSpecifiedRegexGroup( String input, String regex, int groupIndex )
    {
        List<String> tokens = new ArrayList<>();
        PatternExtractor patternExtractor1 = new PatternExtractor( regex );
        List<RegexMatchInfo> regexMatchInfoList = patternExtractor1.matchedPatterns( input );
        for ( RegexMatchInfo regexMatchInfo : regexMatchInfoList ) {
            PatternExtractor patternExtractor = new PatternExtractor( regex );
            if ( patternExtractor.matchedPatterns( regexMatchInfo.getMatchedString() ).get( 0 ).getGroupCount() >= groupIndex
                    && null != patternExtractor.matchedPatterns( regexMatchInfo.getMatchedString(), groupIndex )
                    && !patternExtractor.matchedPatterns( regexMatchInfo.getMatchedString(), groupIndex ).get( 0 )
                    .getMatchedString().trim().isEmpty() ) {
                tokens.add( patternExtractor.matchedPatterns( regexMatchInfo.getMatchedString(), groupIndex ).get( 0 )
                        .getMatchedString().trim() );
            }

        }
        return tokens;
    }


    /**
     * Checks if the given string has words matching the specified regex pattern
     *
     * @param input Input string to check for regex pattern
     * @param regex The regex pattern to be checked
     * @return true is there is a regex pattern match else false
     */
    public static boolean checkIfStringContainsRegexPattern( String input, String regex )
    {

        PatternExtractor patternExtractor = new PatternExtractor( regex );
        return patternExtractor.isMatchedPatterns( input );
    }


    /**
     * Checks if given input matches to any of the key of the map (regex match)
     * @return String key of map by matching input
     */
    public static String getKeyFromMapByMatchingRegex(Map<String, Object> inputMap, String input )
    {
        return inputMap.keySet().stream().filter( m -> StringUtils.checkIfStringContainsRegexPattern( input,
                Constants.WORD_START_REGEX + m + Constants.WORD_END_REGEX ) ).findAny().orElse( "" );
    }


    /**
     * Replaces multiple spaces in between words with a single space
     *
     * @param input Input String
     * @return String with single space between words
     */
    public static String replaceMultipleSpacesBySingleSpace( String input )
    {
        input = input.replaceAll( "\\s+", " " );
        return input;
    }


    /**
     * This method accepts an array of strings and returns the string with the max length of alphabets after removing the numbers in it
     * @param strings an array of strings
     * @return the string with the max length
     */
    public static String getLongestAlphabetString( List<String> strings )
    {
        String maxLengthString = "";
        for ( String string : strings ) {
            if ( string.replaceAll( "\\d", "" ).length() > maxLengthString.replaceAll( "\\d", "" ).length() ) {
                maxLengthString = string;
            }
        }
        return maxLengthString;
    }


    /**
     * Removes the words in the list from the input string by replacing those words with empty spaces
     * @param inputString Input String
     * @param pattern Word to be replaces from input string
     * @return replaced string
     */
    public static String replacePatternWithSpaces( String inputString, String pattern )
    {
        for ( String patternToken : pattern.split( "\\s" ) ) {
            int length = patternToken.length();
            StringBuilder spaces = new StringBuilder();
            for ( int space = 0; space < length; space++ ) {
                spaces.append( " " );
            }
            inputString = inputString.replaceFirst( escapeMetaCharactersForRegex( patternToken ), spaces.toString() );
        }

        return inputString;
    }


    /**
     * Removes the words in the list from the input string by replacing the specified word with empty spaces
     * @param inputString Input String
     * @param stringToReplaceWithSpace Word to be replaces from input string
     * @return replaced string
     */
    public static String replaceStringWithSpace( String inputString, String stringToReplaceWithSpace )
    {
        int length = stringToReplaceWithSpace.length();
        StringBuilder spaces = new StringBuilder();
        for ( int space = 0; space < length; space++ ) {
            spaces.append( " " );
        }
        inputString = inputString.replaceFirst( escapeMetaCharactersForRegex( stringToReplaceWithSpace ), spaces.toString() );


        return inputString;
    }


    /**
     * escape all special charectors
     * @param inputString
     * @return
     */
    public static String escapeMetaCharactersForRegex( String inputString )
    {
        final String[] metaCharacters = { "\\", "^", "$", "{", "}", "[", "]", "(", ")", ".", "*", "+", "?", "|", "<", ">", "-",
                "&" };
        String outputString = inputString;
        for ( int i = 0; i < metaCharacters.length; i++ ) {
            if ( inputString.contains( metaCharacters[i] ) ) {
                outputString = inputString.replace( metaCharacters[i], "\\" + metaCharacters[i] );
                inputString = outputString;
            }
        }
        return outputString;
    }


    public static String getNameFromSpecifiedIndex( String inputLine, String[] lineElements, Integer integer )
    {
        String name = null;
        int threshold = 3;
        for ( String word : lineElements ) {
            if ( integer - threshold < inputLine.indexOf( word ) && inputLine.indexOf( word ) < integer + threshold ) {
                name = word;
            }
        }
        return name;

    }


    /**
     * Use this in place of Arrays.asList(). Because for operations that involve removing elements recursively,
     * from the list created using Arrays.asList(), it throws Concurrent operation or unsupported operation exception
     * @param strArray
     * @return
     */
    public static List<String> convertStringArrayToList( String[] strArray )
    {
        LOG.trace( "Method: convertStringArrayToList called." );
        List<String> strList = new ArrayList<>();
        for ( String string : strArray ) {
            strList.add( string );
        }
        LOG.trace( "Method: convertStringArrayToList finished." );
        return strList;
    }


    public static boolean checkIfStringContainsMultipleKeywords( String originalExtractedText, String keyword )
    {
        boolean multipleKeywords = false;
        keyword = keyword.toLowerCase();
        if ( originalExtractedText.toLowerCase().replaceFirst( keyword, "" ).contains( keyword ) ) {
            multipleKeywords = true;
        }
        return multipleKeywords;
    }


    public static String replaceGivenStringWithSpaces( String inputLine, String probableName )
    {
        int length = probableName.length();
        StringBuilder spaces = new StringBuilder();
        for ( int space = 0; space < length; space++ ) {
            spaces.append( " " );
        }
        inputLine = inputLine.replaceFirst( escapeMetaCharactersForRegex( probableName ), spaces.toString() );


        return inputLine;

    }


    public static int checkNumberOfSpaces( String headerStr )
    {
        headerStr = headerStr.trim();
        int spaces = headerStr.length() - headerStr.replace( " ", "" ).length();
        return spaces;
    }


    /*This method remove space characters from the input string*/
    public static String removeSpaceFromText( String input )
    {
        if ( null != input && !input.isEmpty() ) {
            input = input.replace( " ", "" );
        }
        return input;
    }


    /*this method replaces more than one space with single space in any given string*/
    public static String replaceMultipleSpaces( String data )
    {
        if ( null != data && !data.isEmpty() ) {
            data = data.replaceAll( " {1,}", " " );
        }
        return data;
    }


    public static String replaceMultipleSpacesBySingleSpaceEscapingNextLine( String originalExtractedText )
    {
        return originalExtractedText.replaceAll( "\\r\\n", "@#@#" ).replaceAll( "\\s{2}", " " ).replaceAll( "\\s{3,}", "  " )
                .replaceAll( "@#@#", "\r\n" );
    }


    public static String replaceRegexMatchesWithSpaces( String inputLine, String regex )
    {
        List<String> matchedTokens = getMatchedTokens( inputLine, regex );
        if ( !matchedTokens.isEmpty() ) {
            for ( String matchedToken : matchedTokens ) {
                inputLine = replaceGivenStringWithSpaces( inputLine, matchedToken );
            }
        }
        return inputLine;
    }


    public static List<Integer> getMatchedTokensWithIndex( String input, String regex )
    {
        List<Integer> tokens = new ArrayList<Integer>();
        PatternExtractor patternExtractor = new PatternExtractor( regex );
        List<RegexMatchInfo> regexMatchInfoList = patternExtractor.matchedPatterns( input );
        for ( RegexMatchInfo regexMatchInfo : regexMatchInfoList ) {
            tokens.add( regexMatchInfo.getStartindex() );
        }
        return tokens;
    }


    public static String replaceRegexMatchesWithEmpty( String inputLine, String regex )
    {
        List<String> matchedTokens = getMatchedTokens( inputLine, regex );
        if ( !matchedTokens.isEmpty() ) {
            for ( String matchedToken : matchedTokens ) {
                inputLine = inputLine.replace( matchedToken, "" );
            }
        }
        return inputLine;
    }


    /**
     * returns the list of matched values along with their index
     * @param input
     * @param regex
     * @return
     */
    public static List<Tuple2<String, Integer>> getMatchedTokensWithTheirIndex(String input, String regex )
    {
        List<Tuple2<String, Integer>> tokens = new ArrayList<>();
        PatternExtractor patternExtractor = new PatternExtractor( regex );
        List<RegexMatchInfo> patterRegexMatchInfoList = patternExtractor.matchedPatterns( input );
        for ( RegexMatchInfo regexMatchInfo : patterRegexMatchInfoList ) {
            tokens.add( new Tuple2<>( regexMatchInfo.getMatchedString(), regexMatchInfo.getStartindex() ) );
        }
        return tokens;
    }


    /**
     * provides a string consisting of spaces with same length of count provided.
     * @param count
     * @return
     */
    public static String getSpacesFromInputCount( int count )
    {
        if ( count < 1 ) {
            return "";
        }
        StringBuilder spaceStringBuilder = new StringBuilder();
        for ( ; count > 0; count-- ) {
            spaceStringBuilder.append( " " );
        }
        return spaceStringBuilder.toString();
    }

}
