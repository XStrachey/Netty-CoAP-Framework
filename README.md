# Netty-CoAP-Framework
##CoAP协议介绍
由于物联网中的很多设备都是资源受限型的，即只有少量的内存空间和有限的计算能力，所以传统的HTTP协议应用在物联网上就显得过于庞大而不适用。 IETF的CoRE工作组提出了一种基于REST架构的CoAP协议。
CoAP类似于Http，但更为紧凑，一些协议部分以位为单位。
CoAP头部包括：
coap版本信息 占用2位，当前为01
报文类型T
标签长度指示TKL，TKL的最大允许值，TKL当前支持0~8B，其他长度保留将来扩展用，TKL占4位
消息类型 占用2位，包括CON，NON,ACK,RES：
__CON类型的消息为主动发出消息请求，并且需要接收方作出回复__
__NON类型的消息为主动发出消息请求，但是不需要接收方作出回复__
__ACK类型的消息为接收方作出回复__
__Res类型为发出CON消息后，在还没收到请求时，主动通知不需要再回复__
状态码 占用1字节，分为：发送和接收:
__发送方状态码：EMPTY,GET,POST,PUT,DELETE__
消息编号 占用2字节，代表了该消息的编号
标签Token，Token的字节数由token length,即TKL决定
选项Options，请求消息与回应消息都可以有0..*个option，option主要用于描述请求或相应对应的各个属性，类似于参数或特征描述
负载Payload，由payload Marker标识领起，payload Marker为0xFF
