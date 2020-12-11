package com.radishframework.radish.core.common;

import io.grpc.*;
import io.grpc.inprocess.InProcessSocketAddress;
import io.opentracing.Span;
import io.opentracing.tag.AbstractTag;
import io.opentracing.tag.Tag;
import io.opentracing.tag.Tags;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public final class RadishGrpcTags {

    /**
     * grpc.authority tag.
     */
    public static final RadishGrpcTags.NullableTag<String> GRPC_AUTHORITY = new RadishGrpcTags.NullableTag<>("grpc.authority");

    /**
     * grpc.call_attributes tag.
     */
    public static final RadishGrpcTags.NullableTag<Attributes> GRPC_CALL_ATTRIBUTES =
            new RadishGrpcTags.NullableTag<>("grpc.call_attributes");

    /**
     * grpc.call_options tag.
     */
    public static final RadishGrpcTags.NullableTag<CallOptions> GRPC_CALL_OPTIONS = new RadishGrpcTags.NullableTag<>("grpc.call_options");

    /**
     * grpc.compressor tag.
     */
    public static final RadishGrpcTags.NullableTag<String> GRPC_COMPRESSOR = new RadishGrpcTags.NullableTag<>("grpc.compressor");

    /**
     * grpc.deadline_millis tag.
     */
    public static final Tag<Deadline> GRPC_DEADLINE =
            new AbstractTag<Deadline>("grpc.deadline_millis") {
                @Override
                public void set(Span span, Deadline deadline) {
                    if (deadline != null) {
                        span.setTag(super.key, String.valueOf(deadline.timeRemaining(TimeUnit.MILLISECONDS)));
                    }
                }
            };

    /**
     * grpc.headers tag.
     */
    public static final RadishGrpcTags.NullableTag<Metadata> GRPC_HEADERS = new RadishGrpcTags.NullableTag<>("grpc.headers");

    /**
     * grpc.method_name tag.
     */
    public static final Tag<MethodDescriptor> GRPC_METHOD_NAME =
            new AbstractTag<MethodDescriptor>("grpc.method_name") {
                @Override
                public void set(Span span, MethodDescriptor method) {
                    if (method != null) {
                        span.setTag(super.key, method.getFullMethodName());
                    }
                }
            };

    /**
     * grpc.method_type tag.
     */
    public static final Tag<MethodDescriptor> GRPC_METHOD_TYPE =
            new AbstractTag<MethodDescriptor>("grpc.method_type") {
                @Override
                public void set(Span span, MethodDescriptor method) {
                    if (method != null) {
                        span.setTag(super.key, method.getType().toString());
                    }
                }
            };

    /**
     * grpc.status tag.
     */
    public static final Tag<Status> GRPC_STATUS =
            new AbstractTag<Status>("grpc.status") {
                @Override
                public void set(Span span, Status status) {
                    if (status != null) {
                        span.setTag(super.key, status.getCode().name());
                    }
                }
            };

    /**
     * peer.address tag.
     */
    public static final Tag<Attributes> PEER_ADDRESS =
            new AbstractTag<Attributes>("peer.address") {
                @Override
                public void set(Span span, Attributes attributes) {
                    SocketAddress address = attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                    if (address instanceof InProcessSocketAddress) {
                        span.setTag(super.key, ((InProcessSocketAddress) address).getName());
                    } else if (address instanceof InetSocketAddress) {
                        final InetSocketAddress inetAddress = (InetSocketAddress) address;
                        span.setTag(super.key, inetAddress.getHostString() + ':' + inetAddress.getPort());
                    }
                }
            };

    /**
     * Value for {@link Tags#COMPONENT} for gRPC.
     */
    public static final String COMPONENT_NAME = "java-grpc";

    public static class NullableTag<T> extends AbstractTag<T> {

        NullableTag(String tagKey) {
            super(tagKey);
        }

        @Override
        public void set(Span span, T tagValue) {
            if (tagValue != null) {
                span.setTag(super.key, tagValue.toString());
            }
        }
    }
}
