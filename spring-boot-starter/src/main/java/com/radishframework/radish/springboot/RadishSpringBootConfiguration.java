package com.radishframework.radish.springboot;

import com.radishframework.radish.core.client.ManagedChannelFactory;
import com.radishframework.radish.core.client.RadishNameResolverProvider;
import com.radishframework.radish.discovery.core.DiscoveryGrpc;
import com.radishframework.radish.registry.core.RegistryGrpc;
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
@EnableConfigurationProperties(RadishSpringBootProperties.class)
public class RadishSpringBootConfiguration {

    @Bean
    ManagedChannelFactory managedChannelFactory(@Value("${spring.application.name}") String appName,
                                                RadishSpringBootProperties radishSpringBootProperties) {
        final RadishSpringBootProperties.RadishDiscoveryProperties discoveryProperties = radishSpringBootProperties.getDiscovery();
        return new ManagedChannelFactory(appName,
                discoveryProperties.getServerAddress(), discoveryProperties.getServerPort());
    }

    @Bean
    RadishTraceAspect traceAspect() {
        return new RadishTraceAspect();
    }

    @Bean
    RegistryGrpc.RegistryBlockingStub registryBlockingStub(ManagedChannelFactory managedChannelFactory) {
        return RegistryGrpc.newBlockingStub(managedChannelFactory.create(RegistryGrpc.SERVICE_NAME));
    }
}
