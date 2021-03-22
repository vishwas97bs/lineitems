package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.validator;

import ai.infrrd.idc.commons.entities.FieldExtractionRequest;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.DocumentMetaData;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.Domain;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineItem;
import ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.entity.LineValidator;

import java.util.List;
import java.util.Map;


public abstract class FieldValidator
{
    /**
     * Return true if field is compulsory, else return false
     * @return boolean that defines whether the field is compulsory or not
     */
    public abstract boolean isCompulsory();


    public abstract boolean isOptional();


    /**
     * This method extracts value(s - if isSetAllMatches = true) of a field from the inputline,
     * @param line line object to which the field will be assigned
     * @param indexes list of indexes at which the particular field was found
     * @param setAllMatches set all the matches or only the first match of the field in the line
     * @param helper
     * @param merchantSpecificSyntaxRegex  @return Input String with the set words removed
     * @param metaData
     * @param extractionHelper
     */
    public abstract String setField( LineItem line, List<Integer> indexes, boolean setAllMatches, LineValidator helper,
        String merchantSpecificSyntaxRegex, DocumentMetaData metaData, FieldExtractionRequest extractionHelper,
        Map<String, Object> configuration );
}
