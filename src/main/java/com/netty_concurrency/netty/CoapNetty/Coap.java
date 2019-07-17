package com.netty_concurrency.netty.CoapNetty;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.primitives.Longs;
import com.netty_concurrency.netty.CoapNetty.Options.*;
import io.netty.buffer.*;

import java.nio.charset.Charset;
import java.util.*;

public abstract class Coap {
    //coap版本信息 占用2位，当前为01
    public static final int PROTOCOL_VERSION = 1;

    //确保char占2字节，保证token中的byte[]转char[]符合预期
    //在optionValue的String类型操作中还有用
    //确保一字节8位
    public static final Charset CHARSET = Charset.forName("utf-8");

    //默认分配的msgID
    //每个消息一个ID，重发的消息MID不变
    public static final int UNDEFINED_MESSAGE_ID = -1;

    //TKL的最大允许值，TKL当前支持0~8B，其他长度保留将来扩展用，TKL占4位
    public static final int MAX_TOKEN_LENGTH = 8;

    private static final String WRONG_OPTION_TYPE = "wrong option";

    private static final String OPTION_NOT_ALLOWED_WITH_MESSAGE_TYPE = "not allowed message type";

    private static final String OPTION_ALREADY_SET = "option already set";

    private static final String DOES_NOT_ALLOW_CONTENT = "message with code do not allow payload";

    private static final String EXCLUDES = "excludes";

    //消息类型 占用2位，包括CON，NON,ACK,RES
    /*
    CON类型的消息为主动发出消息请求，并且需要接收方作出回复
    NON类型的消息为主动发出消息请求，但是不需要接收方作出回复
    ACK类型的消息为接收方作出回复
    Res类型为发出CON消息后，在还没收到请求时，主动通知不需要再回复
     */
    private int msgType;

    //状态码 占用1字节，分为：发送和接收
    /*
    发送方状态码：EMPTY,GET,POST,PUT,DELETE
     */
    //一般EMPTY只在 消息类型 为 RES 时才会用到
    private int msgCode;

    //消息编号 占用2字节，代表了该消息的编号
    //采用大端格式描述
    //因为传输层协议采用UDP，所以规定一组对应的coap请求和响应必须采用相同的msgID
    //用于Message的重复性检测，以及CON，NON和ACK，RST的匹配
    //如果是CON类型的消息，在返回时消息编号也应当与发送时相同
    private int msgID;

    //Token的字节数由token length,即TKL决定
    private Token token;

    //请求消息与回应消息都可以有0..*个option
    //option主要用于描述请求或相应对应的各个属性，类似于参数或特征描述
    protected SetMultimap<Integer, OptionValue> options;

    //payload，由payload Marker标识领起，payload Marker为0xFF
    private static final ByteBufAllocator ALLOC = ByteBufAllocator.DEFAULT; //池化
    private ByteBuf content = ALLOC.directBuffer(0); //分配一个直接内存的ByteBuf

    protected Coap(int msgType, int msgCode, int msgID, Token token) throws IllegalArgumentException{
        if (!MsgType.isMsgType(msgType))
            throw new IllegalArgumentException(msgType + "don't match any message type");

        if (!MsgCode.isMsgCode(msgCode))
            throw new IllegalArgumentException(msgCode + "don't match any message code");

        this.setMsgType(msgType);
        this.setMsgID(msgCode);
        this.setMsgID(msgID);
        this.setToken(token);

        this.options = Multimaps.newSetMultimap(new TreeMap<Integer, Collection<OptionValue>>(),
                LinkedHashSetSuppiler.getInstance());
    }

    private final static class LinkedHashSetSuppiler implements Supplier<LinkedHashSet<OptionValue>>{
        public static LinkedHashSetSuppiler instance = new LinkedHashSetSuppiler();

        private LinkedHashSetSuppiler(){}

        public static LinkedHashSetSuppiler getInstance(){
            return instance;
        }

        @Override
        public LinkedHashSet<OptionValue> get(){
            return new LinkedHashSet<>();
        }
    }

    protected Coap(int msgType, int msgCode) throws IllegalArgumentException{
        this(msgType, msgCode, UNDEFINED_MESSAGE_ID, new Token(new byte[0]));
    }

    public static Coap createEmptyRst(int msgID) throws IllegalArgumentException{
        return new Coap(MsgType.RST, MsgCode.EMPTY, msgID, new Token(new byte[0])){};
    }

    public static Coap createEmptyAck(int msgID) throws IllegalArgumentException{
        return new Coap(MsgType.ACK, MsgCode.EMPTY, msgID, new Token(new byte[0])){};
    }

    public static Coap createPing(int msgID) throws IllegalArgumentException{
        return new Coap(MsgType.CON, MsgCode.EMPTY, msgID, new Token(new byte[0])){};
    }

