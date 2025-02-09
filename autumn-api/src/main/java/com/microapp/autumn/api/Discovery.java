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

    /**
     * begin discovery watch service
     */
    void discovery();

    /**
     * add watch service
     *
     * @param name
     */
    void watch(String name);

    /**
     * get all watch service list
     * @return
     */
    Set<String> services();


    List<ConsumerConfig> getInstances(String name);

}
