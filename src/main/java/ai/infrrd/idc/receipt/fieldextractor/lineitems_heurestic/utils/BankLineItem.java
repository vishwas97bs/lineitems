package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItem;

import java.util.ArrayList;
import java.util.List;


public class BankLineItem extends LineItem
{
    private String transactionType;
    private List<String> dates = new ArrayList<>();
    private List<String> currencies = new ArrayList<>();
    private int nameIndex;


    public List<String> getDates()
    {
        return dates;
    }


    public void addToDates( String date )
    {
        if ( this.dates == null ) {
            this.dates = new ArrayList<>();
        }
        this.dates.add( date );
    }


    public void addToCurrency( String currency )
    {
        if ( this.currencies == null )
            this.currencies = new ArrayList<>();
        this.currencies.add( currency );
    }


    public String getTransactionType()
    {
        return transactionType;
    }


    public void setTransactionType( String transactionType )
    {
        this.transactionType = transactionType;
    }


    public int getNameIndex()
    {
        return nameIndex;
    }


    public void setNameIndex( int nameIndex )
    {
        this.nameIndex = nameIndex;
    }


    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        if ( transactionType != null ) {
            builder.append( "transactionType : \"" + transactionType + "\"" );
        }
        int i = 0;
        if ( !dates.isEmpty() ) {
            for ( String date : dates ) {
                if ( builder.length() != 1 ) {
                    builder.append( "," );
                }
                builder.append( " \"DATE" + i++ + "\" : \"" + date + "\"" );
            }
        }
        i = 0;
        if ( !currencies.isEmpty() ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"CURRENCIES" + "\" : " + this.currencies + "" );
            for ( String currency : currencies ) {
                if ( builder.length() != 1 ) {
                    builder.append( "," );
                }
                builder.append( " \"CURRENCY" + i++ + "\" : \"" + currency + "\"" );
            }
        }
        if ( this.getProductName() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"TEXT\" : \"" + this.getProductName() + "\"" );
        }
        i = 0;
        if ( !this.getPrices().isEmpty() ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"AMOUNTS" + "\" : " + this.getPrices() + "" );
            for ( float price : this.getPrices() ) {
                if ( builder.length() != 1 ) {
                    builder.append( "," );
                }
                builder.append( "\"AMOUNT" + i++ + "\" : \"" + price + "\"" );
            }
        }

        if ( this.getRawText() != null ) {
            if ( builder.length() != 1 ) {
                builder.append( "," );
            }
            builder.append( "\"RAWTEXT\" : \"" + this.getRawText() + "\"" );
        }

        builder.append( "}" );
        return builder.toString();
    }

}