    //判断是否为ping或请求或响应
    public boolean isPing(){
        return this.msgCode == MsgCode.EMPTY && this.msgType == MsgType.CON;
    }

    public boolean isRequest(){
        return MsgCode.isRequest(this.getMsgCode());
    }

    public boolean isResponse(){
        return MsgCode.isResponse(this.getMsgCode());
    }

    //对option的设置和添加
    private void checkOptionPermission(int optionNum) throws IllegalArgumentException{
        Option.Occurence permittedOccurence = Option.getPermittedOccurrence(optionNum, this.msgCode);
        if (permittedOccurence == Option.Occurence.NONE){
            throw new IllegalArgumentException(String.format(OPTION_NOT_ALLOWED_WITH_MESSAGE_TYPE,
                    optionNum, Option.asString(optionNum), this.getMsgCodeName()));
        } else if (options.containsKey(optionNum) && permittedOccurence == Option.Occurence.ONCE){
            throw new IllegalArgumentException(String.format(OPTION_ALREADY_SET, optionNum));
        }
    }

    public void addOption(int optionNum, OptionValue optionVal) throws IllegalArgumentException{
        this.checkOptionPermission(optionNum);

        for (int containedOption : options.keySet()){
            if (Option.mutuallyExludes(containedOption, optionNum))
                throw new IllegalArgumentException(String.format(EXCLUDES, containedOption,optionNum));

            options.put(optionNum, optionVal);
        }
    }

    protected void addStringOption(int optionNum, String value) throws IllegalArgumentException{
        if (!(OptionValue.getType(optionNum) == OptionValue.Type.STRING))
            throw new IllegalArgumentException(String.format(WRONG_OPTION_TYPE, optionNum));

        StringOptionValue option = new StringOptionValue(optionNum, value);
        addOption(optionNum, option);
    }

    protected void addUintOption(int optionNum, long value) throws IllegalArgumentException {

        if (!(OptionValue.getType(optionNum) == OptionValue.Type.UINT))
            throw new IllegalArgumentException(String.format(WRONG_OPTION_TYPE, optionNum, OptionValue.Type.STRING));

        byte[] byteValue = Longs.toByteArray(value);
        int index = 0;
        while(index < byteValue.length && byteValue[index] == 0) {
            index++;
        }
        UnitOptionValue option = new UnitOptionValue(optionNum, Arrays.copyOfRange(byteValue, index, byteValue.length));
        addOption(optionNum, option);
    }

    protected void addEmptyOption(int optionNum) throws IllegalArgumentException {
        if (!(OptionValue.getType(optionNum) == OptionValue.Type.EMPTY))
            throw new IllegalArgumentException(String.format(WRONG_OPTION_TYPE, optionNum, OptionValue.Type.EMPTY));

        options.put(optionNum, new EmptyOptionValue(optionNum));
    }

    protected void addOpaqueOption(int optionNum, byte[] value) throws IllegalArgumentException{
        if (!(OptionValue.getType(optionNum) == OptionValue.Type.OPAQUE))
            throw new IllegalArgumentException(String.format(WRONG_OPTION_TYPE, optionNum, OptionValue.Type.OPAQUE));

        OpaqueOptionValue option = new OpaqueOptionValue(optionNum, value);
        addOption(optionNum, option);
    }

    public int removeOptions(int optionNumber) {
        int result = options.removeAll(optionNumber).size();
        return result;
    }

    private static long extractBits(final long value, final int bits, final int offset) {
        final long shifted = value >>> offset;
        final long masked = (1L << bits) - 1L;
        return shifted & masked;
    }

    //版本号，消息类型，消息类型名，消息码，消息码名，消息ID，token的get、set操作
    public int getProtocolVersion(){
        return PROTOCOL_VERSION;
    }

    public int getMsgType(){
        return msgType;
    }

    public String getMsgTypeName(){
        return MsgType.asString(this.msgType);
    }

    public int getMsgCode(){
        return msgCode;
    }

    public String getMsgCodeName(){
        return MsgCode.asString(this.msgCode);
    }

    public int getMsgID(){
        return msgID;
    }

    public Token getToken(){
        return this.token;
    }

    public void setMsgType(int msgType) throws IllegalArgumentException{
        if (!MsgType.isMsgType(msgType))
            throw new IllegalArgumentException("Invalid message type " + msgType);

        this.msgType = msgType;
    }

    public void setMsgCode(int msgCode) throws InterruptedException{
        if (!MsgCode.isMsgCode(msgCode))
            throw new IllegalArgumentException("Invalid message code " + msgCode);

        this.msgCode = msgCode;
    }

    public void setMsgID(int msgID) throws IllegalArgumentException{
        if (msgID < -1 || msgID > 65535)
            throw new IllegalArgumentException(msgID + " is either negative or greater than 65535");

        this.msgID = msgID;
    }

    public void setToken(Token token){
        this.token = token;
    }

    //以下是负载的获取
    public int getContentLength() {
        return this.content.readableBytes();
    }

