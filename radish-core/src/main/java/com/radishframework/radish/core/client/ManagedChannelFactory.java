package com.radishframework.radish.core.client;

import com.radishframework.radish.core.discovery.ServiceDiscovery;
import io.grpc.ManagedChannel;
import io.grpc.NameResolverProvider;
import io.grpc.internal.SharedResourceHolder;

import java.util.concurrent.ConcurrentHashMap;

public class ManagedChannelFactory {

    private final String forAppName;
    private final NameResolverProvider nameResolverProvider;
    private final ConcurrentHashMap<String, ManagedChannelResource> serviceResources = new ConcurrentHashMap<>();

    public ManagedChannelFactory(String forAppName, ServiceDiscovery serviceDiscovery) {
        this.forAppName = forAppName;

        this.nameResolverProvider = new RadishNameResolverProvider(serviceDiscovery);
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
