package com.microapp.autumn.api.config;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(of = {"ip", "port"})
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
     * check health latest active date
     */
    private Date latestActive;
    /**
     * check health fail count
     */
    private Integer checkFail;
}
