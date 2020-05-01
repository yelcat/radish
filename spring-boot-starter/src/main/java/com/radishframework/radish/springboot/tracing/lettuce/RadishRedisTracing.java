package com.radishframework.radish.springboot.tracing.lettuce;

import com.google.common.collect.ImmutableMap;
import io.lettuce.core.tracing.*;
import io.opentracing.Span;
import io.opentracing.log.Fields;
import io.opentracing.util.GlobalTracer;

import java.net.SocketAddress;

public final class RadishRedisTracing implements Tracing {

    private final Endpoint NOOP_ENDPOINT = new Endpoint() {
    };

    public RadishRedisTracing() {
    }

    @Override
    public TraceContextProvider initialTraceContextProvider() {
        return new RadishTraceContextProvider();
    }

    @Override
    public Endpoint createEndpoint(SocketAddress socketAddress) {
        return NOOP_ENDPOINT;
    }

    @Override
    public TracerProvider getTracerProvider() {
        return new RadishTracerProvider();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean includeCommandArgsInSpanTags() {
        return false;
    }

    private static class RadishTracerProvider implements TracerProvider {

        private final io.opentracing.Tracer tracer;

        public RadishTracerProvider() {
            this.tracer = GlobalTracer.get();
        }

        @Override
        public Tracer getTracer() {
            return new RadishTracer(tracer);
        }
    }

    private static class RadishTracer extends Tracer {

        private final io.opentracing.Tracer tracer;

        public RadishTracer(io.opentracing.Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public Span nextSpan() {
            io.opentracing.Span span = tracer.buildSpan("").start();
            span.setTag("redis.client", "Lettuce");
            return new RadishRedisSpan(span);
        }

        @Override
        public Span nextSpan(TraceContext traceContext) {
            if (traceContext instanceof RadishTraceContext) {
                RadishTraceContext context = (RadishTraceContext) traceContext;
                io.opentracing.Span parent = context.getContext();
                if (null != parent) {
                    io.opentracing.Span span = tracer.buildSpan("")
                            .asChildOf(parent).start();
                    span.setTag("redis.client", "Lettuce");
                    return new RadishRedisSpan(span);
                }
            }
            return nextSpan();
        }
    }

    /**
     * 用于获取上下文传播的span
     */
    private static class RadishTraceContextProvider implements TraceContextProvider {

        private final io.opentracing.Tracer tracer;

        public RadishTraceContextProvider() {
            this.tracer = GlobalTracer.get();
        }

        @Override
        public TraceContext getTraceContext() {
            return new RadishTraceContext(tracer.activeSpan());
        }
    }

    /**
     * 上下文传播的span
     */
    private static class RadishTraceContext implements TraceContext {

        private final Span context;

        public RadishTraceContext(Span span) {
            this.context = span;
        }

        public Span getContext() {
            return context;
        }
    }

    private static class RadishRedisSpan extends Tracer.Span {

        private final Span span;

        public RadishRedisSpan(Span span) {
            this.span = span;
        }

        @Override
        public Tracer.Span start() {
            return this;
        }

        @Override
        public Tracer.Span annotate(String value) {
            return this;
        }

        @Override
        public Tracer.Span tag(String key, String value) {
            span.setTag(key, value);
            return this;
        }

        @Override
        public Tracer.Span error(Throwable throwable) {
            span.log(ImmutableMap.<String, Object>builder()
                    .put(Fields.EVENT, "error")
                    .put(Fields.MESSAGE, null != throwable ? throwable.getMessage() : "").build());
            return this;
        }

        @Override
        public Tracer.Span remoteEndpoint(Endpoint endpoint) {
            return this;
        }

        @Override
        public Tracer.Span name(String name) {
            span.setOperationName(name);
            return this;
        }

        @Override
        public void finish() {
            span.finish();
        }
    }
}
