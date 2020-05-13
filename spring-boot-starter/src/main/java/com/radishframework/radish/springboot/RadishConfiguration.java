package com.radishframework.radish.springboot;

import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.lease.LeaseClient;
import com.radishframework.radish.core.client.ManagedChannelFactory;
import com.radishframework.radish.core.discovery.ServiceDiscovery;
import com.radishframework.radish.core.registry.RegistryStorage;
import com.radishframework.radish.springboot.RadishProperties.RadishEtcdProperties;
import com.radishframework.radish.springboot.tracing.RadishTraceAspect;
import com.radishframework.radish.springboot.tracing.lettuce.RadishLettuceConfig;
import com.radishframework.radish.springboot.tracing.mybatis.RadishMybatisConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RadishMybatisConfig.class, RadishLettuceConfig.class})
@EnableConfigurationProperties(RadishProperties.class)
public class RadishConfiguration {

    @Bean
    ManagedChannelFactory managedChannelFactory(@Value("${spring.application.name}") String appName,
        ServiceDiscovery serviceDiscovery) {
        return new ManagedChannelFactory(appName, serviceDiscovery);
    }

    @Bean
    RadishTraceAspect traceAspect() {
        return new RadishTraceAspect();
    }

    @Bean
    KvStoreClient kvStoreClient(RadishProperties radishProperties) {
        final RadishEtcdProperties etcdConfig = radishProperties.getEtcd();
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
    RegistryStorage registryStorage(KvClient kvClient, LeaseClient leaseClient) {
        return new RegistryStorage(kvClient, leaseClient);
    }

}
