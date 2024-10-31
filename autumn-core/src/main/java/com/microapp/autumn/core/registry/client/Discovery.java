package com.microapp.autumn.core.registry.client;

import java.util.List;

import org.apache.thrift.TServiceClient;

import com.microapp.autumn.core.config.ConsumerConfig;
import com.microapp.autumn.core.config.ReferenceConfig;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public interface Discovery {

    <T extends TServiceClient> void reference(Class<T> classType, ReferenceConfig referenceConfig);

    List<String> services();

    List<ConsumerConfig> getInstances(String name);
}
