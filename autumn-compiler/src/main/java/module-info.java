/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
module autumn.compiler {
    requires lombok;
    requires org.slf4j;
    requires org.apache.commons.lang3;
    requires freemarker;
    requires java.annotation;

    requires transitive java.compiler;
    requires transitive autumn.core;
    requires org.apache.commons.collections4;

    exports com.microapp.autumn.compiler;
}