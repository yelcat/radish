package com.radishframework.radish.core.registry;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.ibm.etcd.client.lease.PersistentLease;
import com.radishframework.radish.core.common.InstanceInfo;
import com.radishframework.radish.core.common.ServiceInstance;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;

public class ServiceRegistry {

  private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);

  private final RegistryStorage registryStorage;

  private final ConcurrentHashMap<String/* service instance id */, HealthCheckStream> healthCheckerRegistry = new ConcurrentHashMap<>();

  public ServiceRegistry(RegistryStorage registryStorage) {
    this.registryStorage = registryStorage;
  }

  /**
   * <pre>
   * 注册一个服务
   * </pre>
   */
  public void register(InstanceInfo instanceInfo) {
    final ServiceInstance serviceInstance = new ServiceInstance(instanceInfo);
    healthCheckerRegistry.computeIfAbsent(serviceInstance.getInstanceId(), key -> {
      final HealthCheckStream hcStream = new HealthCheckStream(serviceInstance, healthCheckerRegistry);
      hcStream.start();
      return hcStream;
    });
  }

  private class HealthCheckStream implements StreamObserver<HealthCheckResponse> {
    private final ServiceInstance serviceInstance;
    private final ConcurrentHashMap<String, HealthCheckStream> healthCheckerRegistry;
    private volatile ManagedChannel healthCheckChannel;
    private volatile PersistentLease persistentLease;

    public HealthCheckStream(ServiceInstance serviceInstance,
        ConcurrentHashMap<String, HealthCheckStream> healthCheckerRegistry) {
      this.serviceInstance = serviceInstance;
      this.healthCheckerRegistry = healthCheckerRegistry;
    }

    public void start() {
      this.healthCheckChannel = ManagedChannelBuilder.forAddress(serviceInstance.getIp(), serviceInstance.getPort())
          .usePlaintext().build();
      final HealthGrpc.HealthStub healthStub = HealthGrpc.newStub(healthCheckChannel);
      final HealthCheckRequest checkRequest = HealthCheckRequest.newBuilder()
        .setService(serviceInstance.getDescName())
        .build();

      final Context ctx = Context.current().fork();
      ctx.run(() -> {
        healthStub.watch(checkRequest, this);
      });
    }

    @Override
    public void onNext(HealthCheckResponse healthCheckResponse) {
      final HealthCheckResponse.ServingStatus serviceStatus = healthCheckResponse.getStatus();
      if (serviceStatus != HealthCheckResponse.ServingStatus.SERVING) {
        safetyCloseLease();
        return;
      }

      // 服务探活成功,则将服务信息写入ETCD
      final ListenableFuture<PersistentLease> leaseFuture = registryStorage.save(serviceInstance);
      Futures.addCallback(leaseFuture, new FutureCallback<PersistentLease>() {
        @Override
        public void onSuccess(@Nullable PersistentLease persistentLease) {
          if (persistentLease != null) {
            HealthCheckStream.this.persistentLease = persistentLease;
          }
        }

        @Override
        public void onFailure(@Nonnull Throwable throwable) {
          log.error("Write registry info failure", throwable);
        }
      }, MoreExecutors.directExecutor());
    }

    @Override
    public void onError(Throwable throwable) {
      // 探活发生错误的时候,主动关闭续租,主动关闭探活通道
      safetyCloseHealthChecker();
      log.error("Health check failure, instanceId " + serviceInstance.getInstanceId() + ", Service Name "
          + serviceInstance.getDescName(), throwable);
    }

    @Override
    public void onCompleted() {
      // 探活发生终止的时候,主动关闭续租,主动关闭探活通道
      safetyCloseHealthChecker();
      log.error("Should never happened!");
    }

    private void safetyCloseHealthChecker() {
      safetyCloseLease();
      safetyCloseChannel();
      healthCheckerRegistry.remove(serviceInstance.getInstanceId());
    }

    private void safetyCloseChannel() {
      if (HealthCheckStream.this.healthCheckChannel != null) {
        HealthCheckStream.this.healthCheckChannel.shutdown();
        HealthCheckStream.this.healthCheckChannel = null;
      }
    }

    private void safetyCloseLease() {
      if (persistentLease == null) {
        return;
      }

      try {
        persistentLease.close();
      } catch (IOException e) {
        log.error("Close persistent lease error", e);
      }
      persistentLease = null;
    }
  }
}
