package com.radishframework.radish.discovery.server;

import com.google.common.base.Joiner;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import com.radishframework.radish.core.common.InstanceInfo;
import com.radishframework.radish.core.common.ServiceInstance;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.LogExceptionRunnable;
import io.grpc.internal.SharedResourceHolder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static com.radishframework.radish.core.common.Constant.ETCD_PATH_SPLITTER;
import static com.radishframework.radish.core.common.Constant.ETCD_SERVICES_INSTANCE_PATH_PREFIX;

public class ServiceDiscovery {
    public final static Logger log = LoggerFactory.getLogger(ServiceDiscovery.class);

    /* etcd key path like /radish/instances/com.radishframework.GreeterService/172.10.32.89:8006 */
    private final String instancesPrefix;
    private final KvClient client;
    private final Executor channelExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    /* 本地内部服务实例信息副本 */
    private final ConcurrentMap<String/* description name */, Map<String/* instance id */, ServiceInstance>> serviceInstancesRegistry =
            new ConcurrentHashMap<>();

    private KvClient.Watch watcher;

    public ServiceDiscovery(KvClient kvClient, String datacenter, String segment) {
        this.client = kvClient;
        this.instancesPrefix =
                Joiner.on(ETCD_PATH_SPLITTER)
                        .join(ETCD_SERVICES_INSTANCE_PATH_PREFIX,
                                datacenter,
                                segment);
        this.scheduledExecutor = SharedResourceHolder.get(GrpcUtil.TIMER_SERVICE);
        this.channelExecutor = SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);
    }

    @PostConstruct
    public void init() {
        // 预加载当前环境下的所有注册信息
        preload();
        // 监听当前环境下注册信息的变更
        watch();
    }

    @PreDestroy
    public void close() {
        if (watcher != null) {
            watcher.close();
        }
    }

    /**
     * @param serviceName
     * @return
     */
    public Optional<Collection<ServiceInstance>> find(String serviceName) {
        return Optional.ofNullable(serviceInstancesRegistry.get(serviceName))
                .map(Map::values);
    }

    private void watchAgain() {
        if (watcher != null) {
            watcher.close();
            watcher = null;
        }

        watch();
    }

    private void watch() {
        this.scheduledExecutor.schedule(() ->
                        this.channelExecutor.execute(
                                new LogExceptionRunnable(() -> {
                                    watcher = client.watch(ByteString.copyFromUtf8(instancesPrefix))
                                            .asPrefix()
                                            .start(new WatchObserver());
                                })),
                10, TimeUnit.MILLISECONDS);
    }

    private void preload() {
        final var response = client.get(ByteString.copyFromUtf8(instancesPrefix))
                .asPrefix()
                .sync();
        for (KeyValue kv : response.getKvsList()) {
            parseAndAddLocalRegistry(kv);
        }
    }


    private void parseAndAddLocalRegistry(KeyValue kv) {
        final ServiceInstance instance;
        try {
            instance = new ServiceInstance(InstanceInfo.parseFrom(kv.getValue()));
        } catch (InvalidProtocolBufferException e) {
            log.error("Instance info format error from etcd", e);
            return;
        }

        final var serviceName = instance.getDescName();
        final var instanceId = instance.getInstanceId();

        serviceInstancesRegistry.computeIfAbsent(serviceName, desc_name -> {
            var instances = new ConcurrentHashMap<String, ServiceInstance>();
            instances.put(instanceId, instance);
            return instances;
        });

        serviceInstancesRegistry.computeIfPresent(serviceName, (desc_name, instances) -> {
            instances.put(instanceId, instance);
            return instances;
        });
    }

    /**
     * etcd watch job observer
     */
    private class WatchObserver implements StreamObserver<WatchUpdate> {
        @Override
        public void onNext(WatchUpdate watchUpdate) {
            for (Event event : watchUpdate.getEvents()) {
                if (log.isInfoEnabled()) {
                    log.info("etcd event happend, type " + event.getType() + ", kv " + event.getKv());
                }

                final var kv = event.getKv();
                switch (event.getType()) {
                    case PUT:
                        parseAndAddLocalRegistry(kv);
                        break;
                    case DELETE:
                        final var instanceKey = kv.getKey().toStringUtf8();
                        final var lastSlash = instanceKey.lastIndexOf(ETCD_PATH_SPLITTER);
                        final var descName = instanceKey.substring(instancesPrefix.length() + 1, lastSlash);
                        final var instanceId = instanceKey.substring(lastSlash + 1);
                        serviceInstancesRegistry.computeIfPresent(descName, (desc, instanceInfos) -> {
                            instanceInfos.remove(instanceId);
                            return instanceInfos;
                        });
                        break;
                    case UNRECOGNIZED:
                        log.error("Unrecognized watch event type!");
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("Etcd watch error", throwable);

            watchAgain();
        }

        @Override
        public void onCompleted() {
            log.error("Etcd watch unexpected completed");

            watchAgain();
        }
    }

}
