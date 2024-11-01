package com.microapp.autumn.sample.test;

import java.io.IOException;

import org.apache.thrift.TProcessor;
import org.junit.jupiter.api.Test;


import com.microapp.autumn.api.config.ServiceConfig;
import com.microapp.autumn.api.extension.AttachableProcessor;

import com.microapp.autumn.core.AutumnBootstrap;
import com.microapp.autumn.sample.api.SomeService;
import com.microapp.autumn.sample.service.SomeServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/8
 */
@Slf4j
public class TrainingTest {

    @Test
    void testProvider() {
        log.info("========================");
        ServiceConfig<SomeService.Iface> fooServiceConfig = new ServiceConfig<>();
        fooServiceConfig.setInterfaceClass(SomeService.Iface.class);
        TProcessor tprocessor = new SomeService.Processor<SomeService.Iface>(new SomeServiceImpl());
        AttachableProcessor attachableProcessor = new AttachableProcessor(tprocessor);
        fooServiceConfig.setRef(attachableProcessor);
        AutumnBootstrap.getService().serve();

        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
