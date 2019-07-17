package com.netty_concurrency.netty.CoapNetty.Options;

import java.util.Arrays;

//option长度为0
//IF_NONE_MATCH
public final class EmptyOptionValue extends OptionValue<Void> {
    public EmptyOptionValue(int optionNum) throws IllegalArgumentException{
        //长度为0，所以byte[]长度为0，不用NULL是为了重用
        super(optionNum, new byte[0], false);
    }

    //空值
    @Override
    public Void getDecodedValue(){
        return null;
    }

    @Override
    public int hashCode(){
        return 0;
    }

    @Override
    public boolean equals(Object object){
        //先判定类型是否为empty
        if (!(object instanceof EmptyOptionValue))
            return false;

        //在判定内容是否一致
        EmptyOptionValue other = (EmptyOptionValue) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }
}
