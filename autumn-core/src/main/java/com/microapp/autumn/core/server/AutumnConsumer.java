package com.microapp.autumn.core.server;

import org.apache.thrift.TServiceClient;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.api.util.SpiUtil;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
public class AutumnConsumer {
    private volatile static AutumnConsumer singleton = null;

    private AutumnConsumer() {

    }

    public static AutumnConsumer provider() {
        if (singleton == null) {
            synchronized (AutumnProvider.class) {
                if (singleton == null) {
                    singleton = new AutumnConsumer();
                    return singleton;
                }
            }
        }
        return singleton;
    }

    public <T extends TServiceClient> AutumnConsumer reference(ReferenceConfig<T> referenceConfig) {
        Discovery discovery = SpiUtil.discovery();
        discovery.reference(referenceConfig.getInterfaceClass(), referenceConfig);
        return this;
    }

}
