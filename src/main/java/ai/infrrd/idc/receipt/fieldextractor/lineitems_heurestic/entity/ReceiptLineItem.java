package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

import org.apache.commons.text.StringEscapeUtils;

public class ReceiptLineItem  extends LineItem{
    private double quantity = 0.0;
    private String category;
    private String quantityUnit;
    private Double unitPrice;


    public ReceiptLineItem getCopy()
    {
        ReceiptLineItem copy = new ReceiptLineItem();
        copy.setLineNumber( getLineNumber() );
        copy.setProductName( getProductName() );
        copy.setFinalPrice( getFinalPrice() );
        copy.setProductId( getProductId() );
        copy.setPrices( getPrices() );
        copy.setLineConfidence( getLineConfidence() );
        copy.setAverageOfFieldConfidence( getAverageOfFieldConfidence() );
        copy.setLineSyntaxMatchConfidence( getLineSyntaxMatchConfidence() );
        copy.setAccurate( isAccurate() );
        copy.setCoupon( isCoupon() );
        copy.setFieldConfidences( getFieldConfidences() );
        copy.setNonOptionalFieldsCount( getNonOptionalFieldsCount() );
        copy.setDiscountType( getDiscountType() );
        copy.setRawText( getRawText() );
        copy.setQuantity( getQuantity() );
        copy.setCategory( getCategory() );
        copy.setQuantityUnit( getQuantityUnit() );
        copy.setUnitPrice( getUnitPrice() );
        copy.setStartX( getStartX() );
        copy.setStartY( getStartY() );
        copy.setEndX( getEndX() );
        copy.setEndY( getEndY() );
        return copy;
    }


    public String getCategory()
    {
        return category;
    }


    public void setCategory( String category )
    {
        this.category = category;
    }


    public double getQuantity()
    {
        return quantity;
    }


    public void setQuantity( double quantity )
    {
        this.quantity = quantity;
    }


    public String getQuantityUnit()
    {
        return quantityUnit;
    }


    public void setQuantityUnit( String quantityUnit )
    {
        this.quantityUnit = quantityUnit;
    }


    public Double getUnitPrice()
    {
        return unitPrice;
    }


    public void setUnitPrice( Double unitPrice )
    {
        this.unitPrice = unitPrice;
    }


    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( "\"lineNumber\" : " + this.getLineNumber() );
        if ( builder.length() != 1 ) {
            builder.append( "," );
        }
        builder.append( "\"quantity\" :" + String.format( "%.3f", this.getQuantity() ) );
        if ( this.getCategory() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"category\" : \"" + this.getCategory() + "\"" );
        }
        if ( this.getQuantityUnit() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"quantityUnit\" : \"" + this.getQuantityUnit() + "\"" );
        }
        if ( this.getQuantityUnit() == null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"quantityUnit\" : " + this.getQuantityUnit() );
        }
        if ( builder.length() != 1 ) {
            builder.append( "," );
        }
        if ( this.getUnitPrice() != null ) {
            builder.append( "\"unitPrice\" : " + String.format( "%.2f", this.getUnitPrice() ) );
        }
        if ( this.getUnitPrice() == null ) {
            builder.append( "\"unitPrice\" : " + this.getUnitPrice() );
        }
        if ( this.getProductName() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"productName\" : \"" + this.getProductName().replace( "\"", "'" ) + "\"" );
        }
        if ( this.getProductId() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"productId\" : \"" + this.getProductId() + "\"" );
        }
        if ( this.getProductId() == null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"productId\" : " + this.getProductId() );
        }
        if ( builder.length() != 1 ) {
            builder.append( "," );
        }
        builder.append( " \"finalPrice\" : " + this.getFinalPrice() );


        if ( builder.length() != 1 ) {
            builder.append( "," );
        }
        builder.append( " \"confidence\" : " + String.format( "%.2f", this.getLineConfidence() ) );
        if ( builder.length() != 1 ) {
            builder.append( "," );
        }
        builder.append( " \"isCoupon\" : " + this.getIsCoupon() );
        if ( this.getIsCoupon() ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"discountType\" : \"" + this.getDiscountType() + "\"" );
        }
        if ( this.getRawText() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"rawText\" : \"" + StringEscapeUtils.escapeJava( this.getRawText() ) + "\"" );
        }
        if ( this.getRawText() == null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"rawText\" : " + this.getRawText() );
        }

        builder.append( "}" );
        return builder.toString();
    }
}
