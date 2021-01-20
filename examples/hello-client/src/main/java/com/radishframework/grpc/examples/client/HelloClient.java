package com.radishframework.grpc.examples.client;

import com.google.common.base.Splitter;
import com.radishframework.grpc.client.ManagedChannelFactory;
import com.radishframework.grpc.examples.hello.GreeterRequest;
import com.radishframework.grpc.examples.hello.GreeterResponse;
import com.radishframework.grpc.examples.hello.HelloGrpc;
import io.grpc.ManagedChannel;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class HelloClient {
    private static final Logger log = LoggerFactory.getLogger(HelloClient.class);

    @Autowired
    private ManagedChannelFactory managedChannelFactory;

    @PostConstruct
    public void init() {
        final ManagedChannel channel = managedChannelFactory.create("/default/helloservice/8506");
        final HelloGrpc.HelloBlockingStub stub = HelloGrpc.newBlockingStub(channel);
        final GreeterResponse response = stub.greeter(GreeterRequest.newBuilder()
                .setName("Harry")
                .build());
        log.info("invoke result: " + response.getResult());
    }
}

