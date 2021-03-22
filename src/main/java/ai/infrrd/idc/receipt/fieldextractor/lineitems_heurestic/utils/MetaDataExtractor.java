package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.DocumentMetaData;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.Header;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class MetaDataExtractor
{
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( MetaDataExtractor.class );

    private LineItemExtractorUtils lineItemExtractorUtils;

    private ReceiptDomain receiptDomain;

    @Autowired
    public void setLineItemExtractorUtils( LineItemExtractorUtils lineItemExtractorUtils )
    {
        this.lineItemExtractorUtils = lineItemExtractorUtils;
    }

    @Autowired
    public void setReceiptDomain(ReceiptDomain receiptDomain) {
        this.receiptDomain = receiptDomain;
    }

    public DocumentMetaData getDocumentMetaDataFromTextForReceipts(FieldExtractionRequest helper, Map<String, Object> value, Map<String,Object> configuration )
    {
        DocumentMetaData metaData = new DocumentMetaData();
        metaData.setLanguage( getLanguage( helper.getOcrData().getRawText() ) );
        metaData.setMerchantName( lineItemExtractorUtils.getMerchantName( value, helper,configuration ) );
        metaData.setDomain( receiptDomain );
        return metaData;
    }


    public Constants.LOCALE getLanguage( String originalExtractedText )
    {
        for ( String euroBankKeyword : Constants.EURO_KEYWORDS ) {

            if ( originalExtractedText.toLowerCase().contains( euroBankKeyword ) ) {
                return Constants.LOCALE.EURO;
            }
        }
        return Constants.LOCALE.ENGLISH;
    }


    String getHeader( String syntax, String... rows )
    {
        LOG.trace( "Method: getHeader called." );
        List<List<Header>> multiLineHeaders = new ArrayList<>();
        int h = 0;
        for ( String rowStr : rows ) {
            List<Header> headers = extractHeaderInRow( syntax, rowStr );
            if ( !headers.isEmpty() )
                multiLineHeaders.add( h++, headers );
        }
        multiLineHeaders = Lists.reverse( multiLineHeaders );
        List<Header> finalHeader = getHeaderFromMultiLineHeaderObjects( multiLineHeaders );
        LOG.info( "Headers found : {}", finalHeader );
        LOG.trace( "Method: getHeader finished." );
        return generateHeaderString( finalHeader );
    }


    private String generateHeaderString( List<Header> finalHeader )
    {
        LOG.trace( "Method: generateHeaderString called." );
        StringBuilder headerStr = new StringBuilder();
        headerStr.append( '[' );
        if ( finalHeader != null ) {
            Iterator<Header> itr = finalHeader.iterator();
            while ( itr.hasNext() ) {
                headerStr.append( itr.next().getHeaderName() );
                if ( itr.hasNext() ) {
                    headerStr.append( ", " );
                }
            }
        }
        headerStr.append( ']' );
        LOG.trace( "Method: generateHeaderString finished." );
        return headerStr.toString();
    }


    List<Header> getHeaderFromMultiLineHeaderObjects( List<List<Header>> multiLineHeaders )
    {
        List<Header> finalHeader = null;
        for ( int headerPtr = 0; headerPtr < multiLineHeaders.size(); headerPtr++ ) {
            List<Header> itr = multiLineHeaders.get( headerPtr );
            if ( finalHeader == null ) {
                finalHeader = itr;
            } else {
                for ( int j = 0; j < itr.size(); j++ ) {
                    int currentIndex = itr.get( j ).getIndex();
                    int pos = 0;
                    try {
                        int currInHeader = finalHeader.get( j ).getIndex();
                        int nextInHeader = finalHeader.get( j + 1 ).getIndex();
                        pos = Math.abs( currentIndex - currInHeader ) > Math.abs( currentIndex - nextInHeader ) ? j + 1 : j;
                    } catch ( IndexOutOfBoundsException ex ) {
                        pos = j;
                    }
                    try {
                        Header corrHeader = finalHeader.get( pos );
                        corrHeader.setHeaderName( corrHeader.getHeaderName() + " " + itr.get( j ).getHeaderName() );
                    } catch ( IndexOutOfBoundsException ex ) {
                        LOG.error( "Headers not read correctly" );
                    }
                }
            }
        }
        return finalHeader;
    }


    List<Header> extractHeaderInRow( String syntax, String rowStr )
    {
        List<Header> headers = new ArrayList<>();
        String[] row = rowStr.replaceAll( "\\s{3,}", "  " ).trim().split( "  " );
        if ( row.length >= syntax.split( Constants.SYNTAX_FIELD_DELIMITER_ESCAPED ).length ) {
            for ( String rowElement : row ) {
                if ( !rowElement.isEmpty() ) {
                    headers.add( new Header( rowElement, rowStr.indexOf( rowElement ) ) );
                    rowStr = StringUtils.replacePatternWithSpaces( rowStr, rowElement );
                }
            }
        }
        return headers;
    }

}
