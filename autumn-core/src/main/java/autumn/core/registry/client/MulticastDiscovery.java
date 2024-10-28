package autumn.core.registry.client;

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

import org.apache.thrift.TServiceClient;

import autumn.core.config.ApplicationConfig;
import autumn.core.config.ConsumerConfig;
import autumn.core.config.ProviderConfig;
import autumn.core.config.ReferenceConfig;
import autumn.core.enums.MulticastEventEnum;
import autumn.core.pool.AutumnPool;
import autumn.core.util.ConverterUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/9/30
 */
@Slf4j
@Getter
public class MulticastDiscovery implements Discovery{
    private volatile static MulticastDiscovery singleton = null;
    private ConcurrentHashMap<Class<? extends TServiceClient>, ReferenceConfig> refers = new ConcurrentHashMap<>();
    private AtomicBoolean initStatus = new AtomicBoolean(false);
    private MulticastSocket mc;
    private MulticastDiscovery() {

    }

    public static MulticastDiscovery getInstance() {
        if (singleton == null) {
            synchronized (AutumnPool.class) {
                if (singleton == null) {
                    singleton = new MulticastDiscovery();
                    singleton.init();
                    return singleton;
                }
            }
        }
        return singleton;
    }

    public void init() {
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
        };

        Thread thread = new Thread(runnable, "autumn-multicast-registry-receiver");
        thread.setDaemon(true);
        thread.start();
        log.info("autumn-multicast-registry begin listening");
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
    public List<ConsumerConfig> getInstances(String name) {
        ReferenceConfig<? extends TServiceClient> config =AutumnPool.getInstance().getReferenceConfig(name);
        if(Objects.isNull(config)) {
            return null;
        }
        return config.getInstances();
    }
}
