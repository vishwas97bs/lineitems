package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.utils;

public class Constants
{
    private Constants()
    {}


    public enum LINE_ITEM_FIELDS
    {
        TEXT, AMOUNT, QUANTITY, ID, DATE, TRANSACTION_TYPE, UOM, CURRENCY
    }


    public enum DOMAIN_NAME
    {
        receipt, bank, invoice
    }


    public enum LOCALE
    {
        EURO, ENGLISH
    }


    public enum COUPON_TYPE
    {
        percentOff, amountOff, buyXgetY
    }


    //European keywords
    public static final String[] EURO_KEYWORDS = new String[] { "£", "€", "\\u201a", "eur", "rewe" };

    public static final char REGEX_SEPARATOR = '|';


    public static final String LINE_SEPARATOR = "\r\n";

    public static final String LINE_SEPARATOR_REGEX = "\\r\\n";

    public static final String SYNTAX_FIELD_DELIMITER_ESCAPED = " \\| ";

    public static final String CASE_INSENSITIVE_REGEX = "(?i)";

    public static final String WORD_START_REGEX = "^" + CASE_INSENSITIVE_REGEX;

    public static final String WORD_END_REGEX = "$";

    public static final String START_KEYWORDS = "lineitems_line_item_start";

    public static final String END_KEYWORDS = "lineitems_line_item_end";

    public static final String COMPULSORY_FIELDS = "compulsory_fields";

    //Characters to check in the string which needs either to be replaces by another char or be removed
    public static final String SPELL_CHECK_CHARS = "/b, ";

    //Characters to be replaced: charToBeReplaces|charToBeReplacedWith separated by ","
    public static final String SPELL_CHECK_REPLACEMENT_CHARS = "/|7,b|6";

    //Characters to be replaces with empty strings seperated by '|'
    public static final String SPELL_CHECK_REPLACE_WITH_EMPTY = " |,";


    public static final String QUANTITY_REGEX = "lineitem_quantity_regex";

    public static final String NEGATIVE_HEADER_KEYWORDS = "header_negative_keywords";

    public static final String OPTIONAL_FIELD_SUFFIX = "_optional";

    public static final String MONGO_DB_NAME = "dbName";

    public static final String LINEITEM_HEADER_KEYS_COLLECTION = "lineItemHeaders";

    //Completely neglect the line having these terms
    public static final String RECEIPT_LINE_BANNEDWORDS_REGEX = "lineitem_receipt_line_bannedwords_regex";

    //Terms which should nat be present in the name, but the price should be picked up from this line if it is a 2 line lineItem
    public static final String RECEIPT_NAME_SHOULD_NOT_CONTAIN = "lineitem_receipt_name_should_not_contain";

    //lineitem_receipt_name_should_not_contain_perfect_match
    public static final String RECEIPT_NAME_SHOULD_NOT_CONTAIN_PERFECT_MATCH = "lineitem_receipt_name_should_not_contain_perfect_match";

    //Completely neglect the line having these terms
    //Any product name matching these removes them from line
    public static final String INVOICE_LINE_BANNEDWORDS_REGEX = "lineitem_invoice_line_bannedwords_regex";

    //Any key matching these words will remove the entire line
    public static final String INVOICE_NAME_SHOULD_NOT_CONTAIN = "lineitem_invoice_name_should_not_contain";

    //lineitem_invoice_name_should_not_contain_perfect_match
    public static final String INVOICE_NAME_SHOULD_NOT_CONTAIN_PERFECT_MATCH = "lineitem_invoice_name_should_not_contain_perfect_match";

    public static final String RECEIPT_LINEITEM_PRIVOUS_LINE_PRODUCT_NAME_SHOULD_NOT_CONTAIN = "receipt_lineitem_privous_line_product_name_should_not_contain";
}
