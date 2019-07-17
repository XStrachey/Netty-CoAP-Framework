package com.netty_concurrency.netty.CoapNetty;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

//占8位，为高3位class部分，低5位detail部分
//作为对比的是，在HTTP协议中请求方法和响应采用字符串形式表达，单位就是byte了
//coap和http的请求、响应语义相似
public abstract class MsgCode {

    //状态码格式： a.bc ； 含义为： a为字节前三位，bc为字节后5位
    //如2.31，拆开分成2进制为：
    //010 11111
    //转成16进制则为：0x5f

    //空报文，只有首部没有负载
    //既不是请求也不是响应的消息
    //CON一个EMPTY代表让接收方发出RST,这就是ping
    //状态码/实际数值
    //0.00/0x00
    public static final int EMPTY = 0;

    //发送方（请求）
    //所有请求方法都会放在CoAP CON/NON消息里传输
    //对于不能识别的请求方法，需要返回一个4.05，参考下面
    //状态码/实际数值
    //0.01/0x01
    public static final int GET = 1;
    //0.02/0x02
    public static final int POST = 2;
    //0.03/0x03
    public static final int PUT = 3;
    //0.04/0x04
    public static final int DELETE = 4;

    //响应方（响应）
    //所有的响应可以放在CoAP CON/NON/ACK消息里传输
    //针对CON消息请求，响应如果可以快速处理完，则可直接放在ACK消息包返回
    //否则待资源ready后，单独发响应消息包
    //类型：success
    //状态码/十进制数值/十六进制数值
    //2.01/65/0x41
    //对POST和PUT的响应
    public  static final int CREATED_201 = 65;
    //2.02/66/0x42
    //对POST和DELETE的响应
    //对POST请求下的说明：因为POST可能会使服务器上的一个资源被覆盖，这样就会被返回一个DELETE代表资源覆盖
    public static final int DELETED_202 = 66;
    //2.03/67/0x43
    //常用于GET请求
    //用于指示请求中ETAG指定的响应是有效的
    //响应中的负载内容一般为空
    public static final int VALID_203 = 67;
    //2.04/68/0x44
    //对POST和PUT的响应，not cacheable
    //表示更新了某个资源
    public static final int CHANGED_204 = 68;
    //2.05/69/0x45
    //对GET的响应，响应中包含目标资源的representation，is cacheable
    //响应中一般包含具体负载
    public static final int CONTENT_205 = 69;
    //2.31/95/0x5f 用于块传输大小协商及控制
    public static final int CONTINUE_231 = 95;

    //类型：client error
    //4.00/128/0x80
    public static final int BAD_REQUEST_400 = 128;
    //4.01/129/0x81
    //未有权限
    public static final int UNAUTHORIZED_401 = 129;
    //4.02/130/0x82
    //请求包含一个或多个未能识别的选项
    public static final int BAD_OPTION_402 = 130;
    //4.03/131/0x83
    public static final int FORBIDDEN_403 = 131;
    //4.04/132/0x84
    public static final int NOT_FOUND_404 = 132;
    //4.05/133/0x85
    //表明客户端使用了一个服务器未定义的方法访问该资源
    public static final int METHOD_NOT_ALLOWED_405 = 133;
    //4.06/134/0x86
    public static final int NOT_ACCEPTABLE_406 = 134;
    //4.08/136/0x88 用于块传输大小协商及控制
    public static final int REQUEST_ENTITY_INCOMPLETE_408 = 136;
    //4.12/140/0x8c
    //说明客户端在请求中定义了一个或多个先决条件，而现在未满足特定条件
    public static final int PRECONDITION_FAILED_412 = 140;
    //4.13/141/0x8d
    public static final int REQUEST_ENTITY_TOO_LARGE_413 = 141;
    //4.15/143/0x8f
    public static final int UNSUPPORTED_CONTENT_FORMAT_415 = 143;

    //类型：server error
    //5.00/160/0xa0
    public static final int INTERNAL_SERVER_ERROR_500 = 160;
    //5.01/161/0xa1
    public static final int NOT_IMPLEMENTED_501 = 161;
    //5.02/162/0xa2
    public static final int BAD_GATEWAY_502 = 162;
    //5.03/163/0xa3
    public static final int SERVICE_UNAVAILABLE_503 = 163;
    //5.04/164/0xa4
    public static final int GATEWAY_TIMEOUT_504 = 164;
    //5.05/165/0xa5
    public static final int PROXYING_NOT_SUPPORTED_505 = 165;

