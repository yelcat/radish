package com.radishframework.radish.core.client;

import com.radishframework.radish.core.utils.GrpcReflectionUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverProvider;
import io.grpc.internal.SharedResourceHolder;
import io.opentracing.contrib.grpc.OpenTracingContextKey;
import io.opentracing.util.GlobalTracer;

public final class ManagedChannelResource implements SharedResourceHolder.Resource<ManagedChannel> {
    public final static String DEFAULT_LOAD_BALANCING_POLICY = "round_robin"; // 默认负载均衡策略
    public final static String KUBERNETES_URL_PREFIX = KubernetesNameResolverProvider.SCHEME + "://"; // kubernetes url前缀信息

    private final String serviceName;
    private final String appName;
    private final NameResolverProvider nameResolverProvider;

    ManagedChannelResource(String serviceName, String appName, NameResolverProvider nameResolverProvider) {
        this.serviceName = serviceName;
        this.appName = appName;
        this.nameResolverProvider = nameResolverProvider;
    }

    @Override
    public ManagedChannel create() {
        final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(KUBERNETES_URL_PREFIX + serviceName)
                .nameResolverFactory(nameResolverProvider)
                .userAgent(appName)
                .defaultLoadBalancingPolicy(DEFAULT_LOAD_BALANCING_POLICY)
                .usePlaintext();

            channelBuilder.intercept(
                    new RadishHeaderClientInterceptor(appName),
                    RadishTracingClientInterceptor.newBuilder()
                            .withTracer(GlobalTracer.get())
                            .withStreaming()
                            .withVerbosity()
                            .withActiveSpanSource(OpenTracingContextKey::activeSpan)
                            .withTracedAttributes(RadishTracingClientInterceptor.ClientRequestAttribute.ALL_CALL_OPTIONS)
                            .build());
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
