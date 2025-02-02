package com.microapp.autumn.core.registry.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;

import com.microapp.autumn.api.ControlApi;
import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.config.ApplicationConfig;
import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.config.ProviderConfig;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.api.enums.MulticastEventEnum;
import com.microapp.autumn.api.extension.AttachableBinaryProtocol;
import com.microapp.autumn.api.util.ConverterUtil;
import com.microapp.autumn.api.util.SpiUtil;
import com.microapp.autumn.api.util.ThreadUtil;
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

    private ConcurrentHashMap<Class<? extends TServiceClient>, ReferenceConfig> refers = new ConcurrentHashMap<>();
    private AtomicBoolean initStatus = new AtomicBoolean(false);
    private MulticastSocket mc;


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
            discovery(mc, group, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addInstance(String name, ConsumerConfig consumerConfig) {
        if(!refers.contains(name)) {
            return;
        }
        ReferenceConfig referenceConfig = refers.get(name);
        List<ConsumerConfig> consumers = referenceConfig.getInstances();
        if(consumers.contains(consumerConfig)) {
            return;
        }
        consumers.add(consumerConfig);
        AutumnPool.getInstance().join(consumerConfig);
    }

    private void removeInstance(String name, ConsumerConfig consumerConfig) {
        if(!refers.contains(name)) {
            return;
        }
        ReferenceConfig referenceConfig = refers.get(name);
        List<ConsumerConfig> consumers = referenceConfig.getInstances();
        if(consumers.contains(consumerConfig)) {
            consumers.remove(consumerConfig);
            AutumnPool.getInstance().leave(consumerConfig);
        }
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
                    ProviderConfig config = ProviderConfig.getInstance();
                    String registryResponse = ConverterUtil.registryResponse(config);
                    byte[] sendBuff = registryResponse.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendBuff, sendBuff.length, group, port);
                    mc.send(sendPacket);
                    Arrays.fill(sendBuff, (byte) 0);
                    return;
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
            addInstance(multicastConfig.getName(), multicastConfig);
            return;
        }

        removeInstance(multicastConfig.getName(), multicastConfig);
    }


    @Override
    public <T extends TServiceClient> void reference(Class<T> classType, ReferenceConfig referenceConfig) {
        if(refers.contains(classType)) {
            return;
        }
        refers.put(classType, referenceConfig);
    }

    @Override
    public List<String> services() {
        List<String> services = refers.values()
                .stream()
                .map(ReferenceConfig::getName)
                .distinct()
                .collect(Collectors.toList());
        return services;
    }

    @Override
    public List<ConsumerConfig> getInstances(String name) {
        ReferenceConfig<? extends TServiceClient> config = AutumnPool.getInstance().getReferenceConfig(name);
        if(Objects.isNull(config)) {
            return null;
        }
        return config.getInstances();
    }

    private void handleCheckHealth() {
        Runnable runnable = () -> {
            Discovery discovery = SpiUtil.discovery();
            List<String> services = discovery.services();
            if(Objects.isNull(services) || services.size() < 1) {
                return;
            }
            services.forEach(it -> {
                List<ConsumerConfig> instances = discovery.getInstances(it);
                if(instances.size() < 1) {
                    return;
                }
                instances.forEach(instance -> {
                    Boolean result = checkHealth(instance.getIp(), instance.getPort());
                    if(!result) {
                        Integer count = instance.getCheckFail();
                        if(Objects.isNull(count)) {
                            instance.setCheckFail(1);
                            return;
                        }
                        instance.setCheckFail(++count);
                    }
                    instance.setCheckFail(0);
                });
            });
        };
        ThreadUtil.getInstance().scheduleWithFixedDelay(runnable, 300L);
    }

    public Boolean checkHealth(String ip, Integer port) {
        TTransport transport = null;
        TTransport tsocket = null;
        try {
            tsocket = new TSocket(ip, port);
            transport = new TFramedTransport(tsocket);
            TProtocol protocol = new TBinaryProtocol(transport);
            transport.open();
            TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(protocol, ControlApi.Iface.class.getName());
            ControlApi.Iface client = new ControlApi.Client(multiplexedProtocol);
            String result = client.health();
            return "OK".equals(result);
        } catch (TTransportException e) {
            log.warn("autumn check health connect exception: ", e);
            return false;
        } catch (TException e) {
            log.warn("autumn check health exception: ", e);
            return false;
        } finally {
          transport.close();
          tsocket.close();
        }
    }


}
