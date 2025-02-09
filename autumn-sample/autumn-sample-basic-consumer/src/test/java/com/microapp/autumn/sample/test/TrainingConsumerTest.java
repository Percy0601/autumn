package com.microapp.autumn.sample.test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.jupiter.api.Test;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.Registry;
import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.api.enums.RegistryTypeEnum;
import com.microapp.autumn.api.util.SpiUtil;
import com.microapp.autumn.core.AutumnBootstrap;
import com.microapp.autumn.core.pool.AutumnPool;
import com.microapp.autumn.core.pool.impl.ConcurrentBagEntry;
import com.microapp.autumn.core.server.AutumnConsumer;
import com.microapp.autumn.sample.api.SomeService;
import com.microapp.autumn.sample.api.User;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/8
 */
@Slf4j
public class TrainingConsumerTest {

    @Test
    void testConsumer() {

        AutumnBootstrap.getInstance().serve();

        AutumnConsumer consumer = AutumnConsumer.provider();
        ReferenceConfig<SomeService.Client> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setName("training-a");
        referenceConfig.setInterfaceClass(SomeService.Client.class);
        referenceConfig.setPoolTimeout(10000L);
        referenceConfig.setRegistryTypeEnum(RegistryTypeEnum.MULTICAST);
        referenceConfig.setSocketTimeout(3000L);
        consumer.reference(referenceConfig);
        List<ConsumerConfig> instances = SpiUtil.load(Discovery.class).getInstances(referenceConfig.getName());

        if(Objects.isNull(instances) || instances.size() < 1) {
            for(int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                SpiUtil.load(Registry.class).register();
                instances = SpiUtil.load(Discovery.class).getInstances(referenceConfig.getName());
                if(Objects.nonNull(instances) && instances.size() > 1) {
                    break;
                }
            }
        }

        Runnable r = () -> {
            ConcurrentBagEntry ce = null;
            try {
                ce = AutumnPool.getInstance().borrow(referenceConfig.getName());
                String msg = UUID.randomUUID().toString();
                TTransport transport = ce.getEntry();
                TProtocol protocol = new TBinaryProtocol(transport);
                TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(protocol, SomeService.Iface.class.getName());
                SomeService.Iface client = new SomeService.Client(multiplexedProtocol);

                String echoResult = client.echo(msg);
                if(!echoResult.equals("Hello, ".concat(msg))) {
                    log.info("===========echoResult fail: {}", echoResult);
                } else {
                    log.info("===========echoResult success: {}, content:{}", ce.getId(), echoResult);
                }
            } catch (TException e) {
                log.warn("===========echoResult exception: {}, id:{}, state:{}", e.getMessage(), ce.getId(), ce.getState());
            } finally {
                AutumnPool.getInstance().requite(ce);
            }
        };

        ExecutorService es = Executors.newFixedThreadPool(10);
        int i = 0;
        for(; i < 10000; i++) {
             es.submit(r);
             if(i % 2000 == 0) {
                 try {
                     Thread.sleep(100L);
                 } catch (InterruptedException e) {
                     throw new RuntimeException(e);
                 }
             }

        }

        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("execute over~{}", i);
    }

    @Test
    void testTSerializer() {
        try {
            TSerializer serializer = new TSerializer(new TJSONProtocol.Factory());

            User user = new User();
            user.setUserId(1);
            user.setUsername("Hello");
            byte[] bytes = serializer.serialize(user);
            String result = new String(bytes);

            log.info("==================, {}", result);

            TDeserializer deserializer = new TDeserializer(new TSimpleJSONProtocol.Factory());
            User user2 = new User();
            String json2 = "{\"userId\":2,\"username\":\"Hello2\"}";
            deserializer.deserialize(user2, result, "utf-8");
            log.info("==================, user2:{}", user2);
        } catch (TTransportException e) {
            throw new RuntimeException(e);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }


}
