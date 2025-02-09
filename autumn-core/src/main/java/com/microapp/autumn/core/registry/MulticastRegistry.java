package com.microapp.autumn.core.registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.microapp.autumn.api.Registry;
import com.microapp.autumn.api.config.ApplicationConfig;
import com.microapp.autumn.api.config.ProviderConfig;
import com.microapp.autumn.api.util.CommonUtil;
import com.microapp.autumn.api.util.ConverterUtil;
import com.microapp.autumn.api.util.ThreadUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
@Slf4j
public class MulticastRegistry implements Registry {


    private static volatile MulticastRegistry instance;
    private volatile AtomicLong latest = new AtomicLong(System.currentTimeMillis());
    private volatile AtomicBoolean state = new AtomicBoolean(false);

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
        // avoid frequent registry
//        if((latest.get() + 3 * 1000) > System.currentTimeMillis()) {
//            return false;
//        }
//        latest.set(System.currentTimeMillis());
        if(state.get()) {
            return true;
        }
        state.compareAndSet(false, true);
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

        ProviderConfig config = ProviderConfig.getInstance();
        Properties properties = CommonUtil.readClasspath("application.properties");
        config.init(properties);
        log.info("autumn-multicast registry ip:{}, port:{}, config:{}", ip, port, config);
        Runnable runnable = () -> {
            log.info("autumn-multicast registry ip:{}, port:{}, config:{}", ip, port, config);
            try {
                InetAddress group = InetAddress.getByName(ip);
                MulticastSocket mcs = new MulticastSocket(port);
                registry(mcs, group, port, config);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        ThreadUtil.getInstance().scheduleWithFixedDelay(runnable, 3L);
        return true;
    }

    private void registry(MulticastSocket ms, InetAddress group, Integer port, ProviderConfig config) {
        String registryRequest = ConverterUtil.registryRequest(config);
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
