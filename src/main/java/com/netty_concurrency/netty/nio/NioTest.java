package com.netty_concurrency.netty.nio;

import java.nio.IntBuffer;
import java.security.SecureRandom;

public class NioTest {
    public static void main(String args[]){
        IntBuffer buffer = IntBuffer.allocate(10);

        for(int i = 0; i < buffer.capacity(); ++i){
            int randomNumber = new SecureRandom().nextInt(20);
            buffer.put(randomNumber);
        }

        buffer.flip();//状态翻转,读写切换

        while (buffer.hasRemaining()){
            System.out.println(buffer.get());
        }
    }
}
