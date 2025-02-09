package com.microapp.autumn.sample.test;

import java.io.IOException;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.TTransportException;
import org.junit.jupiter.api.Test;

import com.microapp.autumn.api.config.ServiceConfig;
import com.microapp.autumn.api.extension.AttachableProcessor;
import com.microapp.autumn.core.AutumnBootstrap;
import com.microapp.autumn.sample.api.SomeService;
import com.microapp.autumn.sample.api.User;
import com.microapp.autumn.sample.service.SomeServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/8
 */
@Slf4j
public class TrainingProviderTest {

    @Test
    void testProvider() {
        log.info("========================");
        ServiceConfig<SomeService.Iface> fooServiceConfig = new ServiceConfig<>();
        fooServiceConfig.setInterfaceClass(SomeService.Iface.class);
        TProcessor tprocessor = new SomeService.Processor<SomeService.Iface>(new SomeServiceImpl());
        AttachableProcessor attachableProcessor = new AttachableProcessor(tprocessor);
        fooServiceConfig.setRef(attachableProcessor);

        AutumnBootstrap.getInstance()
                .service(fooServiceConfig)
                .serve();
        try {
            System.in.read();
        } catch (IOException e) {
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
