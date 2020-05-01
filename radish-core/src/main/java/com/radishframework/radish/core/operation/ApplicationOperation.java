package com.radishframework.radish.core.operation;

/**
 * 应用运维管理操作
 */
public interface ApplicationOperation {

    void stop();

    boolean isRunning();
}
