package com.netty_concurrency.netty.firstexample;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class TestSever {
    public static void main(String[] args) throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup(); //接收 //①
        EventLoopGroup workerGroup = new NioEventLoopGroup(); //处理
        try {

            ServerBootstrap serverBootstrap = new ServerBootstrap(); //用于启动服务端②
            serverBootstrap.group(bossGroup,workerGroup).channel(NioServerSocketChannel.class).childHandler(new TestServerInitialier());//③

            ChannelFuture channelFuture = serverBootstrap.bind(8899).sync();//绑定端口
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();//优雅关闭
            workerGroup.shutdownGracefully();
        }

    }
}
