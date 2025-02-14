package com.microapp.autumn.core.pool;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TServiceClient;

import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.core.pool.impl.ConcurrentBag;
import com.microapp.autumn.core.pool.impl.ConcurrentBagEntry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AutumnPool {
    private volatile static AutumnPool singleton = null;
    private ConcurrentHashMap<String, ConcurrentBag> mapping;

    private AutumnPool() {

    }

    private void init() {
        mapping = new ConcurrentHashMap<>();
    }

    public static AutumnPool getInstance() {
        if (singleton == null) {
            synchronized (AutumnPool.class) {
                if (singleton == null) {
                    singleton = new AutumnPool();
                    singleton.init();
                    return singleton;
                }
            }
        }
        return singleton;
    }

    public AutumnPool referenceConfig(ReferenceConfig<? extends TServiceClient> config) {
        if(mapping.contains(config.getName())) {
            return this;
        }
        ConcurrentBag bag = new ConcurrentBag(config);
        mapping.put(config.getName(), bag);
        return this;
    }

    public ReferenceConfig<? extends TServiceClient> getReferenceConfig(String name) {
        ConcurrentBag bag = mapping.get(name);
        ReferenceConfig<? extends TServiceClient> config = bag.getConfig();
        return config;
    }

    public ConcurrentBagEntry borrow(String service) {
        ConcurrentBag bag = mapping.get(service);
        try {
            ConcurrentBagEntry entry = bag.borrow(1000, TimeUnit.MILLISECONDS);
            return entry;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void requite(ConcurrentBagEntry entry) {
        if(Objects.isNull(entry)) {
            return;
        }
        ConcurrentBag bag = mapping.get(entry.getService());
        bag.requite(entry);
    }

    public void remove(ConcurrentBagEntry entry) {
        ConcurrentBag bag = mapping.get(entry.getService());
        bag.remove(entry);
    }

    public void join(ConsumerConfig consumerConfig) {
        ConcurrentBag bag = mapping.get(consumerConfig.getName());
        bag.join(consumerConfig);
    }

    public void leave(ConsumerConfig consumerConfig) {
        ConcurrentBag bag = mapping.get(consumerConfig.getName());
        if(Objects.isNull(bag)) {
            return;
        }
        bag.remove(consumerConfig.getIp());
    }

}
