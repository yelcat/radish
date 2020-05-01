package com.radishframework.radish.springboot.server;

import com.radishframework.radish.core.common.Constant;
import com.radishframework.radish.core.operation.OperationServer;
import com.radishframework.radish.core.server.RadishContextServerInterceptor;
import com.radishframework.radish.core.server.ServerRunnerHelper;
import com.radishframework.radish.registry.core.InstanceInfo;
import com.radishframework.radish.registry.core.RegisterRequest;
import com.radishframework.radish.registry.core.RegisterResponse;
import com.radishframework.radish.registry.core.RegistryGrpc;
import com.radishframework.radish.springboot.RadishSpringBootProperties;
import com.radishframework.radish.springboot.RadishSpringBootProperties.RadishJaegerProperties;
import com.radishframework.radish.springboot.RadishSpringBootProperties.RadishSpringBootServerProperties;
import com.radishframework.radish.springboot.annotations.GrpcService;
import io.grpc.*;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.LogExceptionRunnable;
import io.grpc.internal.SharedResourceHolder;
import io.opentracing.contrib.grpc.TracingServerInterceptor;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;

/**
 * spring boot radish runner
 * <p>
 * 启动gRPC服务器端和管理http服务器
 */
public class SpringBootServerRunner extends ServerRunnerHelper implements CommandLineRunner,
        ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(SpringBootServerRunner.class);
    private static final String SDK_VERSION = "0.5.11";

    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @Autowired
    private RadishSpringBootProperties radishProperties;
    @Autowired
    private RegistryGrpc.RegistryBlockingStub registryStub;
    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${spring.profiles.active}")
    private String environment;

    private InstanceInfo instanceInfo;
    private ScheduledExecutorService scheduledExecutor;
    private Executor executor;
    private ScheduledFuture<?> serviceRegisterTask;
    private OperationServer operationServer;
    private volatile boolean running = true;
    private final List<ServerServiceDefinition> scannedServices = newArrayList();

    @Override
    public void run(String... args) throws Exception {
        final RadishSpringBootServerProperties serverProperties = radishProperties.getServer();
        final ServerBuilder<?> serverBuilder = createServerBuilder(
                serverProperties.getAddress(), radishProperties.getServer().getPort());

        operationServer = createOperationServer(radishProperties.getServer().getOperationPort());

        final RadishJaegerProperties jaegerAgentProperties = radishProperties.getJaeger();
        super.setupTracingAndExporters(jaegerAgentProperties.getEndpoint(), environment);

        final List<ServerInterceptor> globalInterceptors = newArrayList();
        globalInterceptors.add(new RadishContextServerInterceptor());
        globalInterceptors.add(TracingServerInterceptor.newBuilder()
                .withTracer(GlobalTracer.get())
                .withVerbosity()
                .withStreaming()
                .withTracedAttributes(TracingServerInterceptor.ServerRequestAttribute.PEER_ADDRESS)
                .build());
        addServices(serverBuilder, globalInterceptors);

        scheduledExecutor = SharedResourceHolder.get(GrpcUtil.TIMER_SERVICE);
        executor = SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);

        startServer();

        instanceInfo = InstanceInfo.newBuilder()
                .setIp(getHostIp())
                .setPort(getServerPort())
                .setOperationPort(getOperationPort())
                .setHostname(getHostName())
                .setAppName(getAppName())
                .build();

        if (!serverProperties.isDisableRegistry()) {
            startServiceRegister(0);
        }
        getLogger().info("gRPC Server started, listening on ip {} port {}.", getHostIp(), getServerPort());
    }


    @Override
    public void stop() {
        applicationContext.close();
    }

    @Override
    public boolean isRunning() {
        final Server server = getServer();
        if (server == null || applicationContext == null || operationServer == null) {
            return false;
        }

        return !server.isShutdown() && !server.isTerminated() && applicationContext.isRunning();
    }

    @Override
    public void onApplicationEvent(@Nonnull ContextClosedEvent event) {
        if (!running) {
            return;
        }
        running = false;

        unregisterServices();

        final Server server = getServer();
        if (server != null) {
            server.shutdown();
        }

        if (scheduledExecutor != null) {
            scheduledExecutor = SharedResourceHolder
                    .release(GrpcUtil.TIMER_SERVICE, scheduledExecutor);
        }

        if (executor != null) {
            executor = SharedResourceHolder.release(GrpcUtil.SHARED_CHANNEL_EXECUTOR, executor);
        }

        if (operationServer != null) {
            try {
                operationServer.stop();
            } catch (Exception e) {
                getLogger().error("Stop admin http server error", e);
                operationServer = null;
            }
        }

        getLogger().info("Radish gRPC server stopped");
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected String getAppName() {
        return applicationName;
    }

    private void addServices(ServerBuilder<?> serverBuilder, List<ServerInterceptor> globalInterceptors) {
        final Class<? extends Annotation> annotationType = GrpcService.class;
        Stream.of(applicationContext.getBeanNamesForType(BindableService.class))
                .filter(name -> {
                    final BeanDefinition beanDefinition = applicationContext.getBeanFactory()
                            .getBeanDefinition(name);
                    final Map<String, Object> beansWithAnnotation = applicationContext
                            .getBeansWithAnnotation(annotationType);

                    if (beansWithAnnotation.containsKey(name)) {
                        return true;
                    } else if (beanDefinition.getSource() instanceof AnnotatedTypeMetadata) {
                        return ((AnnotatedTypeMetadata) beanDefinition.getSource())
                                .isAnnotated(annotationType.getName());

                    }

                    return false;
                })
                .forEach(name -> {
                    BindableService srv = applicationContext.getBeanFactory()
                            .getBean(name, BindableService.class);
                    ServerServiceDefinition serviceDefinition = srv.bindService();
                    GrpcService serviceAnn = applicationContext
                            .findAnnotationOnBean(name, GrpcService.class);
                    if (serviceAnn == null) {
                        return;
                    }

                    // add global interceptors, AKA: context tracing
                    for (ServerInterceptor serverInterceptor : globalInterceptors) {
                        serviceDefinition = ServerInterceptors.intercept(
                                serviceDefinition, serverInterceptor);
                    }
                    serverBuilder.addService(serviceDefinition);
                    scannedServices.add(serviceDefinition);
                });
    }

    private void unregisterServices() {
        final Server server = getServer();
        if (server == null) {
            return;
        }

        scannedServices.forEach(
                serviceDefinition ->
                        getHealthStatusManager().clearStatus(serviceDefinition.getServiceDescriptor().getName()));
    }

    private void startServiceRegister(int interval) {
        if (serviceRegisterTask != null && !serviceRegisterTask.isCancelled()
                && !serviceRegisterTask.isDone()) {
            serviceRegisterTask.cancel(true);
        }

        serviceRegisterTask = scheduledExecutor.schedule(
                () -> executor.execute(new LogExceptionRunnable(new ServiceRegisterJob())),
                interval, TimeUnit.SECONDS);
    }

    private class ServiceRegisterJob implements Runnable {

        @Override
        public void run() {
            try {
                scannedServices.forEach(serviceDefinition ->
                        registerService(serviceDefinition.getServiceDescriptor()));
                startServiceRegister(Constant.SERVICE_AUTO_REGISTRY_INTERVAL);
            } catch (Exception ex) {
                startServiceRegister(Constant.SERVICE_RETRY_REGISTRY_INTERVAL);

                getLogger().error("Service register error", ex);
            }
        }

        private void registerService(@Nonnull final ServiceDescriptor serviceDescriptor) {
            final String serviceName = serviceDescriptor.getName();
            // 告知当前服务处于服务中
            getHealthStatusManager().setStatus(serviceName, HealthCheckResponse.ServingStatus.SERVING);

            final RegisterRequest registerRequest =
                    RegisterRequest.newBuilder()
                            .setDescName(serviceName)
                            .setInstanceInfo(instanceInfo)
                            .build();
            final RegisterResponse registerResponse = registryStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .register(registerRequest);
            if (registerResponse != null && registerResponse.getSuccess()) {
                getLogger().info("Service {} instance {} is registered",
                        serviceName, instanceInfo.getIp() + ":" + instanceInfo.getPort());
            }
        }
    }

}
