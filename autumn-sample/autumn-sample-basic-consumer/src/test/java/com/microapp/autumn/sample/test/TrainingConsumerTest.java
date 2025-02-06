package com.microapp.autumn.sample.test;

import java.util.Objects;

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

import com.microapp.autumn.api.Registry;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.api.enums.RegistryTypeEnum;
import com.microapp.autumn.api.util.SpiUtil;
import com.microapp.autumn.core.pool.AutumnPool;
import com.microapp.autumn.core.pool.impl.ConcurrentBagEntry;
import com.microapp.autumn.core.registry.client.MulticastDiscovery;
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
        SpiUtil.discovery().discovery();
        AutumnConsumer consumer = AutumnConsumer.provider();
        ReferenceConfig<SomeService.Client> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setName("training-a");
        referenceConfig.setInterfaceClass(SomeService.Client.class);
        referenceConfig.setPoolTimeout(10000L);
        referenceConfig.setRegistryTypeEnum(RegistryTypeEnum.MULTICAST);
        referenceConfig.setSocketTimeout(3000L);
        consumer.reference(referenceConfig);

        ConcurrentBagEntry entry = AutumnPool.getInstance().borrow(referenceConfig.getName());
        if(Objects.isNull(entry)) {
            for (int i = 0; i < 10; i++) {
                log.info("===========registry: {}", i);
                Registry registry = SpiUtil.registry();
                registry.register();
                entry = AutumnPool.getInstance().borrow(referenceConfig.getName());
                if(Objects.nonNull(entry)) {
                    break;
                }
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        TTransport transport = entry.getEntry();
        TProtocol protocol = new TBinaryProtocol(transport);
        TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(protocol, SomeService.Iface.class.getName());
        SomeService.Iface client = new SomeService.Client(multiplexedProtocol);
        try {
            String echoResult = client.echo("1111");
            log.info("===========echoResult result: {}", echoResult);
        } catch (TException e) {
            throw new RuntimeException(e);
        }

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
