package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.ReceiptLineItem;

public class ConfidenceValueHelper {
    public static final float CONFIDENCE_THRESHOLD = 0.85f;


    public static float getFinalConfidence( ReceiptLineItem receiptLineItem, boolean isAbsoluteConfidence )
    {
        float conf = receiptLineItem.getLineConfidence();
        if ( !checkPriceForConfidence( receiptLineItem ) ) {
            conf -= 0.1;
        }
        if ( !isAbsoluteConfidence ) {
            conf = ( conf > CONFIDENCE_THRESHOLD ) ? 1.0f : 0.0f;
        }
        return conf;
    }


    private static boolean checkPriceForConfidence( ReceiptLineItem receiptLineItem )
    {
        if ( receiptLineItem.getUnitPrice() != null ) {
            double quantity = receiptLineItem.getQuantity();
            double unitPrice = receiptLineItem.getUnitPrice();
            double price = receiptLineItem.getFinalPrice();
            double calcPrice = quantity * unitPrice;
            if ( Math.abs( price - calcPrice ) > 0.1 ) {
                return false;
            }
        }
        return true;
    }
}
