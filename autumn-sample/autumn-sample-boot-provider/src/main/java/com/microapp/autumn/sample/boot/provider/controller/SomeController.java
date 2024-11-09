package com.microapp.autumn.sample.boot.provider.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/9
 */
@Slf4j
@RestController
@RequestMapping("/boot-sample-provider")
public class SomeController {

    @GetMapping("/hello")
    public String hello() {
        log.info("===================");

        return "OK";
    }

}
