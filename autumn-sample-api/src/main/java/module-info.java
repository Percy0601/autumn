/**
 * 
 * @author: baoxin.zhao
 * @date: 2024/11/6 
 */


module autumn.sample.api {
    requires java.annotation;
    requires org.slf4j;
    requires lombok;
    requires org.apache.thrift;

    exports com.microapp.autumn.sample.api;
}