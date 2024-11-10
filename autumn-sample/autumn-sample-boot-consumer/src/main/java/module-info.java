/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
module autumn.sample {
    requires java.annotation;
    requires org.slf4j;
    requires lombok;
    requires transitive autumn.core;
    exports com.microapp.autumn.sample.api;
}