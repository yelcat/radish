package com.radishframework.radish.core.operation;

public class OperationConfig {
    private String appName;
    private String hostAddress;
    private int operationPort;
    private boolean daemon;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public int getOperationPort() {
        return operationPort;
    }

    public void setOperationPort(int operationPort) {
        this.operationPort = operationPort;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }
}
