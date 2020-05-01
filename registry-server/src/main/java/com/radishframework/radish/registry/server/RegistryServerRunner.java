package com.radishframework.radish.registry.server;

import com.ibm.etcd.client.lease.PersistentLease;
import com.radishframework.radish.core.common.InstanceInfo;
import com.radishframework.radish.core.common.ServiceInstance;
import com.radishframework.radish.core.server.ServerRunnerHelper;
import com.radishframework.radish.registry.core.RegistryGrpc;
import com.radishframework.radish.registry.core.RegistryStorage;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import java.io.IOException;

public class RegistryServerRunner extends ServerRunnerHelper implements CommandLineRunner, ApplicationListener<ContextClosedEvent> {
    private final static Logger log = LoggerFactory.getLogger(RegistryServerRunner.class);

    @Autowired
    private RegistryServerProperties serverProperties;
    @Autowired
    private RegistryGrpc.RegistryImplBase registry;
    @Autowired
    private RegistryStorage registryStorage;
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Value("${spring.application.name}")
    private String appName;

    private PersistentLease lease;

    @Override
    public void run(String... args) throws Exception {
        final ServerBuilder<?> serverBuilder = createServerBuilder(
                serverProperties.getServerAddress(), serverProperties.getServerPort());
        serverBuilder.addService(registry);

        super.createOperationServer(serverProperties.getOperationPort());

        startServer();
        log.info("Registry server listen on port " + getServerPort());

        final var instanceInfo = new ServiceInstance(
                InstanceInfo.newBuilder()
                        .setDatacenter(serverProperties.getDatacenter())
                        .setSegment(serverProperties.getSegment())
                        .setDescName(RegistryGrpc.SERVICE_NAME)
                        .setHostname(getHostName())
                        .setIp(getHostIp())
                        .setPort(getServerPort())
                        .setAppName(getAppName())
                        .build());
        lease = registryStorage.save(instanceInfo).get();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected String getAppName() {
        return appName;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (lease != null) {
            try {
                lease.close();
            } catch (IOException e) {
                log.error("close lease error", e);
                lease = null;
            }
        }

        if (getServer() != null) {
            getServer().shutdown();
        }
    }

    @Override
    public void stop() {
        applicationContext.close();
    }

    @Override
    public boolean isRunning() {
        final var server = getServer();
        if (server == null || applicationContext == null) {
            return false;
        }

        return !server.isShutdown() && !server.isTerminated() && applicationContext.isRunning();
    }
}
