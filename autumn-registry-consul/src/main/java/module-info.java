/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
module autumn.registry.consul {
    requires lombok;
    requires autumn.api;
    requires autumn.core;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.apache.httpcomponents.client5.httpclient5;

    exports com.microapp.autumn.core.registry;

    provides com.microapp.autumn.api.Registry with
            com.microapp.autumn.core.registry.ConsulRegistry;
    provides com.microapp.autumn.api.Discovery with
            com.microapp.autumn.core.registry.client.ConsulDiscovery;

}