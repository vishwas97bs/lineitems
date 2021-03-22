package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import java.util.Arrays;
import java.util.List;

public class Util {
    /**
     * Splits a text block into lines
     *
     * @param text Text to split
     * @return List of lines from the text
     */
    public List<String> getStringLines(String text )
    {
        return Arrays.asList( text.split( "\n" ) );
    }


    public static <T> T getOrDefault( T property, T defaultVal )
    {
        return property != null ? property : defaultVal;
    }
}
