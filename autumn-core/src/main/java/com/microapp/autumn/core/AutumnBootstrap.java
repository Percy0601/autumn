package com.microapp.autumn.core;

import java.util.Objects;

import com.microapp.autumn.api.config.ApplicationConfig;
import com.microapp.autumn.api.util.SpiUtil;
import com.microapp.autumn.core.server.AutumnProvider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/5/8
 */
@Slf4j
@Setter
@Getter
public class AutumnBootstrap {
    private static volatile AutumnBootstrap instance;
    private ApplicationConfig applicationConfig;
    private AutumnBootstrap() {}

    public static AutumnBootstrap getInstance() {
        if(Objects.isNull(instance)) {
            synchronized (AutumnBootstrap.class) {
                if(Objects.isNull(instance)) {
                    instance = new AutumnBootstrap();
                }
            }
        }
        return instance;
    }

    public void serve() {
        AutumnProvider provider = AutumnProvider.provider();
        provider.start();
        log.info("autumn provider finish");

        SpiUtil.registry().register();
        log.info("autumn registry finish");

        SpiUtil.discovery().discovery();
        shutdownHook();
    }

    private void shutdownHook() {

        Runnable shutdownHook = () -> {
            SpiUtil.registry().shutdownHook();
        };
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
    }



}
