package com.radishframework.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverProvider;
import io.grpc.NameResolverRegistry;
import io.grpc.internal.SharedResourceHolder;

public final class ManagedChannelResource implements SharedResourceHolder.Resource<ManagedChannel> {
    public final static String DEFAULT_LOAD_BALANCING_POLICY = "round_robin"; // 默认负载均衡策略
    public final static String KUBERNETES_URL_PREFIX = KubernetesNameResolverProvider.SCHEME + "://"; // kubernetes url前缀信息

    private final String serviceName;
    private final String appName;

    ManagedChannelResource(String appName, String serviceName) {
        this.appName = appName;
        this.serviceName = serviceName;
    }

    @Override
    public ManagedChannel create() {
        final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(KUBERNETES_URL_PREFIX + serviceName)
                .userAgent(appName)
                .defaultLoadBalancingPolicy(DEFAULT_LOAD_BALANCING_POLICY)
                .usePlaintext();

            channelBuilder.intercept(new OpenTelemetryClientInterceptor());

        return channelBuilder.build();
    }

    @Override
    public void close(ManagedChannel instance) {
        if (instance != null && !instance.isShutdown() && !instance.isTerminated()) {
            instance.shutdown();
        }
    }
}
