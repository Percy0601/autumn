/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
module autumn.registry.consul {
    requires lombok;
    requires transitive autumn.api;
    exports com.microapp.autumn.core.registry;

    provides com.microapp.autumn.api.Registry with
            com.microapp.autumn.core.registry.ConsulRegistry;
    provides com.microapp.autumn.api.Discovery with
            com.microapp.autumn.core.registry.client.ConsulDiscovery;

}