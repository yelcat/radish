package com.radishframework.radish.core.client;

import com.radishframework.radish.core.utils.GrpcReflectionUtils;
import com.radishframework.radish.discovery.core.DiscoveryGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverProvider;
import io.grpc.internal.SharedResourceHolder;

import java.util.concurrent.ConcurrentHashMap;

import static com.radishframework.radish.core.common.Constant.DEFAULT_LOAD_BALANCING_POLICY;
import static com.radishframework.radish.core.common.Constant.OSTRICH_URL_PREFIX;

public class ManagedChannelFactory {

    private final String forAppName;
    private final NameResolverProvider nameResolverProvider;
    private final ConcurrentHashMap<String, ManagedChannelResource> serviceResources = new ConcurrentHashMap<>();

    public ManagedChannelFactory(String forAppName, String discoveryServerAddress, int discoveryServerPort) {
        this.forAppName = forAppName;

        final ManagedChannel bootstrapDiscoveryChannel = ManagedChannelBuilder
                .forAddress(discoveryServerAddress, discoveryServerPort)
                .usePlaintext()
                .build();
        final DiscoveryGrpc.DiscoveryBlockingStub bootstrapDiscovery = DiscoveryGrpc.newBlockingStub(bootstrapDiscoveryChannel);
        final ManagedChannelBuilder<?> realDiscoveryChannelBuilder = ManagedChannelBuilder
                .forTarget(OSTRICH_URL_PREFIX + DiscoveryGrpc.SERVICE_NAME)
                .nameResolverFactory(new RadishNameResolverProvider(bootstrapDiscovery))
                .userAgent(forAppName)
                .defaultLoadBalancingPolicy(DEFAULT_LOAD_BALANCING_POLICY)
                .usePlaintext();
        GrpcReflectionUtils.disableStatsAndTracingModule(realDiscoveryChannelBuilder);
        this.nameResolverProvider = new RadishNameResolverProvider(
                DiscoveryGrpc.newBlockingStub(realDiscoveryChannelBuilder.build()));
    }

    public ManagedChannel create(String serviceName) {
        final ManagedChannelResource channelResource =
                serviceResources.computeIfAbsent(serviceName,
                        (serviceName2) -> new ManagedChannelResource(serviceName2, forAppName,
                                nameResolverProvider));
        return SharedResourceHolder.get(channelResource);
    }

    public void releaseChannel(String serviceName, ManagedChannel channel) {
        ManagedChannelResource channelResource = serviceResources.get(serviceName);
        if (channelResource != null) {
            SharedResourceHolder.release(channelResource, channel);
        }
    }
}
