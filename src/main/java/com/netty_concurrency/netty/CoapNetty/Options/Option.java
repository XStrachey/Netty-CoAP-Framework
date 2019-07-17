package com.netty_concurrency.netty.CoapNetty.Options;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.netty_concurrency.netty.CoapNetty.MsgCode;

import java.util.HashMap;

public abstract class Option {
    //请求或相应中option项可支持的数目
    //0，1，..*
    public enum Occurence{
        NONE, ONCE, MULTIPLE
    }

    public static final int UNKNOWN = -1;
    //用于携带ETAG value，可以有0..*，有一个匹配则条件满足
    //一般用于更新服务器资源
    public static final int IF_MATCH = 1;
    //服务器主机名，大小写不敏感
    public static final int URI_HOST = 3;
    //Enity Tag，实体标签由服务器产生
    //用于区分随时间变化的相同资源的表示之间的资源本地标识符
    public static final int ETAG = 4;
    //不携带ETAG value
    //一般用于在服务器创建新资源
    public static final int IF_NONE_MATCH = 5;
    //用于实现订阅与发布模型管理
    // 在GET请求里，该option值为0代表向服务器订阅一个主题，值为1代表向服务器移除一个已订阅主题
    public static final int OBSERVE = 6;
    //服务器端口号，coap默认为5683，coaps默认为5684
    public static final int URI_PORT = 7;
    //Location_path和Location_query共同组成一个绝对路径或则一个query string或则两者都有
    public static final int LOCATION_PATH = 8;
    //资源路径名，指定资源在host内路径，由“/”分隔
    //空的path组件等效于根目录“/”，应该列出“/”
    public static final int URI_PATH = 11;
    //用于指示coap选项中的负载媒体类型，媒体类型采用无符号整数编码，参见相关类
    public static final int CONTENT_FORMAT = 12;
    //指定响应的生存时间，即保持fresh的时间，默认60s
    public static final int MAX_AGE = 14;
    //定义访问资源参数，由一系列“&”分隔的参数组成，通常由“key=value”的形式出现
    public static final int URI_QUERY = 15;
    //用于表示coap客户端期望接收到的媒体类型格式
    //accept选项和content-format选项的负载类型相同
    //他们两者的配合可以理解为：accept提出想要的媒体类型格式
    //content-format返回告诉客户端它这个报文所携带的内容是哪种格式
    //如果服务器不能返回指定格式的响应，可能会响应4.06
    public static final int ACCEPT = 17;

    public static final int LOCATION_QUERY = 20;
    //用于块传输 主要用于服务器端响应时分块传输
    public static final int BLOCK_2 = 23;
    //用于块传输 主要用于客户端发出请求时分块传输
    public static final int BLOCK_1 = 27;
    //用于块传输 代表服务器端响应资源总大小
    public static final int SIZE_2 = 28;
    //用于发往Forward-Proxy的请求中，表示一个绝对URI
    public static final int PROXY_URI = 35;
    //代理scheme，比如coap，coaps，http，https
    public static final int PROXY_SCHEME = 39;
    //代表客户端发出请求里资源总大小
    public static final int SIZE_1 = 60;

    public static final int ENDPOINT_ID_1 = 124;

    public static final int ENDPOINT_ID_2 = 189;

    private static HashMap<Integer, String> OPTIONS = new HashMap<>();
    static {
        OPTIONS.putAll(ImmutableMap.<Integer, String>builder()
                .put(IF_MATCH, "IF_MATCH(" + IF_MATCH +")")
                .put(URI_HOST, "URI_HOST(" + URI_HOST +")")
                .put(ETAG, "ETAG(" + ETAG +")")
                .put(IF_NONE_MATCH, "IF_NONE_MATCH(" + IF_NONE_MATCH +")")
                .put(OBSERVE, "OBSERVE(" + OBSERVE +")")
                .put(URI_PORT, "URI_PORT(" + URI_PORT +")")
                .put(LOCATION_PATH, "LOCATION_PATH(" + LOCATION_PATH +")")
                .put(URI_PATH, "URI_PATH(" + URI_PATH +")")
                .put(CONTENT_FORMAT, "CONTENT_FORMAT(" + CONTENT_FORMAT +")")
                .put(MAX_AGE, "MAX_AGE(" + MAX_AGE +")")
                .put(URI_QUERY, "URI_QUERY(" + URI_QUERY +")")
                .put(ACCEPT, "ACCEPT(" + ACCEPT +")")
                .put(LOCATION_QUERY, "LOCATION_QUERY(" + LOCATION_QUERY +")")
                .put(BLOCK_2, "BLOCK_2(" + BLOCK_2 +")")
                .put(BLOCK_1, "BLOCK_1(" + BLOCK_1 +")")
                .put(SIZE_2, "SIZE_2(" + SIZE_2 +")")
                .put(PROXY_URI, "PROXY_URI(" + PROXY_URI +")")
                .put(PROXY_SCHEME, "PROXY_SCHEME(" + PROXY_SCHEME +")")
                .put(SIZE_1, "SIZE_1(" + SIZE_1 +")")
                .put(ENDPOINT_ID_1, "ENDPOINT_ID_1(" + ENDPOINT_ID_1 +")")
                .put(ENDPOINT_ID_2, "ENDPOINT_ID_2(" + ENDPOINT_ID_2 +")")
                .build()
        );
    }

