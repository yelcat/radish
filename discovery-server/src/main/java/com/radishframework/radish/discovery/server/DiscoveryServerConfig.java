package com.radishframework.radish.discovery.server;

import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.lease.LeaseClient;
import com.radishframework.radish.discovery.core.DiscoveryGrpc;
import com.radishframework.radish.registry.core.RegistryStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscoveryServerConfig {

    @Bean
    KvStoreClient client(DiscoveryServerProperties properties) {
        DiscoveryServerProperties.EtcdConfig etcd = properties.getEtcd();
        return EtcdClient.forEndpoints(etcd.getEndpoints())
                .withPlainText()
                .withCredentials(etcd.getUsername(), etcd.getPassword())
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
    ServiceDiscovery serviceDiscovery(DiscoveryServerProperties properties, KvClient kvClient) {
        return new ServiceDiscovery(kvClient, properties.getDatacenter(), properties.getSegment());
    }

    @Bean
    DiscoveryGrpc.DiscoveryImplBase discoveryImplBase(ServiceDiscovery serviceDiscovery) {
        return new DiscoveryImpl(serviceDiscovery);
    }

    @Bean
    RegistryStorage registryStorage(KvClient kvClient, LeaseClient leaseClient) {
        return new RegistryStorage(kvClient, leaseClient);
    }
}