    public ByteBuf getContent() {
        return this.content;
    }

    public byte[] getContentAsByteArray() {
        byte[] result = new byte[this.getContentLength()];
        this.getContent().readBytes(result, 0, this.getContentLength());
        return result;
    }

    public void setContent(ByteBuf content) throws IllegalArgumentException{
        if (!(MsgCode.allowsContent(this.msgCode)) && content.readableBytes() > 0){
            throw new IllegalArgumentException(String.format(DOES_NOT_ALLOW_CONTENT, this.getMsgCodeName()));
        }

        this.content = content;
    }

    public void setContent(ByteBuf content, long contentFormat) throws IllegalArgumentException{
        try{
            this.addUintOption(Option.CONTENT_FORMAT, contentFormat);
            setContent(content);
        } catch (IllegalArgumentException e){
            this.content = ALLOC.directBuffer(0);
            this.removeOptions(Option.CONTENT_FORMAT);
            throw e;
        }
    }

    public void setContent(byte[] content) throws IllegalArgumentException {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(content); //利用netty零拷贝机制
        //包装这个 byte 数组, 生成一个新的 ByteBuf 实例, 而不需要进行拷贝操作
        setContent(byteBuf);
    }

    public void setContent(byte[] content, long contentFormat) throws IllegalArgumentException {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(content);
        setContent(byteBuf, contentFormat);
    }

    public SetMultimap<Integer, OptionValue> getAllOptions(){
        return this.options;
    }

    public void setAllOptions (SetMultimap<Integer, OptionValue> options){
        this.options = options;
    }

    public Set<OptionValue> getOptions(int optionNum){
        return this.options.get(optionNum);
    }

    @Override
    public int hashCode(){
        return toString().hashCode() + content.hashCode();
    }

    @Override
    public boolean equals(Object object){

        if (!(object instanceof Coap)){
            return false;
        }

        Coap other = (Coap) object;

        //对比头部
        //版本号
        if (this.getProtocolVersion() != other.getProtocolVersion())
            return false;
        //消息类型
        if (this.getMsgType() != other.getMsgType())
            return false;
        //消息码
        if (this.getMsgCode() != other.getMsgCode())
            return false;
        //消息ID
        if (this.getMsgID() != other.getMsgID())
            return false;
        //token
        if (!this.getToken().equals(other.getToken()))
            return false;

        //添加消息中的option
        Iterator<Map.Entry<Integer, OptionValue>> iterator1 = this.getAllOptions().entries().iterator();
        Iterator<Map.Entry<Integer, OptionValue>> iterator2 = other.getAllOptions().entries().iterator();
        //对比option
        while(iterator1.hasNext()) {
            //检查option数量是否一致
            //iterator1更多
            if (!iterator2.hasNext())
                return false;

            Map.Entry<Integer, OptionValue> entry1 = iterator1.next();
            Map.Entry<Integer, OptionValue> entry2 = iterator2.next();

            if (!entry1.getKey().equals(entry2.getKey()))
                return false;

            if (!entry1.getValue().equals(entry2.getValue()))
                return false;
        }

        //检查option数量是否一致
        //iterator2更多
        if (iterator2.hasNext())
            return false;

        //对比负载
        return this.getContent().equals(other.getContent());
    }

    @Override
    public String toString() {
        StringBuffer result =  new StringBuffer();

        //Header + Token
        result.append("[Header: (V) " + getProtocolVersion() + ", (T) " + getMsgTypeName() + ", (TKL) "
                + token.getBytes().length + ", (C) " + getMsgCodeName() + ", (ID) " + getMsgID() + " | (Token) "
                + token + " | ");

        //Options
        result.append("Options:");
        for(int optionNumber : getAllOptions().keySet()) {
            result.append(" (No. " + optionNumber + ") ");
            Iterator<OptionValue> iterator = this.getOptions(optionNumber).iterator();
            OptionValue optionValue = iterator.next();
            result.append(optionValue.toString());
            while(iterator.hasNext())
                result.append(" / " + iterator.next().toString());
        }
        result.append(" | ");

        //Content
        result.append("Content: ");
        long payloadLength = getContent().readableBytes();
        if (payloadLength == 0)
            result.append("<no content>]");
        else
            result.append(getContent().toString(0, Math.min(getContent().readableBytes(), 20), Coap.CHARSET)
                    + "... ( " + payloadLength + " bytes)]");

        return result.toString();
    }

    private final static class LinkedHashSetSupplier implements Supplier<LinkedHashSet<OptionValue>> {
        public static LinkedHashSetSupplier instance = new LinkedHashSetSupplier();

        private LinkedHashSetSupplier() {}

        public static LinkedHashSetSupplier getInstance() {
            return instance;
        }

        @Override
        public LinkedHashSet<OptionValue> get() {
            return new LinkedHashSet<>();
        }
    }
}
