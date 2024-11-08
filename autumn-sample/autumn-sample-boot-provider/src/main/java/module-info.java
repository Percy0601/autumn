/**
 * 
 * @author: baoxin.zhao
 * @date: 2024/11/6 
 */


module autumn.sample.boot.provider {
    requires java.annotation;
    requires org.slf4j;
    requires lombok;
    requires org.apache.thrift;
    requires autumn.core;
    requires autumn.sample.api;
    requires spring.context;

    exports com.microapp.autumn.sample.boot.provider.service;
}