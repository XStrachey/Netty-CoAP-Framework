package com.netty_concurrency.netty.Communication.Codec;

import com.netty_concurrency.netty.CoapNetty.*;
import com.netty_concurrency.netty.CoapNetty.Options.Option;

import java.net.InetSocketAddress;

public class OptionCodecException extends Exception{
    private static final String msg = "Unsupported or misplaced critical option %s";

    private int optionNum;
    private int msgID;
    private Token token;
    private InetSocketAddress remoteSocket;
    private int msgType;

    public OptionCodecException(int optionNum) {
        super();
        this.optionNum = optionNum;
    }

    public int getOptionNum() {
        return optionNum;
    }

    public void setMsgID(int msgID) {
        this.msgID = msgID;
    }

    public int getMsgID() {
        return msgID;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public void setremoteSocket(InetSocketAddress remoteSocket) {
        this.remoteSocket = remoteSocket;
    }

    public InetSocketAddress getremoteSocket() {
        return remoteSocket;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    @Override
    public String getMessage() {
        return String.format(msg, Option.asString(this.optionNum));
    }
}
