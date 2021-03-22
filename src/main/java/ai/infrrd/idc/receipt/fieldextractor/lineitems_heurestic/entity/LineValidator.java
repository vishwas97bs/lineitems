package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

public class LineValidator {

    private String lineStr;
    private boolean isValidLine;
    private String remainingLineString;
    private int fieldIndex;
    private boolean isLineToReject;
    private boolean isOptional;
    private String rawLineString;
    private int docWidth;


    public LineValidator( String line )
    {
        this.lineStr = line;
        this.remainingLineString = line;
        this.isValidLine = true;
        this.rawLineString = "";
    }


    public LineValidator( String line, String rawLineString, int docWidth )
    {
        this.lineStr = line;
        this.remainingLineString = line;
        this.isValidLine = true;
        this.rawLineString = rawLineString;
        this.docWidth = docWidth;
    }


    public int getDocWidth()
    {
        return docWidth;
    }


    public void setDocWidth( int docWidth )
    {
        this.docWidth = docWidth;
    }


    public String getRawLineString()
    {
        return rawLineString;
    }


    public void setRawLineString( String rawLineString )
    {
        this.rawLineString = rawLineString;
    }


    public boolean isLineToReject()
    {
        return isLineToReject;
    }


    public void setLineToReject( boolean lineToReject )
    {
        isLineToReject = lineToReject;
    }


    public int getFieldIndex()
    {
        return fieldIndex;
    }


    public void setFieldIndex( int setFieldIndex )
    {
        this.fieldIndex = setFieldIndex;
    }


    public String getLineStr()
    {
        return lineStr;
    }


    public void setLineStr( String lineStr )
    {
        this.lineStr = lineStr;
    }


    public boolean isValidLine()
    {
        return isValidLine;
    }


    public void setValidLine( boolean validLine )
    {
        isValidLine = validLine;
    }


    public String getRemainingLineString()
    {
        return remainingLineString;
    }


    public void setRemainingLineString( String remainingLineString )
    {
        this.remainingLineString = remainingLineString;
    }


    public boolean isOptional()
    {
        return isOptional;
    }


    public void setOptional( boolean optional )
    {
        isOptional = optional;
    }

}
