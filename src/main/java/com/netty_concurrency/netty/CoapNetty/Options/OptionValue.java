package com.netty_concurrency.netty.CoapNetty.Options;

import com.google.common.net.InetAddresses;
import com.google.common.primitives.Longs;
import com.netty_concurrency.netty.CoapNetty.Coap;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

//OptionValue支持四种类型，先用抽象类型T
public abstract class OptionValue<T> {

    private static final String UNKNOWN_OPTION = "UNKONWN OPTION";
    private static final String VALUE_IS_DEFAULT_VALUE = "GIVEN VALUE IS DEFAULT VALUE";
    private static final String OUT_OF_ALLOWED_RANGE = "GIVEN VALUE IS OUT OF ALLOWED VALUE";

    //coap选项值包括四种数据类型
    //empty，选项值长度为0
    //string，utf-8编码字符串格式
    //unit，选项值长度为非负整数，该值采用大端格式定义
    //opaque，选项值长度不确定
    public static enum Type{
        EMPTY, STRING, UINT, OPAQUE
    }

    public static final long MAX_AGE_DEFAULT = 60;

    public static final long MAX_AGE_MAX = 0xffffffffL;

    public static final byte[] ENCODED_MAX_AGE_DEFAULT = new BigInteger(1, Longs.toByteArray((MAX_AGE_DEFAULT))).toByteArray();
    //默认端口号
    public static final long URI_PORT_DEFAULT = 5683;

    public static final byte[] ENCODED_URI_PORT_DEFAULT = new BigInteger(1, Longs.toByteArray(URI_PORT_DEFAULT)).toByteArray();

    //封装一个内部类作为option特征存储操作结构
    private static class Characteristics{
        private Type type;
        private int minLength;
        private int maxLength;

        //option特征：数据类型，最小长度，最大长度
        private Characteristics(Type type, int minLength, int maxLength){
            this.type = type;
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        public Type getType(){
            return type;
        }

        public int getMinLength(){
            return minLength;
        }

        public int getMaxLength(){
            return maxLength;
        }
    }

    //指定了option各项数据类型、长度范围
    private static HashMap<Integer, Characteristics> CHARACTERISTICS = new HashMap<>();
    static {
        CHARACTERISTICS.put(Option.IF_MATCH, new Characteristics(Type.OPAQUE, 0, 8));
        CHARACTERISTICS.put(Option.URI_HOST, new Characteristics(Type.STRING, 1,  255 ));
        CHARACTERISTICS.put(Option.ETAG, new Characteristics(Type.OPAQUE,  1,    8 ));
        CHARACTERISTICS.put(Option.IF_NONE_MATCH, new Characteristics(Type.EMPTY,   0,    0 ));
        CHARACTERISTICS.put(Option.URI_PORT, new Characteristics(Type.UINT,    0,    2 ));
        CHARACTERISTICS.put(Option.LOCATION_PATH, new Characteristics(Type.STRING,  0,  255 ));
        CHARACTERISTICS.put(Option.OBSERVE, new Characteristics(Type.UINT,    0,    3 ));
        CHARACTERISTICS.put(Option.URI_PATH, new Characteristics(Type.STRING,  0,  255 ));
        CHARACTERISTICS.put(Option.CONTENT_FORMAT, new Characteristics(Type.UINT,    0,    2 ));
        CHARACTERISTICS.put(Option.MAX_AGE, new Characteristics(Type.UINT,    0,    4 ));
        CHARACTERISTICS.put(Option.URI_QUERY, new Characteristics(Type.STRING,  0,  255 ));
        CHARACTERISTICS.put(Option.ACCEPT, new Characteristics(Type.UINT,    0,    2 ));
        CHARACTERISTICS.put(Option.LOCATION_QUERY, new Characteristics(Type.STRING,  0,  255 ));
        CHARACTERISTICS.put(Option.BLOCK_2, new Characteristics(Type.UINT,    0,    3 ));
        CHARACTERISTICS.put(Option.BLOCK_1, new Characteristics(Type.UINT,    0,    3 ));
        CHARACTERISTICS.put(Option.SIZE_2, new Characteristics(Type.UINT,    0,    4 ));
        CHARACTERISTICS.put(Option.PROXY_URI, new Characteristics(Type.STRING,  1, 1034 ));
        CHARACTERISTICS.put(Option.PROXY_SCHEME, new Characteristics(Type.STRING,  1,  255 ));
        CHARACTERISTICS.put(Option.SIZE_1, new Characteristics(Type.UINT,    0,    4 ));
        CHARACTERISTICS.put(Option.ENDPOINT_ID_1, new Characteristics(Type.OPAQUE,  0,    8 ));
        CHARACTERISTICS.put(Option.ENDPOINT_ID_2, new Characteristics(Type.OPAQUE,  0,    8 ));
    }

