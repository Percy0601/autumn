/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
module autumn.core {
    requires lombok;
    requires transitive autumn.api;

    exports com.microapp.autumn.core.server;
    exports com.microapp.autumn.core;
    exports com.microapp.autumn.core.pool;

    provides com.microapp.autumn.api.Registry with
            com.microapp.autumn.core.registry.MulticastRegistry;
    provides com.microapp.autumn.api.Discovery with
            com.microapp.autumn.core.registry.client.MulticastDiscovery;

}