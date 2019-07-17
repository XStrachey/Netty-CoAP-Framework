package com.netty_concurrency.netty.CoapNetty.Options;

import com.netty_concurrency.netty.CoapNetty.Coap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Locale;

//UTF-8编码字符串格式
//URI_HOST、LOCATION_PATH、URI_PATH、URI_QUERY、LOCATION_QUERY、PROXY_URI、PROXY_SCHEME
public class StringOptionValue extends OptionValue<String> {
    public StringOptionValue(int optionNum, byte[] value) throws IllegalArgumentException{
        this(optionNum, value, false);
    }

    public StringOptionValue(int optionNum, byte[] value, boolean allowDefault) throws IllegalArgumentException{
        super(optionNum, value, allowDefault);
    }

    public StringOptionValue(int optionNum, String value) throws IllegalArgumentException{
        this(optionNum, optionNum == Option.URI_HOST ?
                convertToByteArrayWithoutPercentEncoding(value.toLowerCase(Locale.CHINA)) :
                ((optionNum == Option.URI_PATH || optionNum == Option.URI_QUERY) ?
                        convertToByteArrayWithoutPercentEncoding(value) :
                        value.getBytes(Coap.CHARSET)));
    }

    //String(a,b)第一个参数为待转换字符串，第二个参数为目标字符串编码
    //这里目标字符串编码采用UTF-8，符合协议中optionValue的String类型编码
    @Override
    public String getDecodedValue(){
        return new String(value, Coap.CHARSET);
    }

    @Override
    public int hashCode(){
        return getDecodedValue().hashCode();
    }

    @Override
    public boolean equals(Object object){
        //先判定类型是否为String
        if (!(object instanceof StringOptionValue))
            return false;

        //再判定字串内容是否一致
        StringOptionValue other = (StringOptionValue) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }

    public static byte[] convertToByteArrayWithoutPercentEncoding(String s) throws IllegalArgumentException{
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(Coap.CHARSET));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int i;

        do {
            i = in.read();

            //-1 indicates end of stream
            if (i == -1) {
                break;
            }

            //0x25 = '%'
            if (i == 0x25) {
                //Character.digit returns the integer value encoded as in.read(). Since we know that percent encoding
                //uses bytes from 0x0 to 0xF (i.e. 0 to 15) the radix must be 16.
                int d1 = Character.digit(in.read(), 16);
                int d2 = Character.digit(in.read(), 16);

                if (d1 == -1 || d2 == -1) {
                    //Unexpected end of stream (at least one byte missing after '%')
                    throw new IllegalArgumentException("Invalid percent encoding in: " + s);
                }

                //Write decoded value to output stream (e.g. sequence [0x02, 0x00] results into byte 0x20
                out.write((d1 << 4) | d2);
            } else {
                out.write(i);
            }

        } while(true);

        byte[] result = out.toByteArray();

        return result;
    }
}
