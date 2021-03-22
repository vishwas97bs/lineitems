package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


public class ConfidenceCalculator
{
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( ConfidenceCalculator.class );

    private float syntaxConfidence;
    private List<Float> linesConfidence;
    private float lineCountConfidence;
    private float matchedSyntaxLinesConfidence;
    private float totalValueConfidence;

    public static final float BASE_CONFIDENCE_VALUE = 1.0f;
    public static final float MINIMUM_CONF_DIFF = 0.4f;
    public static final float LINE_SYNTAX_CONFIDENCE_WEIGHTAGE = 0.0f;
    public static final float LINE_AVG_FIELDS_CONFIDENCE_WEIGHTAGE = 0.4f;
    public static final float LINE_SYNTAX_MATCH_CONFIDENCE_WEIGHTAGE = 0.6f;
    public static final float LINES_COUNT_CONFIDENCE_WEIGHT = 0.4f;
    public static final float LINES_SYNTAX_MATCH_COUNT_CONFINENCE_WEIGHT = 0.4f;
    public static final float AVERAGE_LINES_CONFINEDCE_WEIGHT = 0.2f;

    private Utils utils;

    @Autowired
    public void setUtils(Utils utils) {
        this.utils = utils;
    }

    public float getSyntaxConfidence()
    {
        return syntaxConfidence;
    }


    public void setSyntaxConfidence( float syntaxConfidence )
    {
        this.syntaxConfidence = syntaxConfidence;
    }


    public List<Float> getLinesConfidence()
    {
        return linesConfidence;
    }


    public void addToLinesConfidence( float lineConfidence )
    {
        if ( this.linesConfidence == null ) {
            this.linesConfidence = new ArrayList<>();
        }
        try {
            this.linesConfidence.add( utils.roundUptoNDecimals( lineConfidence, 2 ) );
        } catch ( NumberFormatException ex ) {
            LOG.error( "lineConfidence has format issues", ex );
        }
    }


    public float getLineCountConfidence()
    {
        return lineCountConfidence;
    }


    public void setLineCountConfidence( float lineCountConfidence )
    {
        this.lineCountConfidence = lineCountConfidence;
    }


    public float getMatchedSyntaxLinesConfidence()
    {
        return matchedSyntaxLinesConfidence;
    }


    public void setMatchedSyntaxLinesConfidence( float matchedSyntaxLinesConfidence )
    {
        this.matchedSyntaxLinesConfidence = matchedSyntaxLinesConfidence;
    }


    public float getTotalValueConfidence()
    {
        return totalValueConfidence;
    }


    public void setTotalValueConfidence( float totalValueConfidence )
    {
        this.totalValueConfidence = totalValueConfidence;
    }

}
