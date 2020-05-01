package com.radishframework.radish.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("radish")
public class RadishSpringBootProperties {

    private RadishSpringBootAgentProperties agent = new RadishSpringBootAgentProperties();
    private RadishSpringBootServerProperties server = new RadishSpringBootServerProperties();
    private RadishJaegerProperties jaeger = new RadishJaegerProperties();
    private RadishDiscoveryProperties discovery = new RadishDiscoveryProperties();

    public RadishDiscoveryProperties getDiscovery() {
        return discovery;
    }

    public void setDiscovery(RadishDiscoveryProperties discovery) {
        this.discovery = discovery;
    }

    public RadishSpringBootAgentProperties getAgent() {
        return agent;
    }

    public void setAgent(RadishSpringBootAgentProperties agent) {
        this.agent = agent;
    }

    public RadishSpringBootServerProperties getServer() {
        return server;
    }

    public void setServer(RadishSpringBootServerProperties server) {
        this.server = server;
    }

    public RadishJaegerProperties getJaeger() {
        return jaeger;
    }

    public void setJaeger(RadishJaegerProperties jaeger) {
        this.jaeger = jaeger;
    }

    public static class RadishSpringBootAgentProperties {

        private String address = "localhost";

        private int port = 8505;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class RadishSpringBootServerProperties {

        private String address;
        private int port = 8506;
        private int operationPort = 18180;
        private boolean disableRegistry = false;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getOperationPort() {
            return operationPort;
        }

        public void setOperationPort(int operationPort) {
            this.operationPort = operationPort;
        }

        public boolean isDisableRegistry() {
            return disableRegistry;
        }

        public void setDisableRegistry(boolean disableRegistry) {
            this.disableRegistry = disableRegistry;
        }
    }

    public static class RadishJaegerProperties {
        private String endpoint = "http://tracing-analysis-dc-sh-internal.aliyuncs.com/adapt_hjgu8naz1e@26554d45e9d3a80_hjgu8naz1e@53df7ad2afe8301";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }

    public static class RadishDiscoveryProperties {
        private String serverAddress;
        private int serverPort = 8510;

        public String getServerAddress() {
            return serverAddress;
        }

        public void setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
        }

        public int getServerPort() {
            return serverPort;
        }

        public void setServerPort(int serverPort) {
            this.serverPort = serverPort;
        }
    }
}
