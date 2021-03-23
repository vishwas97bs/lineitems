package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

public class CustomisedLineItem
{
    private Double lineNumber;
    private Double quantity;
    private String quantityUnit;
    private Double unitPrice;
    private String productName;
    private String productId;
    private Double finalPrice;
    private Double confidence;
    private Boolean isCoupon;
    private String rawText;

    public Double getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Double lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(Double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Boolean getCoupon() {
        return isCoupon;
    }

    public void setCoupon(Boolean coupon) {
        isCoupon = coupon;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
