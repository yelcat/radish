package com.radishframework.radish.registry.core;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.lease.LeaseClient;
import com.ibm.etcd.client.lease.PersistentLease;
import com.radishframework.radish.core.common.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.radishframework.radish.core.common.Constant.ETCD_PATH_SPLITTER;
import static com.radishframework.radish.core.common.Constant.ETCD_SERVICES_INSTANCE_PATH_PREFIX;

public class RegistryStorage {
    private final static Logger log = LoggerFactory.getLogger(RegistryStorage.class);

    private final KvClient kvClient;
    private final LeaseClient leaseClient;

    public RegistryStorage(KvClient kvClient, LeaseClient leaseClient) {
        this.kvClient = kvClient;
        this.leaseClient = leaseClient;
    }

    public ListenableFuture<PersistentLease> save(ServiceInstance instance) {
        final var persistentLease = leaseClient.maintain().start();
        final var instanceFuture = Futures.transform(persistentLease, leaseId -> {
            final var instanceKey = ByteString.copyFromUtf8(
                    Joiner.on(ETCD_PATH_SPLITTER)
                            .join(ETCD_SERVICES_INSTANCE_PATH_PREFIX,
                                    instance.getDatacenter(),
                                    instance.getSegment(),
                                    instance.getDescName(),
                                    instance.getInstanceId()));
            assert leaseId != null;
            return kvClient.put(instanceKey, instance.toByteString(), leaseId).async();
        }, MoreExecutors.directExecutor());

        return Futures.transform(
                instanceFuture, response -> persistentLease, MoreExecutors.directExecutor());
    }

}
