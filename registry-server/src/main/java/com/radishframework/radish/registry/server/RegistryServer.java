package com.radishframework.radish.registry.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableAutoConfiguration
@Import({
        RegistryServerRunner.class
})
public class RegistryServer {
    public static void main(String[] args) {
        SpringApplication.run(RegistryServer.class, args);
    }
}
