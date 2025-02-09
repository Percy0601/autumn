package com.microapp.autumn.core.registry.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.config.ApplicationConfig;
import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.enums.MulticastEventEnum;
import com.microapp.autumn.api.util.ConverterUtil;
import com.microapp.autumn.core.pool.AutumnPool;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/9/30
 */
@Slf4j
@Getter
public class MulticastDiscovery implements Discovery {
    private static volatile MulticastDiscovery instance;
    private AtomicBoolean initStatus = new AtomicBoolean(false);
    private MulticastSocket mc;
    private ConcurrentHashMap<String, ConsumerConfig> instances = new ConcurrentHashMap();
    private Map<String, AtomicInteger> mapping = new ConcurrentHashMap<>();

    public static MulticastDiscovery provider() {
        if(Objects.isNull(instance)) {
            synchronized (MulticastDiscovery.class) {
                if(Objects.isNull(instance)) {
                    instance = new MulticastDiscovery();
                }
            }
        }
        return instance;
    }

    public void discovery() {
        if(Boolean.TRUE.equals(initStatus.get())) {
            return;
        }
        initStatus.compareAndSet(false, true);
        ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
        String ip = applicationConfig.getMulticastIp();
        Integer port = applicationConfig.getMulticastPort();
        try {
            mc = new MulticastSocket(port);
            InetAddress group = InetAddress.getByName(ip);
            log.info("autumn multicast discovery, ip:{}, port:{}", ip, port);
            discovery(mc, group, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private void discovery(MulticastSocket ms, InetAddress group, Integer port) {
        Runnable runnable = () -> {
            handleDiscovery(ms, group, port);
        };

        Thread thread = new Thread(runnable, "autumn-multicast-discovery");
        thread.setDaemon(true);
        thread.start();
        log.info("autumn-multicast-discovery begin listening");
    }

    private void handleDiscovery(MulticastSocket ms, InetAddress group, Integer port) {
        try {
            ms.joinGroup(group);
            byte[] buffer = new byte[8192];
            while (true) {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                ms.receive(dp);
                String ip = dp.getAddress().getHostAddress();
                String data = new String(dp.getData(), 0, dp.getLength());
                int i = data.indexOf('\n');
                if (i > 0) {
                    data = data.substring(0, i).trim();
                }
                Arrays.fill(buffer, (byte) 0);
                Map<String, String> params = ConverterUtil.getUrlParams(data);
                if(ConverterUtil.MULTICAST_REQUEST.equals(params.get(ConverterUtil.CONSTANT_URL_PATH))) {
                    receive(ip, data, MulticastEventEnum.REGISTRY);
                }

                if(ConverterUtil.MULTICAST_SHUTDOWN_REQUEST.equals(params.get(ConverterUtil.CONSTANT_URL_PATH))) {
                    receive(ip, data, MulticastEventEnum.SHUTDOWN);
                }

            }
        } catch (IOException e) {
            log.warn("autumn-multicast-discovery receive exception: ", e);
        } finally {
            if (ms != null) {
                try {
                    ms.leaveGroup(group);
                    ms.close();
                } catch (IOException e) {
                    log.warn("autumn-multicast-discovery receive exception: ", e);
                }
            }
        }
    }

    private void receive(String ip, String data, MulticastEventEnum eventEnum) {
        ConsumerConfig multicastConfig = ConverterUtil.queryStringToProvider(data);
        if(MulticastEventEnum.REGISTRY.equals(eventEnum)) {
            addInstance(multicastConfig);
            return;
        }

        AutumnPool.getInstance().leave(multicastConfig);
        removeInstance(multicastConfig);
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
        instances.forEach((k, v) -> {
            ConsumerConfig instance = v;
            if(System.currentTimeMillis() - instance.getLatestTime() > 10 * 1000) {
                instances.remove(k);
                AutumnPool.getInstance().leave(v);
            }
        });
    }


}
