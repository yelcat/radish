package com.radishframework.radish.core.common;

import io.grpc.Context;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class Constant {
    public static final Logger LOG = LoggerFactory.getLogger(Constant.class);

    // 特定context value
    public final static String CTX_VALUE_GW_APP_ID_YPDJ = "ypdj";   // APP ID值 - 谊品到家
    public final static String CTX_VALUE_GW_APP_PLATFORM_WXAPP = "wxApp"; // APP平台值 - 微信程序
    public final static String CTX_VALUE_GW_APP_PLATFORM_IOS = "iOS"; // APP平台值 - iOS程序
    public final static String CTX_VALUE_GW_APP_PLATFORM_ANDROID = "android"; // 请求的客户端APP平台值 - 安卓程序
    public final static String CTX_VALUE_GW_APP_PLATFORM_WEB = "web"; // APP平台值 - web应用
    public final static String OSTRICH_URL_PREFIX = "radish://"; // radish url前缀信息
    public final static String DEFAULT_LOAD_BALANCING_POLICY = "round_robin"; // 默认负载均衡策略
    public final static String ETCD_PATH_SPLITTER = "/"; // ETCD路径分隔符
    public final static String ETCD_SERVICES_INSTANCE_PATH_PREFIX = ETCD_PATH_SPLITTER + "radish" + ETCD_PATH_SPLITTER + "instances"; // 服务前缀
    public final static int SERVICE_AUTO_REGISTRY_INTERVAL = 300; // s, 5分钟
    public final static int SERVICE_RETRY_REGISTRY_INTERVAL = 3; // s, 3秒

    public static Metadata.Key<String> CLIENT_ID_MD_KEY;
    public static Metadata.Key<String> TOKEN_METADATA_KEY;
    public static Metadata.Key<String> TRACE_ID_METADATA_KEY;

    // context key 常量
    public static Context.Key<String> CTX_KEY_APP_NAME;
    public static Context.Key<Long> CTX_KEY_USER_ID;  // 当前登录用户ID
    public static Context.Key<String> CTX_KEY_USER_WX_OPEN_ID; // 当前登录用户微信openId
    public static Context.Key<String> CTX_KEY_USER_WX_SESSION_KEY;  // 当前登录用户微信session key
    public static Context.Key<String> CTX_KEY_TOKEN; // 当前登录用户token
    public static Context.Key<String> CTX_KEY_LOGIN; // 当前B端登录用户login，在B端切成C端用户之后，会去掉
    public static Context.Key<String> CTX_KEY_TRACE_ID;
    public static Context.Key<String> CTX_KEY_CLIENT_ID;
    public static Context.Key<String> CTX_KEY_CLIENT_IP;
    public static Context.Key<String> CTX_KEY_GW_CLIENT_IP; // 网关请求
    public static Context.Key<String> CTX_KEY_GW_APP_ID;   // 网关请求的客户端标示，具体值见CTX_VALUE_APP_ID_XXX
    public static Context.Key<String> CTX_KEY_GW_APP_PLATFORM; // 网关请求的客户端平台，具体值见CTX_VALUE_APP_PLATFORM_XXX
    public static Context.Key<String> CTX_KEY_GW_APP_VERSION;  // 网关请求的客户端版本号
    public static Context.Key<String> CTX_KEY_GW_APP_DEVICE_ID;  // 网关请求的客户端版本号
    public static Context.Key<String> CTX_KEY_GW_X_AUTH_TOKEN;  // 网关请求的X-Auth-Token header信息
    public static Context.Key<String> CTX_KEY_USER_AGENT; //由网关传递，只传递一级
    public static Context.Key<String> CTX_KEY_REFERER; //来源地址，由网关传递，只传递一级
    public static Context.Key<String> CTX_KEY_UPSTREAM_IP; //上游地址，从网关开始传递
    public static Context.Key<String> CTX_KEY_LOCAL_IP;//本机IP

    static {
        try {
            CLIENT_ID_MD_KEY = Metadata.Key.of("client-id", ASCII_STRING_MARSHALLER);
            TOKEN_METADATA_KEY = Metadata.Key.of("token", ASCII_STRING_MARSHALLER);
            TRACE_ID_METADATA_KEY = Metadata.Key.of("traceId", ASCII_STRING_MARSHALLER);

            // context key 常量
            CTX_KEY_APP_NAME = Context.key("appName");
            CTX_KEY_USER_ID = Context.key("userId");  // 当前登录用户ID
            CTX_KEY_USER_WX_OPEN_ID = Context.key("wxOpenId"); // 当前登录用户微信openId
            CTX_KEY_USER_WX_SESSION_KEY = Context.key("wxSessionKey");  // 当前登录用户微信session key
            CTX_KEY_TOKEN = Context.key("token"); // 当前登录用户token
            CTX_KEY_LOGIN = Context.key("login"); // 当前B端登录用户login，在B端切成C端用户之后，会去掉
            CTX_KEY_TRACE_ID = Context.key("traceId");
            CTX_KEY_CLIENT_ID = Context.key("clientId");
            CTX_KEY_CLIENT_IP = Context.key("clientIp");
            CTX_KEY_GW_CLIENT_IP = Context.key("gwClientIp"); // 网关请求
            CTX_KEY_GW_APP_ID = Context.key("gwAppId");   // 网关请求的客户端标示，具体值见CTX_VALUE_APP_ID_XXX
            CTX_KEY_GW_APP_PLATFORM = Context.key("gwAppPlatform"); // 网关请求的客户端平台，具体值见CTX_VALUE_APP_PLATFORM_XXX
            CTX_KEY_GW_APP_VERSION = Context.key("gwAppVersion");  // 网关请求的客户端版本号
            CTX_KEY_GW_APP_DEVICE_ID = Context.key("gwAppDeviceId");  // 网关请求的客户端版本号
            CTX_KEY_GW_X_AUTH_TOKEN = Context.key("gwXAuthToken");  // 网关请求的X-Auth-Token header信息
            CTX_KEY_USER_AGENT = Context.key("userAgent");//由网关传递，只传递一级
            CTX_KEY_REFERER = Context.key("referer");//来源地址，由网关传递，只传递一级
            CTX_KEY_UPSTREAM_IP = Context.key("upstreamIp");//上游地址，从网关开始传递
            CTX_KEY_LOCAL_IP = Context.key("localIp");//本机IP
        } catch (Throwable th) {
            LOG.error("Constant exception!!!", th);
        }
    }
}
