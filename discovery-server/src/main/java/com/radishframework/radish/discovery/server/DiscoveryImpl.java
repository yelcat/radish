package com.radishframework.radish.discovery.server;

import com.radishframework.radish.core.common.ServiceInstance;
import com.radishframework.radish.discovery.core.DiscoverRequest;
import com.radishframework.radish.discovery.core.DiscoverResponse;
import com.radishframework.radish.discovery.core.DiscoveryGrpc;
import io.grpc.stub.StreamObserver;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiscoveryImpl extends DiscoveryGrpc.DiscoveryImplBase {

    private final ServiceDiscovery serviceDiscovery;

    public DiscoveryImpl(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public void discover(DiscoverRequest request, StreamObserver<DiscoverResponse> responseObserver) {
        Optional<Collection<ServiceInstance>> serviceInstances;
        try {
            serviceInstances = serviceDiscovery.find(request.getDescName());
        } catch (Exception e) {
            responseObserver.onError(e);
            return;
        }

        if (serviceInstances.isEmpty()) {
            DiscoverResponse.Builder builder = DiscoverResponse.newBuilder()
                    .setSuccess(false);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            return;
        }

        final DiscoverResponse.Builder builder = DiscoverResponse.newBuilder()
                .setSuccess(true);
        final var instances = serviceInstances.get()
                .stream()
                .map(instanceInfo ->
                        com.radishframework.radish.discovery.core.InstanceInfo.newBuilder()
                                .setOperationPort(instanceInfo.getOperationPort())
                                .setPort(instanceInfo.getPort())
                                .setHostname(instanceInfo.getHostname())
                                .setIp(instanceInfo.getIp())
                                .setAppName(instanceInfo.getAppName())
                                .build())
                .collect(Collectors.toList());
        builder.addAllInstance(instances);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
