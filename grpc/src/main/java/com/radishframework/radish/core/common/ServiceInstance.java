package com.radishframework.radish.core.common;

import com.google.protobuf.ByteString;

import java.util.Objects;

public class ServiceInstance {
    private final InstanceInfo instanceInfo;

    public ServiceInstance(InstanceInfo instanceInfo) {
        this.instanceInfo = instanceInfo;
    }

    public String getInstanceId() {
        return  getIp() + ":" + getPort();
    }

    public String getIp() {
        return instanceInfo.getIp();
    }

    public int getPort() {
        return instanceInfo.getPort();
    }

    public String getDescName() {
        return instanceInfo.getDescName();
    }

    public int getOperationPort() {
        return instanceInfo.getOperationPort();
    }

    public String getHostname() {
        return instanceInfo.getHostname();
    }

    public ByteString toByteString() {
        return instanceInfo.toByteString();
    }

    public String getAppName() {
        return instanceInfo.getAppName();
    }

    public String getDatacenter() {
        return instanceInfo.getDatacenter();
    }

    public String getSegment() {
        return instanceInfo.getSegment();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return Objects.equals(instanceInfo, that.instanceInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceInfo);
    }
}
