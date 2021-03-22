package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

public enum Fields {
    MERCHANT_NAME( "merchantname" );

    private String value;


    private Fields( String value )
    {
        this.value = value;
    }


    public String getValue()
    {
        return this.value;
    }
}
