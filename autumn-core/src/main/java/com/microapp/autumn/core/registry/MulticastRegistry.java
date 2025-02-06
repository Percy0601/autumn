package com.microapp.autumn.core.registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import com.microapp.autumn.api.Registry;
import com.microapp.autumn.api.config.ApplicationConfig;
import com.microapp.autumn.api.config.ProviderConfig;
import com.microapp.autumn.api.util.CommonUtil;
import com.microapp.autumn.api.util.ConverterUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
@Slf4j
public class MulticastRegistry implements Registry {
    private static volatile MulticastRegistry instance;
    private volatile AtomicLong latest = new AtomicLong(System.currentTimeMillis());
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
    public Boolean register() {
        if((latest.get() + 10 * 1000) > System.currentTimeMillis()) {
            return false;
        }
        latest.set(System.currentTimeMillis());
        log.info("autumn-multicast register init");
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
        MulticastSocket mcs = null;
        InetAddress group = null;
        try {
            group = InetAddress.getByName(ip);
            mcs = new MulticastSocket(port);
            mcs.joinGroup(group);
            byte[] buffer = registryRequest.getBytes();
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, group, port);
            mcs.send(dp);
            Arrays.fill(buffer, (byte) 0);
        } catch (Exception e) {
            log.warn("autumn-multicast-registry receive exception: ", e);
        } finally {
            if (mcs != null) {
                try {
                    mcs.leaveGroup(group);
                    mcs.close();
                } catch (IOException e) {
                    log.warn("autumn-multicast-registry receive exception: ", e);
                }
            }
        }
    }

    private Boolean init() {
        ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
        String ip = applicationConfig.getMulticastIp();
        Integer port = applicationConfig.getMulticastPort();
        log.info("autumn-multicast registry ip:{}, port:{}", ip, port);
        try {
            InetAddress group = InetAddress.getByName(ip);
            MulticastSocket mcs = new MulticastSocket(port);
            registry(mcs, group, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private void registry(MulticastSocket ms, InetAddress group, Integer port) {
        ProviderConfig config = ProviderConfig.getInstance();
        Properties properties = CommonUtil.readClasspath("application.properties");
        config.init(properties);
        String registryRequest = ConverterUtil.registryRequest(config);
        log.info("autumn multicast registry, ip:{}, port:{}, config:{}", group.getHostAddress(), port, config.toString());
        try {
            ms.joinGroup(group);
            byte[] buffer = registryRequest.getBytes();
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, group, port);
            ms.send(dp);
            Arrays.fill(buffer, (byte) 0);
        } catch (Exception e) {
            log.warn("autumn-multicast-registry receive exception: ", e);
        } finally {
            if (ms != null) {
                try {
                    ms.leaveGroup(group);
                    ms.close();
                } catch (IOException e) {
                    log.warn("autumn-multicast-registry receive exception: ", e);
                }
            }
        }
    }
}
