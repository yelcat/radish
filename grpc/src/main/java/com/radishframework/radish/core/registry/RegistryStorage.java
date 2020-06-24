package com.radishframework.radish.core.registry;

import static com.radishframework.radish.core.common.Constant.ETCD_PATH_SPLITTER;
import static com.radishframework.radish.core.common.Constant.ETCD_SERVICES_INSTANCE_PATH_PREFIX;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.ibm.etcd.api.PutResponse;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.lease.LeaseClient;
import com.ibm.etcd.client.lease.PersistentLease;
import com.radishframework.radish.core.common.ServiceInstance;

public class RegistryStorage {

    private final KvClient kvClient;
    private final LeaseClient leaseClient;

    public RegistryStorage(KvClient kvClient, LeaseClient leaseClient) {
        this.kvClient = kvClient;
        this.leaseClient = leaseClient;
    }

    public ListenableFuture<PersistentLease> save(ServiceInstance instance) {
        final PersistentLease persistentLease = leaseClient.maintain().start();
        final ListenableFuture<ListenableFuture<PutResponse>> instanceFuture = Futures.transform(persistentLease, leaseId -> {
            final ByteString instanceKey = ByteString.copyFromUtf8(
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
