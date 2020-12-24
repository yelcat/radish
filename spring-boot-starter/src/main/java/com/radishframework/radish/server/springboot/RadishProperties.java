package com.radishframework.radish.server.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("radish")
public class RadishProperties {

  private RadishServerProperties server = new RadishServerProperties();
  private RadishEtcdProperties etcd = new RadishEtcdProperties();
  private RadishJaegerProperties jaeger = new RadishJaegerProperties();

  public RadishJaegerProperties getJaeger() {
    return jaeger;
  }

  public RadishEtcdProperties getEtcd() {
    return etcd;
  }

  public void setEtcd(RadishEtcdProperties etcd) {
    this.etcd = etcd;
  }

  public void setJaeger(RadishJaegerProperties jaeger) {
    this.jaeger = jaeger;
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

  public static class RadishEtcdProperties {
    private String endpoints;
    private String username;
    private String password;

    public String getEndpoints() {
      return endpoints;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public void setEndpoints(String endpoints) {
      this.endpoints = endpoints;
    }

  }

  public static class RadishServerProperties {
    private String address;
    private int port = 8086;
    private int operationPort = 18180;
    private boolean disableRegistry;

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

  public RadishServerProperties getServer() {
    return server;
  }

  public void setServer(RadishServerProperties server) {
    this.server = server;
  }
}
