package com.radishframework.radish.discovery.test;

import org.junit.jupiter.api.Test;

import static com.radishframework.radish.core.common.Constant.ETCD_PATH_SPLITTER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EtcdPathProcessTest {

    @Test
    public void testPathProcess() {
        final String instanceKey = "/radish/instances/com.radishframework.GreeterService/172.10.32.89:8006";
        final String instancesPrefix = "/radish/instances";
        final String descName = instanceKey.substring(instancesPrefix.length() + 1,
                instanceKey.lastIndexOf(ETCD_PATH_SPLITTER));
        assertEquals("com.radishframework.GreeterService", descName);
    }
}
