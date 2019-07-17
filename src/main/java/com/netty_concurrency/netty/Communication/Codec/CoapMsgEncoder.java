package com.netty_concurrency.netty.Communication.Codec;

import com.google.common.primitives.Ints;
import com.netty_concurrency.netty.CoapNetty.*;
import com.netty_concurrency.netty.CoapNetty.Options.OptionValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.net.InetSocketAddress;

//在3.x时代，所有的I/O操作都会创建一个新的ChannelEvent对象,如下面的API
//void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception;
//void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception;
//而netty4.x里，为了避免频繁创建与回收ChannelEvent对象所造成的GC压力，上述两个处理所有类型事件的接口被改成了多个接口。
public class CoapMsgEncoder extends MessageToByteEncoder<Coap> {

    public static final int MAX_OPTION_DELTA = 65804;
    public static final int MAX_OPTION_LENGTH = 65804;

    @Override
    protected void encode(ChannelHandlerContext ctx, Coap coapMsg, ByteBuf out) throws OptionCodecException {
        //encode()被调用时将会传入将要被该类编码为ByteBuf的CoAP消息，该方法产生的ByteBuf即out将会在随后转发给ChannelPipeline中下一个ChannelOutboundHandler
        //编码头部（除options）
        encodeHeader(out, coapMsg);
        //EMPTY，空报文，只有首部没有负载
        //既不是请求也不是响应的消息
        //********0000********************
        if (coapMsg.getMsgCode() == MsgCode.EMPTY){
            ByteBuf byteBuf = Unpooled.wrappedBuffer(Ints.toByteArray(out.getInt(0) & 0xF0FFFFFF));
            out = byteBuf;
            return;
        }
        //是否含有options
        if (coapMsg.getAllOptions().size() == 0 && coapMsg.getContent().readableBytes() == 0){
            return;
        }
        //编码options
        encodeOptions(out, coapMsg);
        //编码负载
        if (coapMsg.getContent().readableBytes() > 0){
            //0xFF
            out.writeByte(255);
            //编码负载
            ByteBuf byteBuf = Unpooled.wrappedBuffer(out,coapMsg.getContent());
            out = byteBuf;
        }
    }

    protected void encodeHeader(ByteBuf buffer, Coap coapMsg) { //这里传入的buffer是encode的out
        //获取token
        byte[] token = coapMsg.getToken().getBytes();
        /*Ver,版本号
          00000000000000000000000000000001
         &00000000000000000000000000000011
          00000000000000000000000000000001
              <<30
          010000000000000000000000000000005
        */
        /*MsgT,报文类型
          000000000000000000000000000000**
         &00000000000000000000000000000011
          000000000000000000000000000000**
              <<28
            **0000000000000000000000000000
        */
        /*TKL,token长度
          0000000000000000000000000000****
         &00000000000000000000000000001111
          0000000000000000000000000000****
              <<24
              ****000000000000000000000000
        */
        /*Code,状态码
          000000000000000000000000********
         &00000000000000000000000011111111
          000000000000000000000000********
              <<16
                  ********0000000000000000
        */
        /*ID,消息编号
          0000000000000000****************
         &00000000000000001111111111111111
          0000000000000000****************

                  00000000****************
        */
        //encodeHeader是一个四字节的int
        int encodedHeader = ((coapMsg.getProtocolVersion() & 0x03) << 30)
                | ((coapMsg.getMsgType() & 0x03) << 28)
                | ((token.length & 0x0F) << 24)
                | ((coapMsg.getMsgCode() & 0xFF) << 16)
                | ((coapMsg.getMsgID() & 0xFFFF));

        buffer.writeInt(encodedHeader); //向out写头部
        //写token
        if (token.length > 0) {
            buffer.writeBytes(token); //向out写token
        }
    }

    protected void encodeOptions(ByteBuf buffer, Coap coapMsg) throws OptionCodecException {
        //编码options并将options添加到buf
        int previousOptionNum = 0;

        for(int optionNum : coapMsg.getAllOptions().keySet()) {
            for(OptionValue optionValue : coapMsg.getOptions(optionNum)) {
                encodeOption(buffer, optionNum, optionValue, previousOptionNum);
                previousOptionNum = optionNum;
            }
        }
    }

