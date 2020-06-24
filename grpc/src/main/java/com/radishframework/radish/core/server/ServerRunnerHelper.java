package com.radishframework.radish.core.server;

import com.radishframework.radish.core.common.Constant;
import com.radishframework.radish.core.operation.ApplicationOperation;
import com.radishframework.radish.core.operation.OperationConfig;
import com.radishframework.radish.core.operation.OperationServer;
import com.radishframework.radish.core.utils.GrpcReflectionUtils;
import com.radishframework.radish.core.utils.NetUtils;
import io.grpc.BindableService;
import io.grpc.Context;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ProbabilisticSampler;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class ServerRunnerHelper implements ApplicationOperation {

    private Server server;

    protected abstract Logger getLogger();

    protected abstract String getAppName();

    private String hostName;

    private ServerBuilder<?> serverBuilder;

    private int serverPort;

    private int operationPort;

    private InetAddress hostAddress;

    private final HealthStatusManager healthStatusManager = new HealthStatusManager();

    protected final InetAddress getHostAddress() {
        return hostAddress;
    }

    protected final HealthStatusManager getHealthStatusManager() {
        return healthStatusManager;
    }

    protected final int getServerPort() {
        return serverPort;
    }

    protected final String getHostIp() {
        return getHostAddress().getHostAddress();
    }

    protected final int getOperationPort() {
        return operationPort;
    }

    protected final String getHostName() {
        return hostName;
    }

    protected final Server getServer() {
        return server;
    }

    protected final ServerBuilder<?> getServerBuilder() {
        return serverBuilder;
    }

    protected ServerBuilder<?> createServerBuilder(String serverAddress, int srvPort) throws UnknownHostException {
        hostAddress = StringUtils.isEmpty(serverAddress)
                ? NetUtils.getLocalAddress()
                : InetAddress.getByName(serverAddress);
        hostName = hostAddress.getHostName();
        serverPort =
                NetUtils.getAvailablePort(hostAddress, srvPort);

        Constant.CTX_KEY_LOCAL_IP = Context.keyWithDefault(Constant.CTX_KEY_LOCAL_IP.toString(), hostAddress.getHostAddress());

        serverBuilder = ServerBuilder.forPort(serverPort);
        // register health service self to health status manager
        final BindableService healthService = healthStatusManager.getHealthService();
        serverBuilder.addService(healthService);

        // register reflection service
        serverBuilder.addService(ProtoReflectionService.newInstance());

        GrpcReflectionUtils.disableStatsAndTracingModule(serverBuilder);

        return serverBuilder;
    }

    protected void startServer() throws IOException {
        server = getServerBuilder().build().start();

        final Thread awaitThread = new Thread(() -> {
            try {
                getServer().awaitTermination();
                getLogger().info("Radish discovery server stopped.");
            } catch (InterruptedException e) {
                getLogger().error("Radish discovery server stopped.", e);
                Thread.currentThread().interrupt();
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    protected OperationServer createOperationServer(int opPort) throws IOException {
        final String serverAddress = getHostAddress().getHostAddress();
        operationPort = NetUtils.getAvailablePort(hostAddress, opPort);

        final OperationConfig operationConfig = new OperationConfig();
        operationConfig.setAppName(getAppName());
        operationConfig.setHostAddress(serverAddress);
        operationConfig.setOperationPort(operationPort);
        operationConfig.setDaemon(true);

        final OperationServer operationServer = new OperationServer(operationConfig, this);
        getLogger().info("Admin http server started on ip {} port {}", serverAddress, operationPort);
        return operationServer;
    }

    protected final void setupTracingAndExporters(String jaegerEndpoint, String env) {
        // exports jvm hotspot metrics
        DefaultExports.initialize();

        // Register the jaeger trace exporter
        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
                .withType(ProbabilisticSampler.TYPE)
                .withParam(1);

        Configuration.SenderConfiguration senderConfig = Configuration.SenderConfiguration.fromEnv()
                .withEndpoint(jaegerEndpoint + "_" + env + "/api/traces");

        Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv()
                .withSender(senderConfig)
                .withLogSpans(true)
                .withMaxQueueSize(10000);

        Configuration config = new Configuration(getAppName())
                .withSampler(samplerConfig)
                .withReporter(reporterConfig);

        GlobalTracer.registerIfAbsent(config.getTracer());
    }
}
