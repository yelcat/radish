package com.radishframework.radish.springboot.tracing.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lettuce
 */
@Configuration
@ConditionalOnClass(RedisClient.class)
@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration")
public class RadishLettuceConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(ClientResources.class)
    public DefaultClientResources lettuceClientResources() {
        return DefaultClientResources.builder()
                .tracing(new RadishRedisTracing())
                .build();
    }
}
