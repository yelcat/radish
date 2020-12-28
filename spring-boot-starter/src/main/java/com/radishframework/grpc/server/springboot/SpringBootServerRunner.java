package com.radishframework.grpc.server.springboot;

import com.radishframework.grpc.client.KubernetesNameResolverProvider;
import com.radishframework.grpc.server.OpenTelemetryServerInterceptor;
import com.radishframework.grpc.server.springboot.RadishProperties.RadishServerProperties;
import com.radishframework.grpc.server.annotations.GrpcService;
import io.grpc.*;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.KotlinReflectionParameterNameDiscoverer;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;

/**
 * spring boot radish runner
 * <p>
 * 启动gRPC服务器端和管理http服务器
 */
public class SpringBootServerRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootServerRunner.class);

    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @Autowired
    private RadishProperties radishProperties;
    @Value("${spring.application.name}")
    private String applicationName;
    private HealthStatusManager healthStatusManager = new HealthStatusManager();

    @Override
    public void run(String... args) throws Exception {
        final RadishServerProperties serverProperties = radishProperties.getServer();
        final ServerBuilder<?> serverBuilder =
                ServerBuilder.forPort(serverProperties.getPort());

        addServices(serverBuilder, healthStatusManager, new OpenTelemetryServerInterceptor(applicationName));
        serverBuilder.addService(ProtoReflectionService.newInstance());
        serverBuilder.addService(healthStatusManager.getHealthService());

        final Server server = serverBuilder.build();
        server.start();
        log.info("Grpc server started at port " + serverProperties.getPort());

        // await for application termination
        server.awaitTermination();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
    }

    private void addServices(ServerBuilder<?> serverBuilder, HealthStatusManager healthStatusManager, ServerInterceptor... globalInterceptors) {
        Stream.of(applicationContext.getBeanNamesForType(BindableService.class)).filter(name -> {
            final BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
            final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(GrpcService.class);

            if (beansWithAnnotation.containsKey(name)) {
                return true;
            } else if (beanDefinition.getSource() instanceof AnnotatedTypeMetadata) {
                return ((AnnotatedTypeMetadata) beanDefinition.getSource()).isAnnotated(GrpcService.class.getName());
            }

            return false;
        }).forEach(name -> {
            BindableService srv = applicationContext.getBeanFactory().getBean(name, BindableService.class);
            ServerServiceDefinition serviceDefinition = srv.bindService();
            GrpcService serviceAnn = applicationContext.findAnnotationOnBean(name, GrpcService.class);
            if (serviceAnn == null) {
                return;
            }

            // add global interceptors, AKA: context tracing
            for (ServerInterceptor serverInterceptor : globalInterceptors) {
                serviceDefinition = ServerInterceptors.intercept(serviceDefinition, serverInterceptor);
            }
            serverBuilder.addService(serviceDefinition);
            healthStatusManager.setStatus(
                    serviceDefinition.getServiceDescriptor().getName(),
                    HealthCheckResponse.ServingStatus.SERVING);
        });
    }
}
