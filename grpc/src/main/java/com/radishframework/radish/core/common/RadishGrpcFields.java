package com.radishframework.radish.core.common;

import com.google.common.collect.ImmutableMap;
import io.opentracing.Span;
import io.opentracing.log.Fields;


public final class RadishGrpcFields {
    public static final String ERROR = "error";
    public static final String HEADERS = "headers";

    public static final String CLIENT_CALL_START = "client-call-start";
    public static final String CLIENT_CALL_CANCEL = "client-call-cancel";
    public static final String CLIENT_CALL_HALF_CLOSE = "client-call-half-close";
    public static final String CLIENT_CALL_SEND_MESSAGE = "client-call-send-message";

    public static final String CLIENT_CALL_LISTENER_ON_HEADERS = "client-call-listener-on-headers";
    public static final String CLIENT_CALL_LISTENER_ON_MESSAGE = "client-call-listener-on-message";
    public static final String CLIENT_CALL_LISTENER_ON_CLOSE = "client-call-listener-on-close";

    public static final String SERVER_CALL_SEND_HEADERS = "server-call-send-headers";
    public static final String SERVER_CALL_SEND_MESSAGE = "server-call-send-message";
    public static final String SERVER_CALL_CLOSE = "server-call-close";

    public static final String SERVER_CALL_LISTENER_ON_MESSAGE = "server-call-listener-on-message";
    public static final String SERVER_CALL_LISTENER_ON_HALF_CLOSE = "server-call-listener-on-half-close";
    public static final String SERVER_CALL_LISTENER_ON_CANCEL = "server-call-listener-on-cancel";
    public static final String SERVER_CALL_LISTENER_ON_COMPLETE = "server-call-listener-on-complete";

    public static void logClientCallError(Span span, String message, Throwable cause) {
        logCallError(span, message, cause, "Client");
    }

    public static void logServerCallError(Span span, String message, Throwable cause) {
        logCallError(span, message, cause, "Server");
    }

    private static void logCallError(Span span, String message, Throwable cause, String name) {
        ImmutableMap.Builder<String, Object> builder =
                ImmutableMap.<String, Object>builder().put(Fields.EVENT, RadishGrpcFields.ERROR);
        String causeMessage = null;
        if (cause != null) {
            builder.put(Fields.ERROR_OBJECT, cause);
            causeMessage = cause.getMessage();
        }
        if (message != null) {
            builder.put(Fields.MESSAGE, message);
        } else if (causeMessage != null) {
            builder.put(Fields.MESSAGE, causeMessage);
        } else {
            builder.put(Fields.MESSAGE, name + " call failed");
        }
        span.log(builder.build());
    }
}
