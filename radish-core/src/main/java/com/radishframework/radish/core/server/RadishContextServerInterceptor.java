package com.radishframework.radish.core.server;

import com.radishframework.radish.core.common.Constant;
import com.radishframework.radish.core.utils.GrpcContextUtils;
import io.grpc.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;

public class RadishContextServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        List<Context.Key<String>> stringKeyList = Arrays.asList(
                Constant.CTX_KEY_USER_WX_OPEN_ID,
                Constant.CTX_KEY_USER_WX_SESSION_KEY,
                Constant.CTX_KEY_TOKEN,
                Constant.CTX_KEY_LOGIN,
                Constant.CTX_KEY_CLIENT_ID,
                Constant.CTX_KEY_GW_CLIENT_IP,
                Constant.CTX_KEY_GW_APP_ID,
                Constant.CTX_KEY_GW_APP_PLATFORM,
                Constant.CTX_KEY_GW_APP_VERSION,
                Constant.CTX_KEY_GW_APP_DEVICE_ID,
                Constant.CTX_KEY_GW_X_AUTH_TOKEN,
                Constant.CTX_KEY_REFERER,
                Constant.CTX_KEY_USER_AGENT,
                Constant.CTX_KEY_UPSTREAM_IP);

        List<Context.Key<Long>> longKeyList = Arrays.asList(
                Constant.CTX_KEY_USER_ID
        );


        Context ctx = Context.current();

        ctx = GrpcContextUtils.parseStringFromMetadata(ctx, metadata, stringKeyList);
        ctx = GrpcContextUtils.parseLongFromMetadata(ctx, metadata, longKeyList);
        final SocketAddress remoteAddress = serverCall.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (remoteAddress != null) {
            ctx = GrpcContextUtils.appendToContext(ctx, Constant.CTX_KEY_CLIENT_IP, ((InetSocketAddress) remoteAddress).getAddress().getHostAddress());
        }

        return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
    }
}