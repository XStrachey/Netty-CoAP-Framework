package com.netty_concurrency.netty.Application;

import com.netty_concurrency.netty.Communication.Codec.CoapMsgDecoder;
import com.netty_concurrency.netty.Communication.Codec.CoapMsgEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.LinkedHashSet;
import java.util.Set;

public class CoapChannelInitializer extends ChannelInitializer<SocketChannel> {
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline channelPipeline = ch.pipeline();
        this.channelHandlers = new LinkedHashSet<>();

        //addChannelHandler(new ExecutionHandler(executor));
        addChannelHandler(new CoapMsgEncoder());
        addChannelHandler(new CoapMsgDecoder());
    }

    private Set<ChannelHandler> channelHandlers;

    protected void addChannelHandler(ChannelHandler channelHandler) {
        this.channelHandlers.add(channelHandler);
    }

    public Set<ChannelHandler> getChannelHandlers () {
        return this.channelHandlers;
    }
}
