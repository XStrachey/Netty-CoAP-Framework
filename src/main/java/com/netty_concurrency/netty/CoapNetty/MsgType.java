package com.netty_concurrency.netty.CoapNetty;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

public abstract class MsgType {

    //confirmable 00
    //CON类型的消息为主动发出消息请求，并且需要接收方作出回复
    public static final int CON = 0;

    //non-confirmable 01
    //NON类型的消息为主动发出消息请求，但是不需要接收方作出回复
    //适合用于消息会重复频繁发送，丢掉消息不对业务产生影响
    public static final int NON = 1;

    //acknowledgement 10
    //ACK类型的消息为接收方作出回复
    public static final int ACK = 2;

    //reset 11
    //RST类型为发出CON消息后，在还没收到请求时，主动通知不需要再回复
    //服务器收到一个CON报文，但该报文上下文缺失而导致无法处理，返回RST
    //RST报文负载为空
    public static final int RST = 3;

    //可靠传输，客户端基于CON消息传输，服务器收到CON消息后，需要返回ACK
    //客户端在ACK_TIMEOUT内收到ACK消息，代表消息可靠到达
    //客户端发送CON到服务器，未收到ACK或RST，支持基于指数回退的重发
    //服务器如果可以处理该消息，则返回ACK，否则返回RST
    //同步可靠响应：通过CON的ACK携带响应
    //异步可靠传输：服务器如果不能立即响应（资源没有准备好的话）
    //可以先用空ACK响应客户端，待资源准备好再通过新CON响应给客户端（分离模式）

    //不可靠传输，基于NON消息传输，服务器收到NON消息后，不回复

    //前号后名
    private static final HashMap<Integer, String > MESSAGE_TYPES = new HashMap<>();
    static {
        MESSAGE_TYPES.putAll(ImmutableMap.<Integer, String >builder()
                .put(CON, "CON (" + CON + ")")
                .put(NON, "NON (" + NON +  ")")
                .put(ACK, "ACK (" + ACK + ")")
                .put(RST, "RST (" + RST + ")")
                .build());
    }

    public static String asString(int msgType){
        String result = MESSAGE_TYPES.get(msgType);
        return result == null ? "UNKNOWN (" + msgType + ")" : result;
    }

    public static boolean isMsgType(int key){
        return MESSAGE_TYPES.containsKey(key);
    }
}
