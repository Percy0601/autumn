/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
module autumn.core {
    requires transitive lombok;
    requires transitive autumn.api;
    exports com.microapp.autumn.core.server;
    exports com.microapp.autumn.core;
}