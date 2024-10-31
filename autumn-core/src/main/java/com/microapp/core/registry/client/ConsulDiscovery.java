package com.microapp.core.registry.client;

import java.util.List;
import java.util.Objects;

import org.apache.thrift.TServiceClient;

import com.microapp.core.config.ConsumerConfig;
import com.microapp.core.config.ReferenceConfig;
import com.microapp.core.pool.AutumnPool;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public class ConsulDiscovery implements Discovery{
    @Override
    public <T extends TServiceClient> void reference(Class<T> classType, ReferenceConfig referenceConfig) {

    }

    @Override
    public List<String> services() {
        return List.of();
    }

    @Override
    public List<ConsumerConfig> getInstances(String name) {
        ReferenceConfig<? extends TServiceClient> config = AutumnPool.getInstance().getReferenceConfig(name);
        if(Objects.isNull(config)) {
            return null;
        }
        return config.getInstances();
    }

}
