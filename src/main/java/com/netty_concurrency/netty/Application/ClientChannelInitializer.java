package com.netty_concurrency.netty.Application;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class ClientChannelInitializer extends CoapChannelInitializer {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline channelPipeline = ch.pipeline();
    }
}
