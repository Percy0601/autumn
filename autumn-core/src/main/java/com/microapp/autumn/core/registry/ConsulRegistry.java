package com.microapp.autumn.core.registry;

import java.util.Objects;

import com.microapp.autumn.api.Registry;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public class ConsulRegistry implements Registry {
    private static volatile ConsulRegistry instance;
    private ConsulRegistry() {

    }

    public static ConsulRegistry getInstance() {
        if(Objects.isNull(instance)) {
            synchronized (ConsulRegistry.class) {
                if(Objects.isNull(instance)) {
                    instance = new ConsulRegistry();
                }
            }
        }
        return instance;
    }

    @Override
    public Boolean register() {
        return null;
    }

    @Override
    public void shutdownHook() {

    }
}
