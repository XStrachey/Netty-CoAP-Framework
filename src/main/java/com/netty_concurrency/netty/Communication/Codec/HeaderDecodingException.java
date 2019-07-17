package com.netty_concurrency.netty.Communication.Codec;

import java.net.InetSocketAddress;

public class HeaderDecodingException extends Exception {
    private int msgID;
    private InetSocketAddress remoteSocket;

    public HeaderDecodingException(int msgID, InetSocketAddress remoteSocket, String msg){
        super(msg);
        this.msgID = msgID;
        this.remoteSocket = remoteSocket;
    }

    public int getMsgID(){
        return msgID;
    }

    public InetSocketAddress getRemoteSocket(){
        return remoteSocket;
    }
}
