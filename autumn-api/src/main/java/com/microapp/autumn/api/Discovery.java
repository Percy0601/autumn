package com.microapp.autumn.api;

import java.util.List;

import org.apache.thrift.TServiceClient;

import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.config.ReferenceConfig;


/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public interface Discovery {

    <T extends TServiceClient> void reference(Class<T> classType, ReferenceConfig referenceConfig);

    List<String> services();

    List<ConsumerConfig> getInstances(String name);

    Boolean checkHealth(String ip, Integer port);

}