    //前号后名
    private static final HashMap<Integer, String> MESSAGE_CODE = new HashMap<>();
    static {
        MESSAGE_CODE.putAll(ImmutableMap.<Integer, String>builder()
                .put(EMPTY, "EMPTY (" + EMPTY + ")")
                .put(GET, "GET (" + GET + ")")
                .put(POST, "POST (" + POST + ")")
                .put(PUT, "PUT (" + PUT + ")")
                .put(DELETE, "DELETE (" + DELETE + ")")
                .put(CREATED_201, "CREATED_201 (" + CREATED_201 + ")")
                .put(DELETED_202, "DELETED_202 (" + DELETED_202 + ")")
                .put(VALID_203, "VALID_203 (" + VALID_203 + ")")
                .put(CHANGED_204, "CHANGED_204 (" + CHANGED_204 + ")")
                .put(CONTENT_205, "CONTENT_205 (" + CONTENT_205 + ")")
                .put(CONTINUE_231, "CONTINUE_231 (" + CONTINUE_231 + ")")
                .put(BAD_REQUEST_400, "BAD_REQUEST_400 (" + BAD_REQUEST_400 + ")")
                .put(UNAUTHORIZED_401, "UNAUTHORIZED_401 (" + UNAUTHORIZED_401 + ")")
                .put(BAD_OPTION_402, "BAD_OPTION_402 (" + BAD_OPTION_402 + ")")
                .put(FORBIDDEN_403, "FORBIDDEN_403 (" + FORBIDDEN_403 + ")")
                .put(NOT_FOUND_404, "NOT_FOUND_404 (" + NOT_FOUND_404 + ")")
                .put(METHOD_NOT_ALLOWED_405, "METHOD_NOT_ALLOWED_405 (" + METHOD_NOT_ALLOWED_405 + ")")
                .put(NOT_ACCEPTABLE_406, "NOT_ACCEPTABLE_406 (" + NOT_ACCEPTABLE_406 + ")")
                .put(REQUEST_ENTITY_INCOMPLETE_408, "REQUEST_ENTITY_INCOMPLETE_408 (" + REQUEST_ENTITY_INCOMPLETE_408 + ")")
                .put(PRECONDITION_FAILED_412, "PRECONDITION_FAILED_412 (" + PRECONDITION_FAILED_412 + ")")
                .put(REQUEST_ENTITY_TOO_LARGE_413, "REQUEST_ENTITY_TOO_LARGE_413 (" + REQUEST_ENTITY_TOO_LARGE_413 + ")")
                .put(UNSUPPORTED_CONTENT_FORMAT_415, "UNSUPPORTED_CONTENT_FORMAT_415 (" + UNSUPPORTED_CONTENT_FORMAT_415 + ")")
                .put(INTERNAL_SERVER_ERROR_500, "INTERNAL_SERVER_ERROR_500 (" + INTERNAL_SERVER_ERROR_500 + ")")
                .put(NOT_IMPLEMENTED_501, "NOT_IMPLEMENTED_501 (" + NOT_IMPLEMENTED_501 + ")")
                .put(BAD_GATEWAY_502, "BAD_GATEWAY_502 (" + BAD_GATEWAY_502 + ")")
                .put(SERVICE_UNAVAILABLE_503, "SERVICE_UNAVAILABLE_503 (" + SERVICE_UNAVAILABLE_503 + ")")
                .put(GATEWAY_TIMEOUT_504, "GATEWAY_TIMEOUT_504 (" + GATEWAY_TIMEOUT_504 + ")")
                .put(PROXYING_NOT_SUPPORTED_505, "PROXYING_NOT_SUPPORTED_505 (" + PROXYING_NOT_SUPPORTED_505 + ")")
                .build());
    }

    public static String asString(int msgCode){
        String result = MESSAGE_CODE.get(msgCode);
        return result == null ? "UNKNOWN (" + msgCode + ")" : result;
    }

    public static boolean isMsgCode(int key){
        return MESSAGE_CODE.containsKey(key);
    }

    public static boolean isRequest(int msgCode){
        return (msgCode > 0 && msgCode < 5);
    }

    public static boolean isResponse(int msgCode){
        return msgCode >= 5;
    }

    public static boolean isErrorMsg(int codeKey){
        return (codeKey >= 128);
    }

    //PUT,POST 这两个请求方法携带内容
    public static boolean allowsContent(int codeKey){
        return !(codeKey == GET || codeKey == DELETE);
    }
}
