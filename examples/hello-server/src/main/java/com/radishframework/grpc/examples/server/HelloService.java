package com.radishframework.grpc.examples.server;

import com.radishframework.grpc.examples.hello.*;
import com.radishframework.grpc.server.annotations.GrpcService;
import io.grpc.stub.StreamObserver;

@GrpcService
public class HelloService extends HelloGrpc.HelloImplBase {
    @Override
    public void greeter(GreeterRequest request, StreamObserver<GreeterResponse> responseObserver) {
        final String who = request.getName();
        responseObserver.onNext(GreeterResponse.newBuilder()
                .setResult("Hello " + who)
                .build());
        responseObserver.onCompleted();
    }
}