    public static String asString(int optionNum){
        String result = OPTIONS.get(optionNum);
        return result == null ? "UNKNOWN " + optionNum : result;
    }

    //互斥集
    private static HashMultimap<Integer, Integer> MUTUAL_EXCLUSIONS = HashMultimap.create();
    static {
        MUTUAL_EXCLUSIONS.put(URI_HOST, PROXY_URI);
        MUTUAL_EXCLUSIONS.put(PROXY_URI,URI_HOST);

        MUTUAL_EXCLUSIONS.put(URI_PORT, PROXY_URI);
        MUTUAL_EXCLUSIONS.put(PROXY_URI, URI_PORT);

        MUTUAL_EXCLUSIONS.put(URI_PATH, PROXY_URI);
        MUTUAL_EXCLUSIONS.put(PROXY_URI, URI_PATH);

        MUTUAL_EXCLUSIONS.put(URI_QUERY, PROXY_URI);
        MUTUAL_EXCLUSIONS.put(PROXY_URI, URI_QUERY);

        MUTUAL_EXCLUSIONS.put(PROXY_SCHEME, PROXY_URI);
        MUTUAL_EXCLUSIONS.put(PROXY_URI, PROXY_SCHEME);
    }

    public static boolean mutuallyExludes(int firstOptionNum, int secondOptionNum){
        return MUTUAL_EXCLUSIONS.get(firstOptionNum).contains(secondOptionNum);
    }

