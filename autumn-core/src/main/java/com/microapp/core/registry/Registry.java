package com.microapp.core.registry;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public interface Registry {
    Boolean register();
    void shutdownHook();
}
