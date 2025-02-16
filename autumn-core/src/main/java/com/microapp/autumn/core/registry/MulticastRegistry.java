package com.microapp.autumn.core.registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.Registry;
import com.microapp.autumn.api.config.ApplicationConfig;
import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.config.ProviderConfig;
import com.microapp.autumn.api.enums.MulticastEventEnum;
import com.microapp.autumn.api.util.CommonUtil;
import com.microapp.autumn.api.util.ConverterUtil;
import com.microapp.autumn.api.util.SpiUtil;
import com.microapp.autumn.core.pool.AutumnPool;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
@Slf4j
public class MulticastRegistry implements Registry, Discovery {
    private static volatile MulticastRegistry instance;
    private volatile AtomicInteger count = new AtomicInteger(0);
    private volatile MulticastSocket mc;
    private volatile InetAddress group;
    private ConcurrentHashMap<String, ConsumerConfig> instances = new ConcurrentHashMap();
    private Map<String, AtomicInteger> mapping = new ConcurrentHashMap<>();

    public static MulticastRegistry provider() {
        if(Objects.isNull(instance)) {
            synchronized (MulticastRegistry.class) {
                if(Objects.isNull(instance)) {
                    instance = new MulticastRegistry();
                }
            }
        }
        return instance;
    }

    @Override
    public void discovery() {
        ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
        String ip = applicationConfig.getMulticastIp();
        Integer port = applicationConfig.getMulticastPort();
        try {
            if(Objects.isNull(mc)) {
                mc = new MulticastSocket(port);
                group = InetAddress.getByName(ip);
                mc.joinGroup(group);
            }
            Runnable runnable = () -> {
                handleDiscovery(mc);
            };

            Thread thread = new Thread(runnable, "autumn-multicast-discovery");
            thread.setDaemon(true);
            thread.start();
            log.info("autumn-multicast-discovery begin listening, ip:{}, port:{}", ip, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDiscovery(MulticastSocket ms) {
        try {
            byte[] buffer = new byte[8192];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            while (!ms.isClosed()) {
                ms.receive(dp);
                //String ip = dp.getAddress().getHostAddress();
                String data = new String(dp.getData(), 0, dp.getLength());
                int i = data.indexOf('\n');
                if (i > 0) {
                    data = data.substring(0, i).trim();
                }
                Arrays.fill(buffer, (byte) 0);
                Map<String, String> params = ConverterUtil.getUrlParams(data);
                String protocol = params.get(ConverterUtil.CONSTANT_PROTOCOL);
                receive(data, protocol);
            }
        } catch (Throwable e) {
            log.error("multicast discovery socket is closed. exception:{}", e.getMessage());
        }
    }

    private void receive(String data, String protocol) {
        ConsumerConfig multicastConfig = ConverterUtil.queryStringToProvider(data);
//        ProviderConfig config = ProviderConfig.getInstance();
//        if(Objects.nonNull(config)) {
//            if(!multicastConfig.getReferences().contains(config.getName())) {
//                return;
//            }
//        }

        if(MulticastEventEnum.REGISTRY.getCode().equals(protocol)) {
            addInstance(multicastConfig);
            SpiUtil.registry().register();
            return;
        }
        if (MulticastEventEnum.SUBSCRIBE.getCode().equals(protocol)){
            addInstance(multicastConfig);
            return;
        }
        AutumnPool.getInstance().leave(multicastConfig);
        removeInstance(multicastConfig);
    }

    @Override
    public void watch(String name) {
        if(mapping.containsKey(name)) {
            return;
        }

        mapping.put(name, new AtomicInteger(0));
    }

    private void addInstance(ConsumerConfig consumerConfig) {
        if(!mapping.containsKey(consumerConfig.getName())) {
            return;
        }

        String hash = consumerConfig.getName().concat(":")
                .concat(consumerConfig.getIp())
                .concat(":")
                .concat(consumerConfig.getPort().toString());
        if(instances.contains(hash)) {
            ConsumerConfig instance = instances.get(hash);
            Integer version = instance.getVersion();
            instance.setVersion(version);
            instance.setLatestTime(System.currentTimeMillis());
            return;
        }
        log.info("multicast discovery receive data, config:{}", consumerConfig);
        consumerConfig.setLatestTime(System.currentTimeMillis());
        consumerConfig.setVersion(1);
        instances.put(hash, consumerConfig);
    }

    private void removeInstance(ConsumerConfig consumerConfig) {
        instances.remove(consumerConfig);
    }


    @Override
    public Set<String> services() {
        Set<String> services = instances.values().stream()
                .map(ConsumerConfig::getName)
                .collect(Collectors.toSet());
        return services;
    }

    @Override
    public List<ConsumerConfig> getInstances(String name) {
        Map<String, List<ConsumerConfig>> result = instances.values().stream()
                .collect(Collectors.groupingBy(ConsumerConfig::getName));
        return result.get(name);
    }

    @Override
    public void checkHealth() {
//        instances.forEach((k, v) -> {
//            ConsumerConfig instance = v;
//            if(System.currentTimeMillis() - instance.getLatestTime() > 10 * 1000) {
//                instances.remove(k);
//                AutumnPool.getInstance().leave(v);
//            }
//        });
    }


    @Override
    public Boolean register() {
        if(count.get() > 3) {
            return true;
        }
        return init();
    }

    @Override
    public void shutdownHook() {
        log.info("autumn shutdown hook");
        ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
        String ip = applicationConfig.getMulticastIp();
        Integer port = applicationConfig.getMulticastPort();
        ProviderConfig config = ProviderConfig.getInstance();
        String registryRequest = ConverterUtil.shutdownRequest(config);

        try {
            group = InetAddress.getByName(ip);
            byte[] buffer = registryRequest.getBytes();
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, group, port);
            mc.send(dp);
            Arrays.fill(buffer, (byte) 0);
        } catch (Exception e) {
            log.warn("autumn-multicast-registry shutdown exception: ", e);
        } finally {
            if (mc != null) {
                try {
                    mc.leaveGroup(group);
                    mc.close();
                } catch (IOException e) {
                    log.warn("autumn-multicast-registry shutdown exception: ", e);
                }
            }
        }
    }

    private Boolean init() {
        ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
        String ip = applicationConfig.getMulticastIp();
        Integer port = applicationConfig.getMulticastPort();

        ProviderConfig config = ProviderConfig.getInstance();
        Properties properties = CommonUtil.readClasspath("application.properties");
        config.init(properties);
        log.info("autumn-multicast registry ip:{}, port:{}, config:{}", ip, port, config);
        registry(port, config);
        return true;
    }

    private void registry(Integer port, ProviderConfig config) {
        count.incrementAndGet();
        String registryRequest = ConverterUtil.registryRequest(config);
        if(count.get() > 3) {
            registryRequest = ConverterUtil.subscribeRequest(config);
        }
        try {
            if(Objects.isNull(mc)) {
                ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
                String ip = applicationConfig.getMulticastIp();
                mc = new MulticastSocket(port);
                group = InetAddress.getByName(ip);
                mc.joinGroup(group);
            }
            byte[] buffer = registryRequest.getBytes();
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, group, port);
            mc.send(dp);
            Arrays.fill(buffer, (byte) 0);
        } catch (Exception e) {
            log.warn("autumn-multicast-registry receive exception: ", e);
        }
    }
}
