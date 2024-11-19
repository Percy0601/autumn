package com.microapp.autumn.sample.boot.provider.controller;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.microapp.autumn.sample.boot.provider.service.BootProviderServiceImpl$Autumn;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/9
 */
@Slf4j
@RestController
@RequestMapping("/boot-sample-provider")
public class SomeController {
    @Autowired
    BootProviderServiceImpl$Autumn bootProviderServiceImpl$Autumn;

    @GetMapping("/hello")
    public String hello() {
        log.info("===================");
        try {
            String result = bootProviderServiceImpl$Autumn.echo("213");
            return result;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

}
