package com.radishframework.radish.core.client;

import com.google.common.collect.ImmutableSet;
import com.radishframework.radish.core.utils.GrpcReflectionUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverProvider;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.internal.SharedResourceHolder;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.opentracing.contrib.grpc.OpenTracingContextKey;
import io.opentracing.util.GlobalTracer;

import static com.radishframework.radish.core.common.Constant.DEFAULT_LOAD_BALANCING_POLICY;
import static com.radishframework.radish.core.common.Constant.OSTRICH_URL_PREFIX;

public final class ManagedChannelResource implements SharedResourceHolder.Resource<ManagedChannel> {

    private final String serviceName;
    private final String appName;
    private final NameResolverProvider nameResolverProvider;

    private static final ImmutableSet<String> tracingIgnoreServices = ImmutableSet
            .of(HealthGrpc.SERVICE_NAME, ServerReflectionGrpc.SERVICE_NAME,
                    "com.radishframework.radish.registry.core.Registry", "com.radishframework.radish.discovery.core.Discovery");

    ManagedChannelResource(String serviceName, String appName, NameResolverProvider nameResolverProvider) {
        this.serviceName = serviceName;
        this.appName = appName;
        this.nameResolverProvider = nameResolverProvider;
    }

    @Override
    public ManagedChannel create() {
        final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(OSTRICH_URL_PREFIX + serviceName)
                .nameResolverFactory(nameResolverProvider)
                .userAgent(appName)
                .defaultLoadBalancingPolicy(DEFAULT_LOAD_BALANCING_POLICY)
                .usePlaintext();

        if (!tracingIgnoreServices.contains(serviceName)) {
            channelBuilder.intercept(
                    new RadishHeaderClientInterceptor(appName),
                    RadishTracingClientInterceptor.newBuilder()
                            .withTracer(GlobalTracer.get())
                            .withStreaming()
                            .withVerbosity()
                            .withActiveSpanSource(OpenTracingContextKey::activeSpan)
                            .withTracedAttributes(RadishTracingClientInterceptor.ClientRequestAttribute.ALL_CALL_OPTIONS)
                            .build());
        }
        GrpcReflectionUtils.disableStatsAndTracingModule(channelBuilder);
        return channelBuilder.build();
    }

    @Override
    public void close(ManagedChannel instance) {
        if (instance != null && !instance.isShutdown() && !instance.isTerminated()) {
            instance.shutdown();
        }
    }
}
