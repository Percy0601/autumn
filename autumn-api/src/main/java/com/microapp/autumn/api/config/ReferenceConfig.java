package com.microapp.autumn.api.config;

import java.util.List;
import java.util.function.Function;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TMultiplexedProtocol;

import com.microapp.autumn.api.enums.RegistryTypeEnum;

import lombok.Data;

/**
 * @author: baoxin.zhao
 * @date: 2024/5/9
 */
@Data
public class ReferenceConfig<R extends TServiceClient> {
    /**
     * refer provider app name
     */
    private String name;
    /**
     * pool timeout: default 1s
     */
    private Long poolTimeout;
    /**
     * connection timeout: default 1s
     */
    private Long connectionTimeout;
    /**
     * socket timeout: default 1s
     */
    private Long socketTimeout;
    /**
     * registryTypeEnum: CONSUL, MULTICAST
     */
    private RegistryTypeEnum registryTypeEnum;
    private List<ConsumerConfig> instances;
    private Class<R> interfaceClass;
    private Function<TMultiplexedProtocol, R> converter;
}