    //获取给定option的类型
    public static Type getType(int optionNum) throws IllegalArgumentException{
        Characteristics characteristics = CHARACTERISTICS.get(optionNum);
        if (characteristics == null)
            throw new IllegalArgumentException(String.format(UNKNOWN_OPTION, optionNum));
        else return characteristics.getType();
    }

    //获取给定option的最小长度
    public static int getMinLength(int optionNum) throws IllegalArgumentException{
        Characteristics characteristics = CHARACTERISTICS.get(optionNum);
        if (characteristics == null)
            throw new IllegalArgumentException(String.format(UNKNOWN_OPTION, optionNum));
        else return characteristics.getMinLength();
    }

    //获取给定option的最大长度
    public static int getMaxLength(int optionNum) throws IllegalArgumentException{
        Characteristics characteristics = CHARACTERISTICS.get(optionNum);
        if (characteristics == null)
            throw new IllegalArgumentException(String.format(UNKNOWN_OPTION, optionNum));
        else return characteristics.getMaxLength();
    }

    //判断option是否有采用默认值
    public static boolean isDefaultValue(int optionNum, byte[] value){
        if (optionNum == Option.URI_PORT && Arrays.equals(value, ENCODED_URI_PORT_DEFAULT)){
            return true;
        } else if (optionNum == Option.MAX_AGE && Arrays.equals(value, ENCODED_MAX_AGE_DEFAULT)){
            return true;
        } else if (optionNum == Option.URI_HOST){
            String hostName = new String(value, Coap.CHARSET);
            if (hostName.startsWith("[") && hostName.endsWith("]")){
                hostName = hostName.substring(1, hostName.length() - 1);
            }

            if (InetAddresses.isInetAddress(hostName)){
                return true;
            }
        }

        return false;
    }

    protected byte[] value;

    //带默认值允许否
    //会在子类中具体化，现在只提供基本约束
    protected OptionValue(int optionNum, byte[] value, boolean allowDefault) throws IllegalArgumentException{
        //若不允许默认值option
        if (!allowDefault && OptionValue.isDefaultValue(optionNum, value)){
            throw new IllegalArgumentException(String.format(VALUE_IS_DEFAULT_VALUE, optionNum));
        }
        //检验给定option值是否不在允许数值范围内
        if (getMinLength(optionNum) > value.length || getMaxLength(optionNum) < value.length){
            throw new IllegalArgumentException(String.format(OUT_OF_ALLOWED_RANGE, value.length, optionNum, getMinLength(optionNum), getMaxLength(optionNum)));
        }

        //option对应的值，……这个说的有点绕，大概就是option指代了这个option是干嘛的，而现在这个value就是这么个指令的参数内容
        this.value = value;
    }

    //获取option“参数值”
    public byte[] getValue(){ return this.value;}

    public abstract T getDecodedValue();

    public abstract int hashCode();

    public abstract boolean equals(Object object);

    public String toString(){
        return " " + this.getValue();
    }
}
