package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

public class FieldConfiguration
{

    private boolean absConfidence;

    private boolean text;

    private String customerId;

    public boolean isAbsConfidence()
    {
        return absConfidence;
    }


    public void setAbsConfidence( boolean absConfidence )
    {
        this.absConfidence = absConfidence;
    }


    public boolean isText()
    {
        return text;
    }


    public void setText( boolean text )
    {
        this.text = text;
    }


    public String getCustomerId()
    {
        return customerId;
    }


    public void setCustomerId( String customerId )
    {
        this.customerId = customerId;
    }
}
