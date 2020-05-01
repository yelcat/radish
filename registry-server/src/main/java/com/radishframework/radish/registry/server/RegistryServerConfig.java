package com.radishframework.radish.registry.server;

import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.lease.LeaseClient;
import com.radishframework.radish.registry.core.RegistryGrpc;
import com.radishframework.radish.registry.core.RegistryStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RegistryServerProperties.class, RegistryServerProperties.EtcdConfig.class})
public class RegistryServerConfig {

    @Bean
    KvStoreClient kvStoreClient(RegistryServerProperties registryServerProperties) {
        var etcdConfig = registryServerProperties.getEtcd();
        return EtcdClient.forEndpoints(etcdConfig.getEndpoints())
                .withPlainText()
                .withCredentials(etcdConfig.getUsername(), etcdConfig.getPassword())
                .build();
    }

    @Bean
    KvClient kvClient(KvStoreClient kvStoreClient) {
        return kvStoreClient.getKvClient();
    }

    @Bean
    LeaseClient leaseClient(KvStoreClient kvStoreClient) {
        return kvStoreClient.getLeaseClient();
    }

    @Bean
    RegistryStorage registryStorage(KvClient kvClient, LeaseClient leaseClient, RegistryServerProperties serverProperties) {
        return new RegistryStorage(kvClient, leaseClient);
    }

    @Bean
    RegistryGrpc.RegistryImplBase registryImplBase(RegistryStorage registryStorage, RegistryServerProperties serverProperties) {
        return new RegistryImpl(registryStorage, serverProperties.getDatacenter(), serverProperties.getSegment());
    }
}
