/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
module autumn.sample {
    requires org.slf4j;
    requires lombok;
    requires transitive autumn.core;
    exports com.microapp.autumn.sample.api;
    exports com.microapp.autumn.sample.service;
}