    //CoAP定义了一系列选项用以规范CoAP报文的格式
    //一个CoAP报文可以包含多个CoAP选项
    //CoAP选项实例由选项偏移量（Option Delta），选项长度（Option Length），选项值（Option Value）组成
    //CoAP中不能直接确定选项编号，选项编号必须由上一个选项编号和本次选项偏移量计算得到
    /*选项格式
            |————|————|
               delta  length
            |————|————|
              delta extended
            |————|————|
             length extended
            |————|————|
                  value
            |————|————|
    */
    protected void encodeOption(ByteBuf buffer, int optionNum, OptionValue optionValue, int prevNum) throws OptionCodecException {
        //前驱option号必须小于等于当前option
        if (prevNum > optionNum) {
            throw new OptionCodecException(optionNum);
        }
        //计算选项偏移量
        //选项偏移量为4位无符号整数，其中0~12用于指示选项的偏移量，13、14、15有特殊含义
        int optionDelta = optionNum - prevNum;
        //选项长度为4位无符号整数，其中0~12用于指定选项长度，13、14、15具有特殊含义
        int optionLength = optionValue.getValue().length;

        if (optionLength > MAX_OPTION_LENGTH) {
            throw new OptionCodecException(optionNum);
        }

        if (optionDelta > MAX_OPTION_DELTA) {
            throw new OptionCodecException(optionNum);
        }
        //0~12用于指示选项的偏移量
        if (optionDelta < 13) {
            //0~12用于指定选项长度
            if (optionLength < 13) {
                /*假设偏移量为4，选项长度为11
                1111 1111------1111 1111
                0000 0100------1111 1011
                &
                0000 0100------1111 1011
                <<4
                0100 0000
                ------------------------
                0100 0000   |  1111 1011
                1111 1011
                 */
                buffer.writeByte(((optionDelta & 0xFF) << 4) | (optionLength & 0xFF));
            } else if (optionLength < 269) {
                buffer.writeByte(((optionDelta << 4) & 0xFF) | (13 & 0xFF));
                buffer.writeByte((optionLength - 13) & 0xFF);
            } else {
                buffer.writeByte(((optionDelta << 4) & 0xFF) | (14 & 0xFF));
                buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionLength - 269) & 0xFF);
            }
        } else if (optionDelta < 269) {
            //13 <= option delta < 269，即偏移量为13，扩展偏移量区域定义一个8位无符号整数，此时选项偏移量最大为255+13
            /*选项格式
            |————|————|
               delta  length
            |————|————|
              delta extended
            |————|————|
             length extended
            |————|————|
                  value
            |————|————|
             */
            if (optionLength < 13) {
                buffer.writeByte(((13 & 0xFF) << 4) | (optionLength & 0xFF));
                buffer.writeByte((optionDelta - 13) & 0xFF);
            } else if (optionLength < 269) {
                buffer.writeByte(((13 & 0xFF) << 4) | (13 & 0xFF));
                buffer.writeByte((optionDelta - 13) & 0xFF);
                buffer.writeByte((optionLength - 13) & 0xFF);
            } else {
                buffer.writeByte((13 & 0xFF) << 4 | (14 & 0xFF));
                buffer.writeByte((optionDelta - 13) & 0xFF);
                buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionLength - 269) & 0xFF);
            }
        } else {
            //269 <= option delta < 65805，即偏移量为14，扩展偏移量区域定义一个16位无符号整数，此时选项偏移量最大为2^16-1+269
            /*选项格式
            |————|————|
               delta  length
            |————|————|
              delta extended
            |————|————|
             length extended
            |————|————|
                  value
            |————|————|
             */
            if (optionLength < 13) {
                buffer.writeByte(((14 & 0xFF) << 4) | (optionLength & 0xFF));
                buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionDelta - 269) & 0xFF);
            } else if (optionLength < 269) {
                buffer.writeByte(((14 & 0xFF) << 4) | (13 & 0xFF));
                buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionDelta - 269) & 0xFF);
                buffer.writeByte((optionLength - 13) & 0xFF);
            } else {
                buffer.writeByte(((14 & 0xFF) << 4) | (14 & 0xFF));
                buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionDelta - 269) & 0xFF);
                buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionLength - 269) & 0xFF);
            }
        }
        //偏移量为15保留为将来所用
        //写option value
        buffer.writeBytes(optionValue.getValue());
    }

    private void sendInternalEncodingFailedMessage(ChannelHandlerContext ctx, InetSocketAddress remoteSocket, int msgID, Token token, Throwable cause) {
        String desc = cause.getMessage() == null ? "Encoder (" + cause.getClass().getName() + ")" : cause.getMessage();
        //Miss event = new MiscellaneousErrorEvent(remoteSocket, messageID, token, desc);
        //Channel.fireMessageReceived(ctx, event);
        //触发对下一个ChannelInboundHandler上的fireExceptionCaught的方法调用
        ctx.fireExceptionCaught(cause);
    }
}
