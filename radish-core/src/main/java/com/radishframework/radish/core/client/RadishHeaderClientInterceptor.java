package com.radishframework.radish.core.client;

import com.radishframework.radish.core.common.Constant;
import com.radishframework.radish.core.utils.GrpcContextUtils;
import io.grpc.*;

public class RadishHeaderClientInterceptor implements ClientInterceptor {

    private final String appName;

    public RadishHeaderClientInterceptor(String appName) {
        this.appName = appName;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                GrpcContextUtils.appendToMetadata(headers, Constant.CTX_KEY_CLIENT_ID, appName);
                GrpcContextUtils.appendToMetadata(headers, Constant.CTX_KEY_USER_ID);
                GrpcContextUtils.appendToMetadata(headers, Constant.CTX_KEY_GW_APP_ID);
                GrpcContextUtils.appendToMetadata(headers, Constant.CTX_KEY_GW_APP_PLATFORM);
                GrpcContextUtils.appendToMetadata(headers, Constant.CTX_KEY_GW_APP_VERSION);
                GrpcContextUtils.appendToMetadata(headers, Constant.CTX_KEY_GW_APP_DEVICE_ID);
                GrpcContextUtils.appendToMetadata(headers, Constant.CTX_KEY_UPSTREAM_IP, Constant.CTX_KEY_LOCAL_IP.get());

                super.start(responseListener, headers);
            }
        };
    }

}