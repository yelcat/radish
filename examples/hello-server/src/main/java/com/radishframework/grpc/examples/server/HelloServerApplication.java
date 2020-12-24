package com.radishframework.grpc.examples.server;

import com.radishframework.grpc.server.annotations.EnableGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableGrpc
@SpringBootApplication
@SpringBootConfiguration
public class HelloServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelloServerApplication.class, args);
    }
}
