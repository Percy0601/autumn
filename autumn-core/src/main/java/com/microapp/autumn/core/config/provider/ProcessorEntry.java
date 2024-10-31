package com.microapp.autumn.core.config.provider;

import lombok.Data;
import org.apache.thrift.TProcessor;

@Data
public class ProcessorEntry {
    private TProcessor processor;
    private String serviceName;
}
