package com.radishframework.grpc.server.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("radish")
public class RadishProperties {

  private RadishServerProperties server = new RadishServerProperties();

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
