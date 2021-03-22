package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.DocumentMetaData;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.ConfigService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.service.GimletConfigService;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.Constants;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils.PriceValidator;
import ai.infrrd.idc.utils.entity.RegexMatchInfo;
import ai.infrrd.idc.utils.extractors.PatternExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
public class DiscountListValidator
{
    private GimletConfigService gimletConfigService;

    private ConfigService configService;

    @Autowired
    public void setGimletConfigService( GimletConfigService gimletConfigService )
    {
        this.gimletConfigService = gimletConfigService;
    }


    @Autowired
    public void setConfigService( ConfigService configService )
    {
        this.configService = configService;
    }


    public void setDiscountList( LineItem dummyLine, String inputLine, String probableName, DocumentMetaData metaData,
        FieldExtractionRequest extractionData )
    {
        Map<String, Object> configuration = gimletConfigService.getGimletConfig();
        String productName = inputLine;
        List<String> couponKeywords = configService.getValueList( "lineitem_coupon_keywords", configuration );
        for ( String couponKeyword : couponKeywords ) {
            if ( inputLine.toLowerCase().contains( couponKeyword ) ) {
                if ( couponKeyword.contains( "off" ) && inputLine.toLowerCase().substring( 0, 5 ).contains( "off" ) ) {
                    dummyLine.setIsCoupon( false );
                } else {
                    dummyLine.setIsCoupon( true );
                }
                int lastPriceMatchIndex = getLastPriceMatchIndex( inputLine );
                if ( lastPriceMatchIndex != -1 ) {
                    productName = productName.substring( 0, lastPriceMatchIndex );
                }
                dummyLine.setProductName( productName );
                break;
            }
        }

        //TO-DO : Put Buy X Get Y condition.
        if ( dummyLine.getIsCoupon() ) {
            int flg = 0;
            List<String> percentKeywords = configService.getValueList( "lineitem_coupon_percentOff_characters", configuration );
            for ( String percentKeyword : percentKeywords ) {
                if ( inputLine.contains( percentKeyword ) ) {
                    dummyLine.setDiscountType( Constants.COUPON_TYPE.percentOff.name() );
                    flg = 1;
                    break;
                }
            }
            if ( flg == 0 ) {
                dummyLine.setDiscountType( Constants.COUPON_TYPE.amountOff.name() );
            }
        }
    }


    private int getLastPriceMatchIndex( String inputLine )
    {
        int lastmatchIndex = -1;
        PatternExtractor patternExtractor = new PatternExtractor( PriceValidator.PERFECT_PRICE_REGEX );
        // search the 2nd half of the string for the price

        int length = inputLine.length();
        int secondHalfBeginIndex = length / 2;
        String secondHalf = inputLine.substring( secondHalfBeginIndex, length );
        List<RegexMatchInfo> regexMatchInfoList = patternExtractor.matchedPatterns( secondHalf );
        for ( RegexMatchInfo regexMatchInfo : regexMatchInfoList ) {
            lastmatchIndex = regexMatchInfo.getStartindex() + secondHalfBeginIndex;
        }

        if ( lastmatchIndex == -1 ) {
            patternExtractor = new PatternExtractor( PriceValidator.PRICE_WITH_SPACES_REGEX );
            regexMatchInfoList = patternExtractor.matchedPatterns( secondHalf );
            for ( RegexMatchInfo regexMatchInfo : regexMatchInfoList ) {
                lastmatchIndex = regexMatchInfo.getStartindex() + secondHalfBeginIndex;
            }
        }

        return lastmatchIndex;
    }


    public Float getMin( List<Float> values )
    {
        float out = 0;
        if ( !values.isEmpty() ) {
            out = values.get( 0 );
            for ( float value : values ) {
                if ( value < out ) {
                    out = value;
                }
            }
        }
        return out;
    }
}
