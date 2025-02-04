package com.microapp.autumn.api.config;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ConsumerConfig {
    /**
     * reference service name
     */
    private String name;
    /**
     * consumer ip
     */
    private String ip;
    /**
     * consumer port
     */
    private Integer port;
    /**
     * consumer label, with label route the consumer instance.
     */
    private String label;
    /**
     * current active connection
     */
    private AtomicInteger active;
    /**
     * max connection
     */
    private Integer connections;
    /**
     * check health fail count
     */
    private Integer checkFail;
}
