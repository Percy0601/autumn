package com.microapp.autumn.core.registry.client;

import java.util.List;
import java.util.Objects;

import org.apache.thrift.TServiceClient;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.core.pool.AutumnPool;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
@Slf4j
public class ConsulDiscovery implements Discovery {
    private static volatile ConsulDiscovery instance;

    public static ConsulDiscovery provider() {
        if(Objects.isNull(instance)) {
            synchronized (ConsulDiscovery.class) {
                if(Objects.isNull(instance)) {
                    instance = new ConsulDiscovery();
                }
            }
        }
        return instance;
    }

    @Override
    public void discovery() {
        log.info("begin discovery reference instances.");
    }

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

    @Override
    public Boolean checkHealth(String ip, Integer port) {
        return null;
    }

}
