package com.netty_concurrency.netty.com.netty_concurrency.netty.thirdExample;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MyChatClient {
    public static void main(String[] args) throws Exception{
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        try{
            io.netty.bootstrap.Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new MyChatClientInitializer());

            Channel channel = bootstrap.connect("localhost",8899).sync().channel();//建立连接

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));//输入缓存

            for(;;){
                channel.writeAndFlush(br.readLine() + "\r\n");
            }
        }finally {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
