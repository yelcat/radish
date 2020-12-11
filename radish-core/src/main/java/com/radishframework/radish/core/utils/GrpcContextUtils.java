package com.radishframework.radish.core.utils;

import io.grpc.Context;
import io.grpc.Context.Key;
import io.grpc.Metadata;

import java.util.List;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class GrpcContextUtils {

    public static final String METADATA_KEY_PREFIX = "radish_";

    /**
     * 将值放入context中
     *
     * @param ctx
     * @param contextKey
     * @param value
     * @param <T>
     * @return
     */
    public static <T> Context appendToContext(Context ctx, Context.Key<T> contextKey, T value) {
        if (value != null) {
            ctx = ctx.withValue(contextKey, value);
        }
        return ctx;
    }

    /**
     * 将context放入Metadata中
     *
     * @param contextKeyList
     * @param headers
     */
    public static void appendToMetadata(List<Key<?>> contextKeyList, Metadata headers) {

        if (contextKeyList == null) {
            return;
        }

        contextKeyList.forEach(contextKey -> {
            appendToMetadata(headers, contextKey);
        });
    }

    public static void appendToMetadata(Metadata headers, Key<?> contextKey, Object value) {
        String keyName = contextKey.toString();
        appendToMetadata(headers, keyName, value);
    }

    public static void appendToMetadata(Metadata headers, Key<?> contextKey) {
        String keyName = contextKey.toString();
        Object value = contextKey.get();
        appendToMetadata(headers, keyName, value);
    }

    public static void appendToMetadata(Metadata headers, String keyName, Object value) {
        if (value == null) {
            return;
        }

        Metadata.Key<String> metadataKey = Metadata.Key.of(METADATA_KEY_PREFIX + keyName, ASCII_STRING_MARSHALLER);
        headers.put(metadataKey, value.toString());
    }

    /**
     * 从Metadata解析String类型的context
     *
     * @param ctx
     * @param headers
     * @param contextKeyList
     * @return
     */
    public static Context parseStringFromMetadata(Context ctx, Metadata headers, List<Key<String>> contextKeyList) {
        if (contextKeyList == null) {
            return ctx;
        }

        for (Context.Key<String> contextKey : contextKeyList) {
            ctx = setContextValue(ctx, headers, contextKey);
        }

        return ctx;
    }

    public static Context setContextValue(Context ctx, Metadata headers, Key<String> contextKey) {
        String keyName = contextKey.toString();
        String value = headers.get(Metadata.Key.of(METADATA_KEY_PREFIX + keyName, ASCII_STRING_MARSHALLER));
        if (value != null) {
            ctx = ctx.withValue(contextKey, value);
        }
        return ctx;
    }

    /**
     * 从Metadata解析Long类型的Context值
     *
     * @param ctx
     * @param headers
     * @param contextKeyList
     * @return
     */
    public static Context parseLongFromMetadata(Context ctx, Metadata headers, List<Key<Long>> contextKeyList) {
        if (contextKeyList == null) {
            return ctx;
        }

        for (Context.Key<Long> contextKey : contextKeyList) {
            String keyName = contextKey.toString();
            String valueStr = headers.get(Metadata.Key.of(METADATA_KEY_PREFIX + keyName, ASCII_STRING_MARSHALLER));
            if (valueStr != null) {
                Long value = Long.valueOf(valueStr);
                ctx = ctx.withValue(contextKey, value);
            }
        }

        return ctx;
    }

}
