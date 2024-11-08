package com.microapp.autumn.core.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/5
 */
@Data
public class ConsulServiceCheck {
    @JsonProperty("DeregisterCriticalServiceAfter")
    private String deregisterCriticalServiceAfter;
    @JsonProperty("TCP")
    private String tcp;
    @JsonProperty("Interval")
    private String interval;
    @JsonProperty("Timeout")
    private String timeout;

}
