package com.netty_concurrency.netty.CoapNetty.Options;

public abstract class ContentFormat {
    //用于指示CoAP选项中的负载媒体类型
    //coap媒体类型采用2字节无符号整数编号的方式定义（这里多用一个-1作为无定义）
    //coap的媒体类型是http的媒体类型的微小子集
    public static final long UNDEFINED = -1;
    public static final long TEXT_PLAIN_UTF8 = 0;
    public static final long APP_LINK_FORMAT = 40;
    public static final long APP_XML = 41;
    public static final long APP_OCTET_STREAM = 42;
    public static final long APP_EXI = 47;
    public static final long APP_JSON = 50;
    public static final long APP_CBOR = 60;
}
