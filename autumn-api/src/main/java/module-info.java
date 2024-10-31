/**
 * 
 * @author: baoxin.zhao
 * @date: 2024/10/31 
 */
module autumn.api {
    requires java.management;
    requires java.annotation;
    requires lombok;
    requires transitive org.slf4j;
    requires transitive org.apache.thrift;
    requires transitive org.apache.httpcomponents.core5.httpcore5;
    requires transitive org.apache.httpcomponents.client5.httpclient5;

    exports com.microapp.autumn.api.config;
    exports com.microapp.autumn.api.util;
    exports com.microapp.autumn.api.extension;
    exports com.microapp.autumn.api.enums;
    exports com.microapp.autumn.api.annotation;
    exports com.microapp.autumn.api;
}