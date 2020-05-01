package com.radishframework.radish.discovery.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties
@Component
public class DiscoveryServerProperties {
    private int serverPort = 8510;
    private String serverAddress;
    private String datacenter;
    private String segment;
    private EtcdConfig etcd = new EtcdConfig();
    private int operationPort = 18180;

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public EtcdConfig getEtcd() {
        return etcd;
    }

    public void setEtcd(EtcdConfig etcd) {
        this.etcd = etcd;
    }

    public int getOperationPort() {
        return operationPort;
    }

    public void setOperationPort(int operationPort) {
        this.operationPort = operationPort;
    }

    @ConfigurationProperties
    public static class EtcdConfig {
        private List<String> endpoints;
        private String username;
        private String password;

        public List<String> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(List<String> endpoints) {
            this.endpoints = endpoints;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
