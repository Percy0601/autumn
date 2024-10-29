package autumn.core.registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import autumn.core.config.ApplicationConfig;
import autumn.core.config.ProviderConfig;
import autumn.core.util.ConverterUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
@Slf4j
public class MulticastRegistry implements Registry{
    @Override
    public Boolean register() {
        return init();
    }

    @Override
    public void shutdownHook() {
        ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
        String ip = applicationConfig.getMulticastIp();
        Integer port = applicationConfig.getMulticastPort();
        ProviderConfig config = ProviderConfig.getInstance();
        String registryRequest = ConverterUtil.registryRequest(config);
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
