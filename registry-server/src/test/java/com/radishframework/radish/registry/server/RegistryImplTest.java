package com.radishframework.radish.registry.server;


import com.google.common.util.concurrent.Futures;
import com.ibm.etcd.client.lease.PersistentLease;
import com.radishframework.radish.core.common.ServiceInstance;
import com.radishframework.radish.registry.core.InstanceInfo;
import com.radishframework.radish.registry.core.RegisterRequest;
import com.radishframework.radish.registry.core.RegisterResponse;
import com.radishframework.radish.registry.core.RegistryStorage;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegistryImplTest {
    RegistryImpl registry;
    @Mock
    RegistryStorage registryStorage;
    @Mock
    StreamObserver<RegisterResponse> responseStreamObserver;
    @Captor
    ArgumentCaptor<RegisterResponse> responseCaptor;
    @Mock
    PersistentLease persistentLease;

    InstanceInfo testInstance = InstanceInfo.newBuilder()
            .setHostname("testhost")
            .setIp("127.0.0.1")
            .setPort(1234)
            .setOperationPort(1235)
            .build();
    com.radishframework.radish.core.common.InstanceInfo saveInstance = com.radishframework.radish.core.common.InstanceInfo.newBuilder()
            .setHostname("testhost")
            .setIp("127.0.0.1")
            .setPort(1234)
            .setOperationPort(1235)
            .setDatacenter("aliyun")
            .setSegment("unittest")
            .setDescName("testservice")
            .build();
    ServiceInstance serviceInstance = new ServiceInstance(saveInstance);

    RegisterRequest registerRequest = RegisterRequest.newBuilder()
            .setDescName("testservice")
            .setInstanceInfo(testInstance)
            .build();

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        registry = new RegistryImpl(registryStorage, "aliyun", "unittest");
    }

    @Test
    @DisplayName("测试服务注册过程")
    public void testRegister() {
        registry.register(registerRequest, responseStreamObserver);
        when(registryStorage.save(serviceInstance)).thenReturn(Futures.immediateFuture(persistentLease));

        verify(responseStreamObserver).onNext(responseCaptor.capture());
        var registerResponse = responseCaptor.getValue();
        assertTrue(registerResponse.getSuccess());
    }


}
