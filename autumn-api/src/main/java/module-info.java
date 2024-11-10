/**
 * 
 * @author: baoxin.zhao
 * @date: 2024/10/31 
 */
module autumn.api {
    requires java.management;
    requires java.annotation;
    requires lombok;
    requires org.slf4j;
    requires org.apache.thrift;

    exports com.microapp.autumn.api.config;
    exports com.microapp.autumn.api.util;
    exports com.microapp.autumn.api.extension;
    exports com.microapp.autumn.api.enums;
    exports com.microapp.autumn.api.annotation;
    exports com.microapp.autumn.api;
    uses com.microapp.autumn.api.Discovery;
    uses com.microapp.autumn.api.Registry;
}