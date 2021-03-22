package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

public class Header {
    private String headerName;
    private int index;


    public Header( String name, int index )
    {
        this.headerName = name;
        this.index = index;
    }


    public String getHeaderName()
    {
        return headerName;
    }


    public void setHeaderName( String headerName )
    {
        this.headerName = headerName;
    }


    public int getIndex()
    {
        return index;
    }


    public void setIndex( int index )
    {
        this.index = index;
    }
}
