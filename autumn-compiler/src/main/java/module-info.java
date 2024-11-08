/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
module autumn.compiler {

    requires lombok;
    requires org.slf4j;
    requires org.apache.commons.lang3;
    requires freemarker;
    requires transitive java.compiler;
    requires transitive autumn.core;

    exports com.microapp.autumn.compiler;
}