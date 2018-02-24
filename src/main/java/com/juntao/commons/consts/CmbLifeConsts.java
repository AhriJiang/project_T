package com.juntao.commons.consts;

import org.apache.commons.lang3.tuple.Pair;

public class CmbLifeConsts {

    public static final String MID = "mid";
    public static final String AID = "aid";
    public static final String DATE = "date";
    public static final String RANDOM = "random";
    public static final String SIGN = "sign";

    public static final String BILL_NO_N = "billNo";
    public static final String BILL_NO = "billno";
    public static final String PRODUCT_NAME = "productname";
    public static final String AMOUNT = "amount";
    public static final String BONUS = "bonus";
    public static final String CMB_MERCHANT_NO = "cmbmerchantno";
    public static final String CMB_TERMINAL_NO = "cmbterminalno";
    public static final String NOTIFY_URL = "notifyurl";
    public static final String RETURN_URL = "returnurl";
    public static final String PAY_PERIOD = "payPeriod";
    public static final String REFUND_TOKEN = "refundToken";
    public static final String V = "v";

    public static final String MESSAGE = "message";
    public static final String CUP_REF_NO = "cuprefno";
    public static final String SHIELD_CARD_NO = "shieldcardno";
    public static final String BANK_PAY_SERIAL = "bankpayserial";
    public static final String REF_NUM = "refnum";
    public static final String ORDER_NUM = "orderNum";
    public static final String RESULT = "result";
    public static final String PAY_TYPE_T = "payType";
    public static final String PAY_TYPE = "paytype";

    public static final String BANK_REFUND_SERIAL = "bankRefundSerial";
    public static final String RETURN_REF_NUM = "returnRefNum";
    public static final String REFUND_STATUS = "refundStatus";

    public static final String RESP_CODE = "respCode";
    public static final String RESP_MSG = "respMsg";

    public static final String USER_ID = "userId";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String GAME_CODE = "gameCode";
    public static final String CODE = "code";
    public static final String TREASURE_TYPE = "treasureType";
    public static final String TREASURE_ID = "treasureId";
    public static final String TREASURE_AMOUNT = "treasureAmount";
    public static final String REF_TOKEN = "refToken";
    public static final String RES_TOKEN = "resToken";

    public static final String CONFIG_PAY_FUNCNAME = "PAY_FUNCNAME";
    public static final String CONFIG_PAYORDER_FUNCNAME = "PAYORDER_FUNCNAME";
    public static final String CONFIG_REFUND_FUNCNAME = "REFUND_FUNCNAME";
    public static final String CONFIG_REFUNDORDER_FUNCNAME = "REFUNDORDER_FUNCNAME";
    public static final String CONFIG_PAYORDER_URL = "PAYORDER_URL";
    public static final String CONFIG_REFUND_URL = "REFUND_URL";
    public static final String CONFIG_REFUNDORDER_URL = "REFUNDORDER_URL";
    public static final String CONFIG_PAY_PERIOD = "PAY_PERIOD";
    public static final String CONFIG_REFUND_VERSION = "REFUND_VERSION";
    public static final String CONFIG_CMB_MERCHANT_NO = "FUNMALL_CMBMERCHANTNO";
    public static final String CONFIG_CMB_TERMINAL_NO = "FUNMALL_CMBTERMINALNO";
    public static final String CONFIG_PAY_NOTIFY_URL = "PAY_NOTIFY_URL";
    public static final String CONFIG_FUNMALL_PRI_KEY = "FUNMALL_PRI_KEY";
    public static final String CONFIG_FUNMALL_PUB_KEY = "FUNMALL_PUB_KEY";
    public static final String CONFIG_FUNMALL_AID = "FUNMALL_AID";
    public static final String CONFIG_FUNMALL_MID = "FUNMALL_MID";
    public static final String CONFIG_CMBLIFE_PUB_KEY = "CMBLIFE_PUB_KEY";

    public static final Pair<String, String> NOTIFY_RESULT_SUCCESS = Pair.of("2", "成功");
    public static final Pair<String, String> NOTIFY_RESULT_FAIL = Pair.of("3", "失败");
    public static final Pair<String, String> NOTIFY_RESULT_200 = Pair.of("200", "支付成功通知失败");
    public static final Pair<String, String> NOTIFY_RESULT_300 = Pair.of("300", "支付失败通知失败");

    public static final Pair<String, String> PAY_QUERY_RESULT_UNPAID = Pair.of("1", "待支付");
    public static final Pair<String, String> PAY_QUERY_RESULT_SUCCESS = Pair.of("2", "成功");
    public static final Pair<String, String> PAY_QUERY_RESULT_FAIL = Pair.of("3", "失败");
    public static final Pair<String, String> PAY_QUERY_RESULT_UNKNOW = Pair.of("4", "未知");
    public static final Pair<String, String> PAY_QUERY_RESULT_PROCESSING = Pair.of("5", "处理中");

    public static final Pair<String, String> REFUND_QUERY_RESULT_NOTFOUND = Pair.of("0", "未找到");
    public static final Pair<String, String> REFUND_QUERY_RESULT_UNREFUNDED = Pair.of("1", "待退款");
    public static final Pair<String, String> REFUND_QUERY_RESULT_SUCCESS = Pair.of("2", "成功");
    public static final Pair<String, String> REFUND_QUERY_RESULT_FAIL = Pair.of("3", "失败");
    public static final Pair<String, String> REFUND_QUERY_RESULT_UNKNOW = Pair.of("4", "未知");

    public static final Pair<String, String> REFUND_RESULT_FAIL = Pair.of("0", "失败");
    public static final Pair<String, String> REFUND_RESULT_SUCCESS = Pair.of("1", "成功");

    public static final String NOTIFY_SUCCESS_CODE = "1000";
    public static final String NOTIFY_FAIL_CODE = "1001";


}
