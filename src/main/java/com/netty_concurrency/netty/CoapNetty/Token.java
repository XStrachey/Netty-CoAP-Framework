package com.netty_concurrency.netty.CoapNetty;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLongs;

import java.util.Arrays;

public class Token implements Comparable<Token> {

    //限定token的最大长度，当前TKL有效取值为0~8
    public static int MAX_LENGTH = 8;

    //存储一个16进制表示数组
    private final static char[] hexArr = "0123456789ABCDEF".toCharArray();

    //也叫请求ID，把响应与之前的请求关联起来（可以理解为一个标签或则说标记）
    //常用16进制表示，比如Token 0x71
    //客户端发出请求带上token
    //服务器回复时返回token，客户端收到响应后，取出token，就可以知道该响应针对哪个请求
    private byte[] token;

    public Token(byte[] token){
        if(token.length > 8){
            throw new IllegalArgumentException("Maximum token length is 8, but given length is " + token.length + ".");
        }
        this.token = token;
    }

    //获取字节编码的token，比如0x71在字节编码中为0111 0001，即1+16+32+64=113
    public byte[] getBytes(){
        return this.token;
    }

    public static String bytesToHex(byte[] bytes){
        //16进制字符串需要两倍的空间才可以存储原本8位字节存储的信息
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++){
            int v = bytes[i] & 0xFF; //1111 1111
            hexChars[i * 2] = hexArr[v >>> 4]; //逻辑右移4位 0000 ****
            hexChars[i * 2 + 1] = hexArr[v & 0x0F]; //掩码
            /*
            举个例子：
            byte[0] = 1+2+16+128 = 147
            1111 1111
            1001 0011
            & =
            1001 0011 v
            >>>4 =
            0000 1001 hexChars[i * 2] 高位

            1001 0011 v
            0000 1111 0x0F
            & =
            0000 0011 hexChars[i * 2 + 1] 低位
             */
        }
        return new String(hexChars);
    }

    @Override
    public String toString(){
        String tmp = bytesToHex(getBytes());

        if (tmp.length() == 0)
            return "<EMPTY>";
        else
            return "0x" + tmp;
    }

    @Override
    public boolean equals(Object object){
        if(object == null || (!(object instanceof Token)))
            return false;

        Token other = (Token)object;
        //比较基于数组内容返回的哈希值
        return Arrays.equals(this.getBytes(), other.getBytes());
    }

    //返回的就是对象存储位置的映像
    @Override
    public int hashCode(){
        return Arrays.hashCode(token);
    }

    @Override
    public int compareTo(Token other){
        if (other.equals(this))
            return 0;
        if (this.getBytes().length < other.getBytes().length)
            return -1;
        if (this.getBytes().length > other.getBytes().length)
            return 1;

        return UnsignedLongs.compare(Longs.fromByteArray(Bytes.concat(this.getBytes(), new byte[8])),
                Longs.fromByteArray(Bytes.concat(other.getBytes(), new byte[8])));
    }
}
