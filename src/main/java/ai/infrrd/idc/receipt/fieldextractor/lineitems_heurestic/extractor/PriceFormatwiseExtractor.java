package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.extractor;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Constants;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.PriceValidator;
import ai.infrrd.idc.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

public class PriceFormatwiseExtractor {

    private static final String PERFECT_PRICE_REGEX = "(" + PriceValidator.PERFECT_PRICE_REGEX + ")";
    private static final String PRICE_WITH_SPACES_REGEX = "(" + PriceValidator.PRICE_WITH_SPACES_REGEX + ")";
    private static final String PERFECT_PRICE_REGEX_INVOICE = "(" + PriceValidator.PERFECT_PRICE_REGEX_INVOICE + ")";
    private static final String PRICE_WITH_SPACES_REGEX_INVOICE = "(" + PriceValidator.PRICE_WITH_SPACES_REGEX_INVOICE + ")";

    private static final String PRICE_REGEX = "(" + PERFECT_PRICE_REGEX + Constants.REGEX_SEPARATOR + PRICE_WITH_SPACES_REGEX
            + Constants.REGEX_SEPARATOR + PERFECT_PRICE_REGEX_INVOICE + Constants.REGEX_SEPARATOR + PRICE_WITH_SPACES_REGEX_INVOICE
            + ")";


    public int getCountOfLinesWithPrice( String text )
    {
        String ocrText = text.toLowerCase();
        String[] lines = (String[]) getStringLines( ocrText ).toArray();
        int linesCount = 0;
        for ( String line : lines ) {
            if ( StringUtils.checkIfStringContainsRegexPattern( line, PRICE_REGEX ) )
                linesCount++;
        }
        return linesCount;
    }

    /**
     * Splits a text block into lines
     *
     * @param text Text to split
     * @return List of lines from the text
     */
    public static List<String> getStringLines(String text )
    {
        return Arrays.asList( text.split( "\n" ) );
    }


}
