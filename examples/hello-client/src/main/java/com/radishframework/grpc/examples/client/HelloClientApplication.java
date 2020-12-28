package com.radishframework.grpc.examples.client;

import com.radishframework.grpc.server.annotations.EnableGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@EnableGrpc
@Configuration
@SpringBootApplication
public class HelloClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelloClientApplication.class, args);
    }
}
