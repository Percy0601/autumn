package com.microapp.core.config;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(of = {"ip", "port"})
public class ConsumerConfig {
    private String name;
    private String ip;
    private Integer port;
    private String label;
    private AtomicInteger active;
    private Integer connections;
}
