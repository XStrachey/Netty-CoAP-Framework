package com.netty_concurrency.netty.Communication.Codec;

import com.netty_concurrency.netty.CoapNetty.*;
import com.netty_concurrency.netty.CoapNetty.Options.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

//还差从UDP报文中读取远程地址的部分
public class CoapMsgDecoder extends MessageToMessageDecoder<DatagramPacket> {

    protected void decode(ChannelHandlerContext ctx, DatagramPacket coapMsgPacketedByUDP, List<Object> out) throws HeaderDecodingException, OptionCodecException {
        //获取UDP报文内容，即CoAP协议报文
        ByteBuf data = coapMsgPacketedByUDP.content();
        //InetAddress一般这样使用：其中不可以包含端口
        InetAddress remoteAddress = coapMsgPacketedByUDP.sender().getAddress();
        int remotePort = coapMsgPacketedByUDP.sender().getPort();
        //InetSocketAddress可以实现地址＋端口。在需要设置连接超时时间的场合，必须使用InetSocketAddress
        InetSocketAddress remoteSocketAddress = new InetSocketAddress(remoteAddress,remotePort);
        //CoAP最小长度
        if (data.readableBytes() < 4){
            String msg = "Encoded CoAP messages MUST have min. 4 bytes. This has " + data.readableBytes();
            throw new HeaderDecodingException(Coap.UNDEFINED_MESSAGE_ID, remoteSocketAddress, msg);
        }
        //解码头部
        int encodedHeader = data.readInt();
        int version =     (encodedHeader >>> 30) & 0x03;
        int msgType = (encodedHeader >>> 28) & 0x03;
        int tokenLength = (encodedHeader >>> 24) & 0x0F;
        int msgCode = (encodedHeader >>> 16) & 0xFF;
        int msgID =   (encodedHeader)        & 0xFFFF;
        //检查版本号，当前版本号固定为01
        if (version != Coap.PROTOCOL_VERSION) {
            String message = "CoAP version (" + version + ") is other than \"1\"!";
            throw new HeaderDecodingException(msgID, remoteSocketAddress, message);
        }
        //检查TKL是否符合长度要求
        if (tokenLength > Coap.MAX_TOKEN_LENGTH) {
            String message = "TKL value (" + tokenLength + ") is larger than 8!";
            throw new HeaderDecodingException(msgID, remoteSocketAddress, message);
        }
        if (data.readableBytes() < tokenLength) {
            String message = "TKL value is " + tokenLength + " but only " + data.readableBytes() + " bytes left!";
            throw new HeaderDecodingException(msgID, remoteSocketAddress, message);
        }
        //检查是否为EMPTY消息
        if (msgCode == MsgCode.EMPTY) {

            if (msgType == MsgType.ACK) {
                out.add(Coap.createEmptyAck(msgID));
            } else if (msgType == MsgType.RST) {
                out.add(Coap.createEmptyRst(msgID));
            } else if (msgType == MsgType.CON) {
                out.add(Coap.createPing(msgID));
            } else {
                //缺少已定义的空消息
                throw new HeaderDecodingException(msgID, remoteSocketAddress, "Empty NON messages are invalid!");
            }
        }
        //读取token
        byte[] token = new byte[tokenLength];
        data.readBytes(token);

        Coap coapMsg;

        if(MsgCode.isRequest(msgCode)){
            coapMsg = new CoapRequest(msgType,msgCode);
        }else{
            coapMsg = new CoapResponse(msgType, msgCode);
            coapMsg.setMsgType(msgType);
        }

        coapMsg.setMsgID(msgID);
        coapMsg.setToken(new Token(token));

        if(data.readableBytes() > 0){
            try{
                setOptions(coapMsg, data);
            } catch (OptionCodecException ex){
                ex.setMsgID(msgID);
                ex.setToken(new Token(token));
                ex.setremoteSocket(remoteSocketAddress);
                ex.setMsgType(msgType);
                throw ex;
            }
        }

        data.discardReadBytes();

        try{
            coapMsg.setContent(data);
        } catch(IllegalArgumentException e){
            String warning = "Msg code {} does not allow content.";
        }

        out.add(coapMsg);
    }