    //各请求或响应的option限制或说要求
    public static final HashBasedTable<Integer, Integer, Option.Occurence> OCCURENCE_CONSTRAINTS
            = HashBasedTable.create();
    static {
        //.row返回包含给定行键MsgCode.的所有映射的视图
        //.put关联指定值与指定键
        //.putAll复制从指定的表中的所有映射到这个表
        //ImmutableMap是不可变集合
        OCCURENCE_CONSTRAINTS.row(MsgCode.GET).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(URI_HOST, Occurence.ONCE)
                .put(URI_PORT, Occurence.ONCE)
                .put(URI_PATH, Occurence.MULTIPLE)
                .put(URI_QUERY, Occurence.MULTIPLE)
                .put(PROXY_URI, Occurence.ONCE)
                .put(PROXY_SCHEME, Occurence.ONCE)
                .put(ACCEPT, Occurence.MULTIPLE)
                .put(OBSERVE, Occurence.ONCE)
                .put(BLOCK_2, Occurence.ONCE)
                .put(SIZE_2, Occurence.ONCE)
                .put(ENDPOINT_ID_1, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.POST).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(URI_HOST, Occurence.ONCE)
                .put(URI_PORT, Occurence.ONCE)
                .put(URI_PATH, Occurence.MULTIPLE)
                .put(URI_QUERY, Occurence.MULTIPLE)
                .put(ACCEPT, Occurence.MULTIPLE)
                .put(PROXY_URI, Occurence.ONCE)
                .put(PROXY_SCHEME, Occurence.ONCE)
                .put(BLOCK_2, Occurence.ONCE)
                .put(SIZE_2, Occurence.ONCE)
                .put(BLOCK_1, Occurence.ONCE)
                .put(SIZE_1, Occurence.ONCE)
                .put(ENDPOINT_ID_1, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.PUT).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(URI_HOST, Occurence.ONCE)
                .put(URI_PORT, Occurence.ONCE)
                .put(URI_PATH, Occurence.MULTIPLE)
                .put(URI_QUERY, Occurence.MULTIPLE)
                .put(ACCEPT, Occurence.MULTIPLE)
                .put(PROXY_URI, Occurence.ONCE)
                .put(PROXY_SCHEME, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(IF_MATCH, Occurence.ONCE)
                .put(IF_NONE_MATCH, Occurence.ONCE)
                .put(BLOCK_2, Occurence.ONCE)
                .put(SIZE_2, Occurence.ONCE)
                .put(BLOCK_1, Occurence.ONCE)
                .put(SIZE_1, Occurence.ONCE)
                .put(ENDPOINT_ID_1, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.DELETE).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(URI_HOST, Occurence.ONCE)
                .put(URI_PORT, Occurence.ONCE)
                .put(URI_PATH, Occurence.MULTIPLE)
                .put(URI_QUERY, Occurence.MULTIPLE)
                .put(PROXY_URI, Occurence.ONCE)
                .put(PROXY_SCHEME, Occurence.ONCE)
                .put(ENDPOINT_ID_1, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.CREATED_201).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(ETAG, Occurence.ONCE)
                .put(OBSERVE, Occurence.ONCE)
                .put(LOCATION_PATH, Occurence.MULTIPLE)
                .put(LOCATION_QUERY, Occurence.MULTIPLE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(BLOCK_2, Occurence.ONCE)
                .put(BLOCK_1, Occurence.ONCE)
                .put(SIZE_2, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.DELETED_202).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(BLOCK_2, Occurence.ONCE)
                .put(BLOCK_1, Occurence.ONCE)
                .put(SIZE_2, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.VALID_203).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(OBSERVE, Occurence.ONCE)
                .put(ETAG, Occurence.ONCE)
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_1, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.CHANGED_204).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(ETAG, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(BLOCK_1, Occurence.ONCE)
                .put(BLOCK_2, Occurence.ONCE)
                .put(SIZE_2, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.CONTENT_205).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(OBSERVE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(MAX_AGE, Occurence.ONCE)
                .put(ETAG, Occurence.ONCE)
                .put(BLOCK_2, Occurence.ONCE)
                .put(BLOCK_1, Occurence.ONCE)
                .put(SIZE_2, Occurence.ONCE)
                .put(ENDPOINT_ID_1, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.CONTINUE_231).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(BLOCK_1, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.BAD_REQUEST_400).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.UNAUTHORIZED_401).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.BAD_OPTION_402).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.FORBIDDEN_403).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.NOT_FOUND_404).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.METHOD_NOT_ALLOWED_405).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.NOT_ACCEPTABLE_406).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.REQUEST_ENTITY_INCOMPLETE_408).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.PRECONDITION_FAILED_412).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.REQUEST_ENTITY_TOO_LARGE_413).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(BLOCK_1, Occurence.ONCE)
                .put(SIZE_1, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.UNSUPPORTED_CONTENT_FORMAT_415).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.INTERNAL_SERVER_ERROR_500).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.NOT_IMPLEMENTED_501).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.BAD_GATEWAY_502).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.GATEWAY_TIMEOUT_504).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );

        OCCURENCE_CONSTRAINTS.row(MsgCode.PROXYING_NOT_SUPPORTED_505).putAll(ImmutableMap.<Integer, Occurence>builder()
                .put(MAX_AGE, Occurence.ONCE)
                .put(CONTENT_FORMAT, Occurence.ONCE)
                .put(ENDPOINT_ID_2, Occurence.ONCE)
                .build()
        );
    }

    public static Occurence getPermittedOccurrence(int optionNum, int msgCode){
        Occurence result = OCCURENCE_CONSTRAINTS.get(msgCode, optionNum);
        return result == null ? Occurence.NONE :result;
    }

    public static boolean isCritical(int optionNum){
        return (optionNum & 1) == 1;
    }

    public static boolean isSafe(int optionNum){
        return !((optionNum & 2) == 2);
    }
    /*
    举个例子：
    IF_MATCH = 1
    0000 0001 1
    0000 0010
    & =
    0000 0000 0
    0 ！= 2 OK
    再如：
    0000 1010 10
    0000 0010
    & =
    0000 0010 2
    2 == 2 UN-OK
     */

    public static boolean isCacheKey(int optionNum){
        return !((optionNum & 0x1e) == 0x1c);
    }
    /*
    举个例子：
    IF_MATCH = 1
    0000 0001 1
    0001 1110 0x1e
    & =
    0000 0000 0
    0001 1100 0x1c
    0 ！= 0x1c OK
     */
}
