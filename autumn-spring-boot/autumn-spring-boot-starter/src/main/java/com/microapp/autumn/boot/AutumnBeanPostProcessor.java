package com.microapp.autumn.boot;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/9
 */
@Slf4j
public class AutumnBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(beanName.endsWith("$AutumnProviderService")) {
            log.info("autumn bean post process handle provider service, name:{}", beanName);
        }

        if(beanName.endsWith("$AutumnConsumerClient")) {
            log.info("autumn bean post process handle consumer client, name:{}", beanName);
        }

        return bean;
    }
}
