package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions.LineExtractionException;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Utils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LineItem
{
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( LineItem.class );

    private int lineNumber;
    private String productName;
    private Float finalPrice = null;
    private String productId;
    private List<Float> prices = new LinkedList<>();
    private float lineConfidence;
    private float averageOfFieldConfidence;
    private float lineSyntaxMatchConfidence;
    private boolean isAccurate = true;
    private boolean isCoupon = false;
    private List<Float> fieldConfidences = new ArrayList<>();
    private float nonOptionalFieldsCount;
    private String discountType;
    private String rawText;
    private int startX;
    private int startY;
    private int endX;
    private int endY;

    private Utils utils = new Utils();

    public int getStartX()
    {
        return startX;
    }


    public void setStartX( int startX )
    {
        this.startX = startX;
    }


    public int getStartY()
    {
        return startY;
    }


    public void setStartY( int startY )
    {
        this.startY = startY;
    }


    public int getEndX()
    {
        return endX;
    }


    public void setEndX( int endX )
    {
        this.endX = endX;
    }


    public int getEndY()
    {
        return endY;
    }


    public void setEndY( int endY )
    {
        this.endY = endY;
    }


    public List<Float> getFieldConfidences()
    {
        return fieldConfidences;
    }


    public LineItem getCopy()
    {
        LineItem copy = new LineItem();
        copy.lineNumber = lineNumber;
        copy.productName = productName;
        copy.setFinalPrice( finalPrice );
        copy.productId = productId;
        copy.setPrices( prices );
        copy.lineConfidence = lineConfidence;
        copy.averageOfFieldConfidence = averageOfFieldConfidence;
        copy.lineSyntaxMatchConfidence = lineSyntaxMatchConfidence;
        copy.isAccurate = isAccurate;
        copy.setCoupon( isCoupon );
        copy.setFieldConfidences( fieldConfidences );
        copy.nonOptionalFieldsCount = nonOptionalFieldsCount;
        copy.discountType = discountType;
        copy.rawText = rawText;
        return copy;
    }


    public void addToFieldConfidenceList( Float fieldConfidence ) throws LineExtractionException
    {
        if ( this.getFieldConfidences() == null ) {
            this.setFieldConfidences( new ArrayList<Float>() );
        }
        try {
            this.getFieldConfidences().add( utils.roundUptoNDecimals( fieldConfidence, 2 ) );
        } catch ( NumberFormatException ex ) {
            LOG.error( "fieldConfidences has format issues", ex );
            throw new LineExtractionException( "fieldConfidences has format issues" );
        }
    }


    public float getLineConfidence()
    {
        return lineConfidence;
    }


    public void setLineConfidence( float lineConfidence )
    {
        this.lineConfidence = lineConfidence;
    }


    public int getLineNumber()
    {
        return lineNumber;
    }


    public void setLineNumber( int lineNumber )
    {
        this.lineNumber = lineNumber;
    }


    public List<Float> getPrices()
    {
        return prices;
    }


    public void addToPriceList( String price ) throws LineExtractionException
    {
        if ( this.getPrices() == null ) {
            this.setPrices( new ArrayList<Float>() );
        }
        try {
            this.getPrices().add( utils.roundUptoNDecimals( Float.parseFloat( price ), 2 ) );
        } catch ( NumberFormatException ex ) {
            LOG.error( "Price has format issues", ex );
            throw new LineExtractionException( "Price has format issues" );
        }
    }


    /**
     * Line's validity is returned by this method
     * @return boolean that tells whether the line is valid
     */
    public boolean isLineObjectValid()
    {
        return this.getProductName() != null && this.getFinalPrice() != null && this.getFinalPrice() != 0.0f;
    }


    public String getProductName()
    {
        return productName;
    }


    public void setProductName( String productName )
    {
        this.productName = productName.replace( "?", "" ).replaceAll( "\\s+", " " ).trim();

    }


    public void setFinalPrice()
    {
        this.finalPrice = this.getPrices().isEmpty() ? null : this.getPrices().get( this.getPrices().size() - 1 );
    }


    public void setFinalPrice( Float finalPrice )
    {
        this.finalPrice = finalPrice;
    }


    public Float getFinalPrice()
    {
        return finalPrice;
    }


    public String getProductId()
    {
        return productId;
    }


    public void setProductId( String productId )
    {
        if ( productId != null ) {
            this.productId = productId.replace( "?", "" ).trim();
        }
    }


    public boolean isAccurate()
    {
        return isAccurate;
    }


    public void setAccurate( boolean accurate )
    {
        isAccurate = accurate;
    }


    public float getLineSyntaxMatchConfidence()
    {
        return lineSyntaxMatchConfidence;
    }


    public void setLineSyntaxMatchConfidence( float lineSyntaxMatchConfidence )
    {
        this.lineSyntaxMatchConfidence = lineSyntaxMatchConfidence;
    }


    public float getAverageOfFieldConfidence()
    {
        return averageOfFieldConfidence;
    }


    public void setAverageOfFieldConfidence( float averageOfFieldConfidence )
    {
        this.averageOfFieldConfidence = averageOfFieldConfidence;
    }


    public float getNonOptionalFieldsCount()
    {
        return nonOptionalFieldsCount;
    }


    public void setNonOptionalFieldsCount( float nonOptionalFieldsCount )
    {
        this.nonOptionalFieldsCount = nonOptionalFieldsCount;
    }


    public boolean getIsCoupon()
    {
        return isCoupon();
    }


    public void setIsCoupon( boolean isCoupon )
    {
        this.setCoupon( isCoupon );
    }


    public String getDiscountType()
    {
        return discountType;
    }


    public void setDiscountType( String discountType )
    {
        this.discountType = discountType;
    }


    public String getRawText()
    {
        return rawText;
    }


    public void setRawText( String rawText )
    {
        this.rawText = rawText;
    }


    public void setPrices( List<Float> prices )
    {
        this.prices = prices;
    }


    public boolean isCoupon()
    {
        return isCoupon;
    }


    public void setCoupon( boolean isCoupon )
    {
        this.isCoupon = isCoupon;
    }


    public void setFieldConfidences( List<Float> fieldConfidences )
    {
        this.fieldConfidences = fieldConfidences;
    }


    @Override
    public String toString()
    {
        return "LineItem{" + "lineNumber=" + lineNumber + ", productName='" + productName + '\'' + ", finalPrice=" + finalPrice
            + ", productId='" + productId + '\'' + ", prices=" + prices + ", lineConfidence=" + lineConfidence
            + ", averageOfFieldConfidence=" + averageOfFieldConfidence + ", lineSyntaxMatchConfidence="
            + lineSyntaxMatchConfidence + ", isAccurate=" + isAccurate + ", isCoupon=" + isCoupon + ", fieldConfidences="
            + fieldConfidences + ", nonOptionalFieldsCount=" + nonOptionalFieldsCount + ", discountType='" + discountType + '\''
            + ", rawText='" + rawText + '\'' + '}';
    }
}
