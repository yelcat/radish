package com.radishframework.radish.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GrpcReflectionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcReflectionUtils.class);

    private GrpcReflectionUtils() {}

    public static void disableStatsAndTracingModule(Object object) {
        try {
            final Method setTracingEnabled = object.getClass()
                    .getDeclaredMethod("setTracingEnabled", boolean.class);
            if (setTracingEnabled == null) {
                return;
            }
            setTracingEnabled.setAccessible(true);
            setTracingEnabled.invoke(object, false);

            final Method setStatsEnabled = object.getClass()
                    .getDeclaredMethod("setStatsEnabled", boolean.class);
            if (setStatsEnabled == null) {
                return;
            }
            setStatsEnabled.setAccessible(true);
            setStatsEnabled.invoke(object, false);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOG.error("disable tracing module error", e);
        }
    }
}
