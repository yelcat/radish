package com.radishframework.grpc.client;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.*;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.internal.SharedResourceHolder;

import javax.annotation.concurrent.GuardedBy;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KubernetesNameResolver extends NameResolver {
    private final String namespace;
    private final String name;
    private final int port;
    private final SharedResourceHolder.Resource<ScheduledExecutorService> timerServiceResource;
    private final KubernetesClient kubernetesClient;
    private Listener listener;

    private volatile boolean refreshing = false;
    private volatile boolean watching = false;

    public KubernetesNameResolver(
            final String namespace, final String name, final int port,
            final SharedResourceHolder.Resource<ScheduledExecutorService> timerServiceResource,
            final KubernetesClient kubernetesClient) {
        this.namespace = namespace;
        this.name = name;
        this.port = port;
        this.timerServiceResource = timerServiceResource;
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public String getServiceAuthority() {
        return kubernetesClient.getMasterUrl().getAuthority();
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        refresh();
    }

    @Override
    public void shutdown() {
        kubernetesClient.close();
    }

    @Override
    @GuardedBy("this")
    public void refresh() {
        if (refreshing) return;
        try {
            refreshing = true;

            final Endpoints endpoints = kubernetesClient.endpoints()
                    .inNamespace(namespace)
                    .withName(name)
                    .get();

            if (endpoints == null) {
                // Didn't find anything, retrying
                final ScheduledExecutorService timerService = SharedResourceHolder.get(timerServiceResource);
                timerService.schedule(this::refresh, 30, TimeUnit.SECONDS);
                return;
            }

            update(endpoints);
            watch();
        } finally {
            refreshing = false;
        }
    }

    private void update(Endpoints endpoints) {
        if (endpoints.getSubsets() == null) return;

        final List<EquivalentAddressGroup> servers = endpoints.getSubsets()
                .stream()
                .filter(subset -> subset.getPorts().stream().anyMatch(p -> p != null && p.getPort() == port))
                .flatMap(subset ->
                        subset.getAddresses()
                                .stream()
                                .map(address -> new EquivalentAddressGroup(
                                        new InetSocketAddress(address.getIp(), port))))
                .collect(Collectors.toList());

        listener.onAddresses(servers, Attributes.EMPTY);
    }

    @GuardedBy("this")
    protected void watch() {
        if (watching) return;
        watching = true;

        kubernetesClient.endpoints()
                .inNamespace(namespace)
                .withName(name)
                .watch(new Watcher<Endpoints>() {
                    @Override
                    public void eventReceived(Action action, Endpoints endpoints) {
                        switch (action) {
                            case MODIFIED:
                            case ADDED:
                                update(endpoints);
                                return;
                            case DELETED:
                                listener.onAddresses(Collections.emptyList(), Attributes.EMPTY);
                        }
                    }

                    @Override
                    public void onClose(KubernetesClientException e) {
                        watching = false;
                    }
                });
    }
}
