package com.microapp.core.config.provider;

import org.apache.thrift.TProcessor;

public interface AutumnProcessor {

    TProcessor multiplexedProcessor();

}
