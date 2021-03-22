package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class InvoiceLineItem extends LineItem {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( InvoiceLineItem.class );
    private List<Float> quantities = new ArrayList<>();
    private String date;
    private List<String> untagged = new ArrayList<>();
    private int nameIndex;
    private List<String> uom = new ArrayList<>();
    private float unitPrice;
    private String gst;
    private String lineText;


    public int getNameIndex()
    {
        return nameIndex;
    }


    public void setNameIndex( int nameIndex )
    {
        this.nameIndex = nameIndex;
    }


    public List<Float> getQuantities()
    {
        return quantities;
    }


    public void addToQuantities( String quantity )
    {
        if ( this.quantities == null ) {
            quantities = new ArrayList<>();
        }
        this.quantities.add( Float.parseFloat( quantity ) );
    }


    public String getDate()
    {
        return date;
    }


    public void setDate( String date )
    {
        this.date = date;
    }


    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( "\"Line Number\" : \"" + this.getLineNumber() + "\"" );
        if ( this.getLineText() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"LINE\" : \"" + this.getLineText() + "\"" );
        }
        if ( this.getQuantities() != null && !this.getQuantities().isEmpty() ) {
            builder.append( "\"QUANTITY\" : \"" + this.getQuantities() + "\"" );
        }
        if ( this.getDate() != null && !this.getDate().isEmpty() ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"DATE\" : \"" + this.getDate() + "\"" );
        }
        if ( this.getProductName() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"TEXT\" : \"" + this.getProductName().replace( "\\", "" ).replace( "\"", "" ) + "\"" );
        }
        if ( this.getPrices() != null && !this.getPrices().isEmpty() ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"PRICE\" : \"" + this.getPrices() + "\"" );
        }
        if ( this.getFinalPrice() != 0 ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"FINAL PRICE\" : \"" + this.getFinalPrice() + "\"" );
        }
        if ( this.getUnitPrice() != 0 ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"UNIT PRICE\" : \"" + this.getUnitPrice() + "\"" );
        }
        if ( this.getProductId() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"ID\" : \"" + this.getProductId() + "\"" );
        }
        if ( this.getUom() != null && !this.getUom().isEmpty() ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"UOM\" : \"" + this.getUom() + "\"" );
        }
        if ( this.getGst() != null && !this.getGst().isEmpty() ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"GST\" : \"" + this.getGst() + "\"" );
        }
        if ( this.getUntagged() != null && !this.getUntagged().isEmpty() ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( " \"UNTAGGED\" : \"" + this.getUntagged() + "\"" );
        }
        if ( builder.length() != 1 ) {
            builder.append( "," );
        }
        builder.append( " \"isAccurate\" : " + this.isAccurate() );
        builder.append( "}" );
        return builder.toString();
    }


    public List<String> getUntagged()
    {
        return untagged;
    }


    public void addToUntagged( String untaggedWord )
    {
        if ( this.untagged == null ) {
            untagged = new ArrayList<>();
        }
        this.untagged.add( untaggedWord.replace( "\"", "" ).replace( "\\", "" ) );
    }


    public List<String> getUom()
    {
        return uom;
    }


    public void addToUom( String uomStr )
    {
        if ( this.uom == null ) {
            uom = new ArrayList<>();
        }
        this.uom.add( uomStr );
    }


    public float getUnitPrice()
    {
        return unitPrice;
    }


    public void setUnitPrice( float unitPrice )
    {
        this.unitPrice = unitPrice;
    }


    public String getGst()
    {
        return gst;
    }


    public void setGst( String gst )
    {
        this.gst = gst;
    }


    public String getLineText()
    {
        return lineText;
    }


    public void setLineText( String lineText )
    {
        this.lineText = lineText;
    }
}
