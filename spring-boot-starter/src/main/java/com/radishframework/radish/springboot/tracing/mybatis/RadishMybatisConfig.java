package com.radishframework.radish.springboot.tracing.mybatis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = {"org.apache.ibatis.session.SqlSessionFactory",
        "org.apache.ibatis.plugin.Interceptor"})
public class RadishMybatisConfig {

    @Bean
    MyBatisTracingIntercepter mybatisIntercept() {
        return new MyBatisTracingIntercepter();
    }
}
