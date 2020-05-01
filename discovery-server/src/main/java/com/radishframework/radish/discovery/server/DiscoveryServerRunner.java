package com.radishframework.radish.discovery.server;

import com.ibm.etcd.client.lease.PersistentLease;
import com.radishframework.radish.core.common.InstanceInfo;
import com.radishframework.radish.core.common.ServiceInstance;
import com.radishframework.radish.core.operation.OperationServer;
import com.radishframework.radish.core.server.ServerRunnerHelper;
import com.radishframework.radish.discovery.core.DiscoveryGrpc;
import com.radishframework.radish.registry.core.RegistryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import javax.annotation.Nonnull;
import java.io.IOException;

public class DiscoveryServerRunner extends ServerRunnerHelper
        implements CommandLineRunner, ApplicationListener<ContextClosedEvent> {

    private final static Logger log = LoggerFactory.getLogger(DiscoveryServerRunner.class);

    @Autowired
    private DiscoveryServerProperties discoveryProperties;
    @Autowired
    private DiscoveryGrpc.DiscoveryImplBase discoveryImpl;
    @Autowired
    private RegistryStorage registryStorage;
    @Value("${spring.profiles.active}")
    private String environment;
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private PersistentLease lease;

    @Value("${spring.application.name}")
    private String appName;
    private OperationServer operationServer;

    @Override
    public void run(String... args) throws Exception {
        final var serverBuilder = super.createServerBuilder(
                discoveryProperties.getServerAddress(), discoveryProperties.getServerPort());
        serverBuilder.addService(discoveryImpl);

        operationServer = super.createOperationServer(discoveryProperties.getOperationPort());

        super.startServer();
        log.info("Discovery server listen on port " + getServerPort());

        // 注册自己到etcd上
        final var instanceInfo = new ServiceInstance(
                InstanceInfo.newBuilder()
                        .setDatacenter(discoveryProperties.getDatacenter())
                        .setSegment(discoveryProperties.getSegment())
                        .setDescName(DiscoveryGrpc.SERVICE_NAME)
                        .setHostname(getHostAddress().getHostName())
                        .setIp(getHostAddress().getHostAddress())
                        .setPort(getServer().getPort())
                        .setAppName(getAppName())
                        .build());
        lease = registryStorage.save(instanceInfo).get();
    }

    @Override
    public void onApplicationEvent(@Nonnull ContextClosedEvent event) {
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

        if (operationServer != null) {
            try {
                operationServer.stop();
            } catch (Exception e) {
                getLogger().error("Stop admin http server error", e);
            }

            operationServer = null;
        }

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
