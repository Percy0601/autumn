package com.microapp.autumn.core.registry.consul.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/5
 */
@Data
public class ConsulServiceWeights {
    @JsonProperty("Passing")
    private String passing;
    @JsonProperty("Warning")
    private String warning;
}
