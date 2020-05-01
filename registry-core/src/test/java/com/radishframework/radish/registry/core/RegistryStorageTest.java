package com.radishframework.radish.registry.core;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ibm.etcd.api.RangeResponse;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.kv.KvClient;
import com.radishframework.radish.core.common.InstanceInfo;
import com.radishframework.radish.core.common.ServiceInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class RegistryStorageTest {
    public static final String SERVICE_NAME = "ServiceName";
    private RegistryStorage registryStorage;
    private EtcdClient etcdClient;
    private KvClient kvClient;

    @BeforeEach
    void init() {
        etcdClient = EtcdClient.forEndpoints(
                Lists.newArrayList(
                        "http://172.16.10.241:12379",
                        "http://172.16.10.242:12379",
                        "http://172.16.10.243:12379"))
                .withPlainText()
                .withCredentials("service_registry", "Services@ypsx123")
                .build();
        kvClient = etcdClient.getKvClient();

        registryStorage = new RegistryStorage(kvClient, etcdClient.getLeaseClient());
    }

    @Test
    @DisplayName("测试ETCD配置中心写入")
    public void testWrite() throws ExecutionException, InterruptedException, InvalidProtocolBufferException {
        var instanceInfo = InstanceInfo.newBuilder()
                .setIp("127.0.0.1")
                .setPort(8005)
                .setHostname("testcomputer")
                .setDescName(SERVICE_NAME)
                .setOperationPort(8909)
                .setDatacenter("aliyun")
                .setSegment("unittest")
                .setAppName("appname")
                .build();
        var leaseFuture = registryStorage.save(new ServiceInstance(instanceInfo));
        var lease = leaseFuture.get();
        assertNotNull(lease);
        assertTrue(lease.getLeaseId() > 0);

        final var instanceKey = ByteString.copyFromUtf8(
                "/radish/instances/aliyun/unittest/" + SERVICE_NAME);
        final RangeResponse rangeResp = kvClient.get(instanceKey).asPrefix().sync();
        assertTrue(rangeResp.getKvsCount() > 0);

        var kv = rangeResp.getKvs(0);
        var newInstanceInfo = InstanceInfo.parseFrom(kv.getValue());
        assertEquals(instanceInfo, newInstanceInfo);
    }
}
