package com.radishframework.radish.server.springboot;

import com.radishframework.radish.client.ManagedChannelFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RadishProperties.class)
public class RadishConfiguration {

    @Bean
    ManagedChannelFactory managedChannelFactory(@Value("${spring.application.name}") String appName) {
        return new ManagedChannelFactory(appName);
    }
}