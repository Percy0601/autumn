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
import java.util.stream.Collectors;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.Registry;
import com.microapp.autumn.api.config.ApplicationConfig;
import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.enums.MulticastEventEnum;
import com.microapp.autumn.api.util.ConverterUtil;
import com.microapp.autumn.api.util.SpiUtil;

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

    private void addInstance(ConsumerConfig consumerConfig) {
        String hash = consumerConfig.getName().concat(":")
                .concat(consumerConfig.getIp())
                .concat(":")
                .concat(consumerConfig.getPort().toString());
        if(instances.contains(hash)) {
            return;
        }
        instances.put(hash, consumerConfig);
    }

    private void removeInstance(ConsumerConfig consumerConfig) {
        instances.remove(consumerConfig);
    }

    private void discovery(MulticastSocket ms, InetAddress group, Integer port) {
        Runnable runnable = () -> {
            handleDiscovery(ms, group, port);
        };

        Thread thread = new Thread(runnable, "autumn-multicast-registry-receiver");
        thread.setDaemon(true);
        thread.start();
        log.info("autumn-multicast-registry begin listening");
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
                    // multicast retry registry
                    Registry registry = SpiUtil.registry();
                    registry.register();
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
        log.info("multicast discovery receive data, ip:{}, data:{}", ip, data);
        ConsumerConfig multicastConfig = ConverterUtil.queryStringToProvider(data);
        if(MulticastEventEnum.REGISTRY.equals(eventEnum)) {
            addInstance(multicastConfig);
            return;
        }

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


}