    private void setOptions(Coap coapMsg, ByteBuf buffer) throws OptionCodecException {
        //Decode the options
        int previousOptionNumber = 0;
        //这些&操作都是为了让字节显为位
        int firstByte = buffer.readByte() & 0xFF;
        //Options应该在0xFF前面
        while(firstByte != 0xFF && buffer.readableBytes() >= 0) {
            /*
            **** ****
            1111 0000
            **** 0000
            >>>4
            0000 ****
             */
            int optionDelta =   (firstByte & 0xF0) >>> 4;
            /*
            **** ****
            0000 1111
            0000 ****
             */
            int optionLength =   firstByte & 0x0F;
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
            if (optionDelta == 13) {
                //选项偏移量为13，偏移量扩展区域定义一个8位无符号数，所以再读进一个字节
                //这里根据《IoT开发实战——CoAP卷》来看，偏移量应该还需要增加13
                optionDelta += buffer.readByte() & 0xFF;
            } else if (optionDelta == 14) {
                //选项偏移量为14，偏移量扩展区域定义一个16位无符号整数，所以需要读进两个字节
                //第一个字节作为高8位，第二个读进字节作为低8位
                optionDelta = 269 + ((buffer.readByte() & 0xFF) << 8) + (buffer.readByte() & 0xFF);
            }

            if (optionLength == 13) {
                //选项长度的扩展计算和选项偏移量是一样的
                optionLength += buffer.readByte() & 0xFF;
            } else if (optionLength == 14) {
                optionLength = 269 + ((buffer.readByte() & 0xFF) << 8) + (buffer.readByte() & 0xFF);
            }
            //选项编号必须由上一个选项比编号和本次选项偏移量计算得到
            int actualOptionNumber = previousOptionNumber + optionDelta;

            try {
                byte[] optionValue = new byte[optionLength];
                //顺序处理下来，这里读入的是选项值
                buffer.readBytes(optionValue);
                //选项的数据类型有4种，这可以参考OptionValue中的设定
                switch(OptionValue.getType(actualOptionNumber)) {
                    case EMPTY: {
                        EmptyOptionValue value = new EmptyOptionValue(actualOptionNumber);
                        coapMsg.addOption(actualOptionNumber, value);
                        break;
                    }
                    case OPAQUE: {
                        OpaqueOptionValue value = new OpaqueOptionValue(actualOptionNumber, optionValue);
                        coapMsg.addOption(actualOptionNumber, value);
                        break;
                    }
                    case STRING: {
                        StringOptionValue value = new StringOptionValue(actualOptionNumber, optionValue, true);
                        coapMsg.addOption(actualOptionNumber, value);
                        break;
                    }
                    case UINT: {
                        UnitOptionValue value = new UnitOptionValue(actualOptionNumber, optionValue, true);
                        coapMsg.addOption(actualOptionNumber, value);
                        break;
                    }
                    default: {
                        throw new RuntimeException("This should never happen!");
                    }
                }
            } catch (IllegalArgumentException e) {
                //failed option creation leads to an illegal argument exception
                if (MsgCode.isResponse(coapMsg.getMsgCode())) {
                    //Malformed options in responses are silently ignored...
                } else if (Option.isCritical(actualOptionNumber)) {
                    //Critical malformed options in requests cause an exception
                    throw new OptionCodecException(actualOptionNumber);
                } else {
                    //Not critical malformed options in requests are silently ignored...
                }
            }
            //为读取下一个选项做准备
            previousOptionNumber = actualOptionNumber;

            if (buffer.readableBytes() > 0) {
                firstByte = buffer.readByte() & 0xFF;
            } else {
                // this is necessary if there is no payload and the last option is empty (e.g. UintOption with value 0)
                //如果没有负载
                firstByte = 0xFF;
            }
        }
    }

    private static String toBinaryString(int byteValue) {
        StringBuilder buffer = new StringBuilder(8);

        for(int i = 7; i >= 0; i--) {
            if ((byteValue & (int) Math.pow(2, i)) > 0) {
                buffer.append("1");
            } else {
                buffer.append("0");
            }
        }
        return buffer.toString();
    }

    private void writeReset(ChannelHandlerContext ctx, int msgID, InetSocketAddress remoteSocket, ChannelPromise promise) {
        Coap resetMessage = Coap.createEmptyRst(msgID);
        //Channel.write(ctx, Channels.future(ctx.getChannel()), resetMessage, remoteSocket);
        Channel channel = ctx.channel();
        channel.writeAndFlush(resetMessage, promise);
        //remoteSocket还是交给下一个handler吧
    }


    private void writeBadOptionResponse(ChannelHandlerContext ctx, int msgType, int msgID,
                                        Token token, InetSocketAddress remoteSocket, String content, ChannelPromise promise) {

        CoapResponse errorResponse = CoapResponse.createErrorResponse(msgType, MsgCode.BAD_OPTION_402, content);
        errorResponse.setMsgID(msgID);
        errorResponse.setToken(token);

        //Channels.write(ctx, Channels.future(ctx.getChannel()), errorResponse, remoteSocket);
        Channel channel = ctx.channel();
        channel.writeAndFlush(errorResponse, promise);
    }
}
