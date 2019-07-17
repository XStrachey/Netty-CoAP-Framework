package com.netty_concurrency.netty.fifthExample;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

public class MyServer {
    public static void main(String[] args) throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup(); //接收 //①
        EventLoopGroup workerGroup = new NioEventLoopGroup(); //处理
        try {

            ServerBootstrap serverBootstrap = new ServerBootstrap(); //用于启动服务端②
            serverBootstrap.group(bossGroup,workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO)).childHandler(new MyServerInitializer());//③handler针对boss，childhandler针对worker

            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(8899)).sync();//绑定端口
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();//优雅关闭
            workerGroup.shutdownGracefully();
        }

    }
}
