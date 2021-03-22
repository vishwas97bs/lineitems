package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity;

import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Constants;

import java.util.List;
import java.util.Map;

public class DocumentMetaData {
    Constants.LOCALE language;
    Domain domain;
    String syntax;
    String merchantName;
    int firstLineNumber = -1;
    String headers;
    String headerLineIdentifier;
    String headerLineString;
    // Map of <headerLineNo,<headerIndex,headerName>>
    Map<Integer, Map<Integer, String>> headersMap;
    List<Integer> linesHavingMatchedSyntax;


    public String getRemainingHeaderStr()
    {
        return remainingHeaderStr;
    }


    public void setRemainingHeaderStr( String remainingHeaderStr )
    {
        this.remainingHeaderStr = remainingHeaderStr;
    }


    String remainingHeaderStr;

    public Constants.LOCALE getLanguage()
    {
        return language;
    }


    public void setLanguage( Constants.LOCALE language )
    {
        this.language = language;
    }


    public List<Integer> getLinesHavingMatchedSyntax()
    {
        return linesHavingMatchedSyntax;
    }


    public void setLinesHavingMatchedSyntax( List<Integer> linesHavingMatchedSyntax )
    {
        this.linesHavingMatchedSyntax = linesHavingMatchedSyntax;
    }


    public Map<Integer, Map<Integer, String>> getHeadersMap()
    {
        return headersMap;
    }


    public void setHeadersMap( Map<Integer, Map<Integer, String>> finalHeadersMap )
    {
        this.headersMap = finalHeadersMap;
    }


    public String getHeaderLineIdentifier()
    {
        return headerLineIdentifier;
    }


    public void setHeaderLineIdentifier( String headerKeyword )
    {
        this.headerLineIdentifier = headerKeyword;
    }


    public String getHeaderLineString()
    {
        return headerLineString;
    }


    public void setHeaderLineString( String headerLineString )
    {
        this.headerLineString = headerLineString;
    }


    public String getMerchantName()
    {
        return merchantName;
    }


    public void setMerchantName( String merchantName )
    {
        this.merchantName = merchantName;
    }


    public String getHeaders()
    {
        return headers;
    }


    public void setHeaders( String headers )
    {
        this.headers = headers;
    }


    public int getFirstLineNumber()
    {
        return firstLineNumber;
    }


    public void setFirstLineNumber( int firstLineNumber )
    {
        this.firstLineNumber = firstLineNumber;
    }


    public Domain getDomain()
    {
        return domain;
    }


    public void setDomain( Domain domain )
    {
        this.domain = domain;
    }


    public String getSyntax()
    {
        return syntax;
    }


    public void setSyntax( String syntax )
    {
        this.syntax = syntax;
    }
}
