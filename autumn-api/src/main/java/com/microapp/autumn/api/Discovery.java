package com.microapp.autumn.api;

import java.util.List;
import java.util.Set;

import org.apache.thrift.TServiceClient;

import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.config.ReferenceConfig;


/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public interface Discovery {

    void discovery();

    Set<String> services();

    List<ConsumerConfig> getInstances(String name);

}
