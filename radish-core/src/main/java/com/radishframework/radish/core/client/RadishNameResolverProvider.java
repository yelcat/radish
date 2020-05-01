package com.radishframework.radish.core.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.radishframework.radish.discovery.core.DiscoverRequest;
import com.radishframework.radish.discovery.core.DiscoverResponse;
import com.radishframework.radish.discovery.core.DiscoveryGrpc;
import com.radishframework.radish.discovery.core.InstanceInfo;
import io.grpc.*;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * usage: radish://{service-name}/?{attr-key1}={attr-value1}&amp;{attr-key2}={attr-value2}
 */
public class RadishNameResolverProvider extends NameResolverProvider {

    public static final String SCHEME = "radish";

    public static final Logger logger = LoggerFactory.getLogger(RadishNameResolverProvider.class);

    private final DiscoveryGrpc.DiscoveryBlockingStub discoveryStub;

    public RadishNameResolverProvider(@Nonnull final DiscoveryGrpc.DiscoveryBlockingStub discoveryStub) {
        this.discoveryStub = discoveryStub;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, final NameResolver.Args args) {
        if (!SCHEME.equals(targetUri.getScheme())) {
            return null;
        }

        final String authority = requireNonNull(targetUri.getAuthority(), "authority");
        final String query = targetUri.getQuery() == null ? "" : targetUri.getQuery();
        return new RadishNameResolver(authority, query, discoveryStub,
                GrpcUtil.SHARED_CHANNEL_EXECUTOR, GrpcUtil.TIMER_SERVICE);
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

    @VisibleForTesting
    static final class RadishNameResolver extends NameResolver {

        // 默认5分钟重新刷新一次
        @VisibleForTesting
        static int REFRESH_INTERVAL = 300;
        private final SharedResourceHolder.Resource<Executor> executorResource;
        private final SharedResourceHolder.Resource<ScheduledExecutorService> scheduledExecutorResource;
        private final Runnable resolveRunnable = new Resolve(this);
        private final DiscoveryGrpc.DiscoveryBlockingStub discoveryStub;
        private Listener listener;
        private String authority;
        @GuardedBy("this")
        private boolean shutdown;
        @GuardedBy("this")
        private Executor executor;
        @GuardedBy("this")
        private ScheduledExecutorService scheduledExecutor;
        @GuardedBy("this")
        private boolean resolving;

        private RadishNameResolver(@Nonnull String authority, String query,
                                    DiscoveryGrpc.DiscoveryBlockingStub discoveryStub,
                                    SharedResourceHolder.Resource<Executor> executorResource,
                                    SharedResourceHolder.Resource<ScheduledExecutorService> scheduledExecutorResource) {
            this.authority =
                    (query != null && !query.isEmpty()) ? authority + "?" + query : authority;
            this.discoveryStub = discoveryStub;
            this.executorResource = executorResource;
            this.scheduledExecutorResource = scheduledExecutorResource;
        }

        @Override
        public String getServiceAuthority() {
            return authority;
        }

        @Override
        public final synchronized void start(Listener listener) {
            Preconditions.checkState(this.listener == null, "already started");

            this.executor = SharedResourceHolder.get(executorResource);
            this.scheduledExecutor = SharedResourceHolder.get(scheduledExecutorResource);
            this.listener = checkNotNull(listener, "listener");

            resolve();
        }

        @Override
        public final synchronized void refresh() {
            Preconditions.checkState(listener != null, "not started");
            resolve();
        }

        @Override
        public final synchronized void shutdown() {
            if (shutdown) {
                return;
            }
            shutdown = true;

            if (executor != null) {
                executor = SharedResourceHolder.release(executorResource, executor);
            }

            if (scheduledExecutor != null) {
                scheduledExecutor = SharedResourceHolder
                        .release(scheduledExecutorResource, scheduledExecutor);
            }
        }

        @GuardedBy("this")
        private void resolve() {
            if (resolving || shutdown) {
                return;
            }

            executor.execute(resolveRunnable);
        }

        private final class Resolve implements Runnable {

            private final RadishNameResolver resolver;

            Resolve(RadishNameResolver resolver) {
                this.resolver = resolver;
            }

            @Override
            public void run() {
                if (logger.isTraceEnabled()) {
                    logger.trace("Attempting radish resolution of {}", resolver.authority);
                }

                Listener savedListener;
                synchronized (resolver) {
                    if (resolver.shutdown) {
                        return;
                    }
                    savedListener = resolver.listener;
                    resolver.resolving = true;
                }

                try {
                    resolveInternal(savedListener);

                    // auto refresh after 5 minutes for discovery new service providers
                    resolver.scheduledExecutor
                            .schedule(resolver::refresh, REFRESH_INTERVAL, TimeUnit.SECONDS);
                } finally {
                    synchronized (resolver) {
                        resolver.resolving = false;
                    }
                }
            }

            private void resolveInternal(Listener savedListener) {
                final DiscoverResponse discoverResponse;
                try {
                    final DiscoverRequest discoverRequest = DiscoverRequest.newBuilder()
                            .setDescName(resolver.authority)
                            .build();
                    discoverResponse = RadishNameResolver.this.discoveryStub.discover(discoverRequest);
                } catch (Exception e) {
                    logger.error("resolve internal error ", e);

                    savedListener.onError(
                            Status.UNAVAILABLE
                                    .withCause(e)
                                    .withDescription("Unable to resolve service info "
                                            + resolver.authority));
                    return;
                }

                if (discoverResponse == null
                        || discoverResponse.getInstanceList() == null
                        || discoverResponse.getInstanceList().isEmpty()) {
                    savedListener.onError(
                            Status.UNAVAILABLE.withDescription(
                                    "Unable to resolve service info " + resolver.authority));
                    return;
                }

                final List<InstanceInfo> instanceInfoList = discoverResponse.getInstanceList();
                if (logger.isInfoEnabled()) {
                    for (InstanceInfo instanceInfo : instanceInfoList) {
                        logger.info("service {} provider: {}:{}", resolver.authority,
                                instanceInfo.getIp(), instanceInfo.getPort());
                    }
                }

                final List<EquivalentAddressGroup> serviceAddresses = instanceInfoList
                        .stream()
                        .map(instanceInfo -> {
                            final InetSocketAddress socketAddress = new InetSocketAddress(
                                    instanceInfo.getIp(), instanceInfo.getPort());
                            return new EquivalentAddressGroup(socketAddress);
                        }).collect(Collectors.toList());

                savedListener.onAddresses(serviceAddresses, Attributes.EMPTY);
            }
        }
    }
}
