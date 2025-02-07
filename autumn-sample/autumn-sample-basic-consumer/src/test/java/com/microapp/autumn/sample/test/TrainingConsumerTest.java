package com.microapp.autumn.sample.test;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.microapp.autumn.api.util.ThreadUtil;
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
        AutumnPool.getInstance().requite(entry);

//        AtomicInteger total = new AtomicInteger(0);
//        Runnable r = () -> {
//            for(; total.get() < 10000; total.incrementAndGet()) {
//                ConcurrentBagEntry ce = null;
//                try {
//                    if(ce == null) {
//                        continue;
//                    }
//                    ce = AutumnPool.getInstance().borrow(referenceConfig.getName());
//                    String msg = UUID.randomUUID().toString();
//                    TTransport transport = ce.getEntry();
//                    TProtocol protocol = new TBinaryProtocol(transport);
//                    TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(protocol, SomeService.Iface.class.getName());
//                    SomeService.Iface client = new SomeService.Client(multiplexedProtocol);
//
//                    String echoResult = client.echo(msg);
//                    if(!echoResult.equals("Hello, ".concat(msg))) {
//                        log.info("===========echoResult result: {}", echoResult);
//                    } else {
//                        log.info("===========echoResult success: {}", ce.getId());
//                    }
//                } catch (TException e) {
//                    log.warn("===========echoResult exception: {}", e.getMessage());
//                } finally {
//                    log.info("===========echoResult final: {}", total.get());
//                    AutumnPool.getInstance().requite(ce);
//                }
//            }
//        };
//
////        ExecutorService es = Executors.newFixedThreadPool(10);
////        es.submit(r);
////        for(int i = 0; i < 10000; i++) {
////            es.submit(r);
////        }
//
//        for(int i = 0; i < 10; i++) {
//            Thread t = new Thread(r, "t-" + i);
//            t.start();
//        }
//
//        try {
//            System.in.read();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

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

    @Test
    void test() {
        long timeout = 1;
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        timeout = timeUnit.toMillis(timeout);
        log.info("=====================, 1");
        long current = currentTime();
        while (true) {
            Thread.yield();
            timeout -= elapsedNanos(currentTime());
            if(timeout < 0) {
                break;
            }
        }
        log.info("=====================, 2");
    }

    private long elapsedNanos(final long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }

    @Test
    void test2() {
        SynchronousQueue<String> handoffQueue = new SynchronousQueue<>(true);

        for(int i = 0; i < 10; i++) {
            String c = UUID.randomUUID().toString();
            boolean r = false;
            try {
                handoffQueue.put(c);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("save content:{}, result:{}", c, r);
        }

        for(int i = 0; i < 10; i++) {
            String result = handoffQueue.poll();
            log.info("==========:{}", result);
        }


    }


}
