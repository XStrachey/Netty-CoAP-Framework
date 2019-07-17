package com.netty_concurrency.netty.CoapNetty.Options;

import java.math.BigInteger;
import java.util.Arrays;

//option长度为非负整数，该值采用大端格式定义
//URI_PORT、OBSERVE、CONTENT_FORMAT、MAX_AGE、ACCEPT、BLOCK_2、BLOCK_1、SIZE_2、SIZE_1
public class UnitOptionValue extends OptionValue<Long> {

    public static final long UNDEFINED = -1;

    public UnitOptionValue(int optionNum, byte[] value) throws IllegalArgumentException{
        this(optionNum, shortenValue(value), false);
    }

    public UnitOptionValue(int optionNum, byte[] value, boolean allowDefault) throws IllegalArgumentException{
        super(optionNum, shortenValue(value), allowDefault);
    }

    @Override
    public Long getDecodedValue(){
        return new BigInteger(1, value).longValue();
    }

    @Override
    public int hashCode(){
        return getDecodedValue().hashCode();
    }

    @Override
    public boolean equals(Object object){
        //先判定类型是否为Unit
        if (!(object instanceof UnitOptionValue))
            return false;

        //再判定内容是否一致
        UnitOptionValue other = (UnitOptionValue) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }

    public static byte[] shortenValue(byte[] value){
        int index = 0;
        while (index < value.length - 1 && value[index] == 0)
            index++;

        return Arrays.copyOfRange(value, index, value.length);
    }
}
