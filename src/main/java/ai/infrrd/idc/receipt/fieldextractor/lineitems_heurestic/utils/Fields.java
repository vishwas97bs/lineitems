package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

public enum Fields {
    MERCHANT_NAME( "merchantname" ),
    BILLING_DATE( "billingdate" ),
    TOTAL_BILL_AMOUNT( "totalbillamount" ),
    CURRENCY( "currency" ),
    CATEGORY( "category" ),
    LOCALE( "locale" ),
    VAT_NUMBER( "vatnumber" ),
    BASKET_DISCOUNT_LIST( "basketDiscountList" ),
    TENDER_DETAIL_LIST( "tenderDetailList" ),
    TAX_DETAIL_LIST( "taxDetailList" );

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
