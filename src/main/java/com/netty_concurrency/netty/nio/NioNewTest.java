package com.netty_concurrency.netty.nio;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class NioNewTest {
    public static void main(String[] args) throws Exception{
        FileInputStream fileInputStream = new FileInputStream("Nio.txt");
        FileChannel fileChannel = fileInputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        fileChannel.read(byteBuffer);

        byteBuffer.flip();

        while(byteBuffer.remaining() > 0){
            byte b = byteBuffer.get();
            System.out.println("Charater: " + (char)b);
        }

        fileInputStream.close();
    }
}
