package com.netty_concurrency.netty.CoapNetty.Options;

import java.util.Arrays;

//option长度不确定
//IF_MATCH、ETAG、ENDPOINT_ID_1、ENDPOINT_ID_2
public class OpaqueOptionValue extends OptionValue<byte[]> {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public OpaqueOptionValue(int optionNum, byte[] value) throws IllegalArgumentException{
        super(optionNum, value, false);
    }

    //在基类中，optionvalue也是用字节数组存储的
    @Override
    public byte[] getDecodedValue(){
        return this.value;
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(getDecodedValue());
    }

    @Override
    public boolean equals(Object object){
        //先判定类型是否为opaque
        if (!(object instanceof OpaqueOptionValue))
            return false;

        //再判定内容是否一致
        OpaqueOptionValue other = (OpaqueOptionValue) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }

    @Override
    public String toString(){
        return toHexString(this.value);
    }

    public static String toHexString(byte[] bytes){
        //因为长度不确定，所以需要考虑长度为0的情况
        //与empty不同的是对应的option集合不一样，而且在这事先对这长度是一无所知的
        if (bytes.length == 0)
            return "<empty>";
        else
            return "0x" + bytesToHex(bytes);
    }

    //可参考Token类
    private static String bytesToHex(byte[] bytes){
        //16进制字符串需要两倍的空间才可以存储原本8位字节存储的信息
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4]; //逻辑右移4位 0000 ****
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]; //掩码
        }
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
        return new String(hexChars);
    }
}
