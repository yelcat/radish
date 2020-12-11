package com.radishframework.radish.core.client;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.radishframework.radish.core.common.RadishGrpcTags;
import io.grpc.*;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.*;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

import java.util.*;

public class RadishTracingClientInterceptor implements ClientInterceptor {

    private final Tracer tracer;
    private final OperationNameConstructor operationNameConstructor;
    private final boolean streaming;
    private final boolean verbose;
    private final Set<ClientRequestAttribute> tracedAttributes;
    private final ActiveSpanSource activeSpanSource;
    private final ActiveSpanContextSource activeSpanContextSource;
    private final ImmutableList<ClientSpanDecorator> clientSpanDecorators;
    private final ImmutableList<ClientCloseDecorator> clientCloseDecorators;

    public RadishTracingClientInterceptor(Builder builder) {
        this.tracer = builder.tracer;
        this.operationNameConstructor = builder.operationNameConstructor;
        this.streaming = builder.streaming;
        this.verbose = builder.verbose;
        this.tracedAttributes = builder.tracedAttributes;
        this.activeSpanSource = builder.activeSpanSource;
        this.activeSpanContextSource = builder.activeSpanContextSource;
        this.clientSpanDecorators = ImmutableList.copyOf(builder.clientSpanDecorators.values());
        this.clientCloseDecorators = ImmutableList.copyOf(builder.clientCloseDecorators.values());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        String operationName = operationNameConstructor.constructOperationName(method);
        SpanContext activeSpanContext = getActiveSpanContext();
        final Span span = createSpanFromParent(activeSpanContext, operationName);
        span.setTag(Tags.PEER_SERVICE, method.getServiceName());
        try (Scope ignored = tracer.scopeManager().activate(span)) {
            for (ClientSpanDecorator clientSpanDecorator : clientSpanDecorators) {
                clientSpanDecorator.interceptCall(span, method, callOptions);
            }

            for (ClientRequestAttribute attr : this.tracedAttributes) {
                switch (attr) {
                    case ALL_CALL_OPTIONS:
                        RadishGrpcTags.GRPC_CALL_OPTIONS.set(span, callOptions);
                        break;
                    case AUTHORITY:
                        RadishGrpcTags.GRPC_AUTHORITY.set(
                                span, MoreObjects.firstNonNull(callOptions.getAuthority(), next.authority()));
                        break;
                    case COMPRESSOR:
                        RadishGrpcTags.GRPC_COMPRESSOR.set(span, callOptions.getCompressor());
                        break;
                    case DEADLINE:
                        RadishGrpcTags.GRPC_DEADLINE.set(span, callOptions.getDeadline());
                        break;
                    case METHOD_NAME:
                        RadishGrpcTags.GRPC_METHOD_NAME.set(span, method);
                        break;
                    case METHOD_TYPE:
                        RadishGrpcTags.GRPC_METHOD_TYPE.set(span, method);
                        break;
                    case HEADERS:
                        break;
                    default:
                }
            }

            return new RadishForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions),
                    span, tracer, streaming, verbose, tracedAttributes);
        }
    }

    private SpanContext getActiveSpanContext() {
        if (activeSpanSource != null) {
            Span activeSpan = activeSpanSource.getActiveSpan();
            if (activeSpan != null) {
                return activeSpan.context();
            }
        }
        if (activeSpanContextSource != null) {
            final SpanContext spanContext = activeSpanContextSource.getActiveSpanContext();
            if (spanContext != null) {
                return spanContext;
            }
        }
        if (tracer.activeSpan() != null) {
            return tracer.activeSpan().context();
        }
        return null;
    }

    private Span createSpanFromParent(SpanContext parentSpanContext, String operationName) {
        final Tracer.SpanBuilder spanBuilder;
        if (parentSpanContext == null) {
            spanBuilder = tracer.buildSpan(operationName);
        } else {
            spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanContext);
        }
        return spanBuilder
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                .withTag(Tags.COMPONENT.getKey(), RadishGrpcTags.COMPONENT_NAME)
                .start();
    }

    public static class Builder {

        private Tracer tracer;
        private OperationNameConstructor operationNameConstructor;
        private boolean streaming;
        private boolean verbose;
        private Set<ClientRequestAttribute> tracedAttributes;
        private ActiveSpanSource activeSpanSource;
        private ActiveSpanContextSource activeSpanContextSource;
        private Map<Class<?>, ClientSpanDecorator> clientSpanDecorators;
        private Map<Class<?>, ClientCloseDecorator> clientCloseDecorators;

        /**
         * Creates a Builder with GlobalTracer if present else NoopTracer.
         */
        public Builder() {
            this.tracer = GlobalTracer.isRegistered() ? GlobalTracer.get() : NoopTracerFactory.create();
            this.operationNameConstructor = OperationNameConstructor.DEFAULT;
            this.streaming = false;
            this.verbose = false;
            this.tracedAttributes = new HashSet<>();
            this.activeSpanSource = ActiveSpanSource.NONE;
            this.activeSpanContextSource = ActiveSpanContextSource.NONE;
            this.clientSpanDecorators = new HashMap<>();
            this.clientCloseDecorators = new HashMap<>();
        }

        /**
         * Provide the {@link Tracer}.
         *
         * @param tracer the tracer
         * @return this Builder with configured tracer
         */
        public RadishTracingClientInterceptor.Builder withTracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        /**
         * Provide operation name constructor.
         *
         * @param operationNameConstructor to name all spans created by this interceptor
         * @return this Builder with configured operation name
         */
        public RadishTracingClientInterceptor.Builder withOperationName(OperationNameConstructor operationNameConstructor) {
            this.operationNameConstructor = operationNameConstructor;
            return this;
        }

        /**
         * Logs streaming events to client spans.
         *
         * @return this Builder configured to log streaming events
         */
        public RadishTracingClientInterceptor.Builder withStreaming() {
            this.streaming = true;
            return this;
        }

        /**
         * Provide traced attributes.
         *
         * @param tracedAttributes to set as tags on client spans created by this interceptor
         * @return this Builder configured to trace attributes
         */
        public RadishTracingClientInterceptor.Builder withTracedAttributes(ClientRequestAttribute... tracedAttributes) {
            this.tracedAttributes = new HashSet<>(Arrays.asList(tracedAttributes));
            return this;
        }

        /**
         * Logs all request life-cycle events to client spans.
         *
         * @return this Builder configured to be verbose
         */
        public RadishTracingClientInterceptor.Builder withVerbosity() {
            this.verbose = true;
            return this;
        }

        /**
         * Provide the active span source.
         *
         * @param activeSpanSource that provides a method of getting the active span before the client
         *                         call
         * @return this Builder configured to start client span as children of the span returned by
         * activeSpanSource.getActiveSpan()
         */
        public RadishTracingClientInterceptor.Builder withActiveSpanSource(ActiveSpanSource activeSpanSource) {
            this.activeSpanSource = activeSpanSource;
            return this;
        }

        /**
         * Provide the active span context source.
         *
         * @param activeSpanContextSource that provides a method of getting the active span context
         *                                before the client call
         * @return this Builder configured to start client span as children of the span context returned
         * by activeSpanContextSource.getActiveSpanContext()
         */
        public RadishTracingClientInterceptor.Builder withActiveSpanContextSource(ActiveSpanContextSource activeSpanContextSource) {
            this.activeSpanContextSource = activeSpanContextSource;
            return this;
        }

        /**
         * Decorates the client span with custom data.
         *
         * @param clientSpanDecorator used to decorate the client span
         * @return this builder configured to decorate the client span
         */
        public RadishTracingClientInterceptor.Builder withClientSpanDecorator(ClientSpanDecorator clientSpanDecorator) {
            this.clientSpanDecorators.put(clientSpanDecorator.getClass(), clientSpanDecorator);
            return this;
        }

        /**
         * Decorates the client span with custom data when the call is closed.
         *
         * @param clientCloseDecorator used to decorate the client span when the call is closed
         * @return this builder configured to decorate the client span when the call is closed
         */
        public RadishTracingClientInterceptor.Builder withClientCloseDecorator(ClientCloseDecorator clientCloseDecorator) {
            this.clientCloseDecorators.put(clientCloseDecorator.getClass(), clientCloseDecorator);
            return this;
        }

        /**
         * Build the RadishTracingClientInterceptor.
         *
         * @return a RadishTracingClientInterceptor with this Builder's configuration
         */
        public RadishTracingClientInterceptor build() {
            return new RadishTracingClientInterceptor(this);
        }
    }

    public enum ClientRequestAttribute {
        METHOD_TYPE,
        METHOD_NAME,
        DEADLINE,
        COMPRESSOR,
        AUTHORITY,
        ALL_CALL_OPTIONS,
        HEADERS
    }
}
