package autumn.api.test;

import java.io.IOException;

import org.apache.thrift.TProcessor;
import org.junit.jupiter.api.Test;

import autumn.core.extension.AttachableProcessor;
import autumn.sample.api.SomeService;
import autumn.core.AutumnBootstrap;
import autumn.core.config.ReferenceConfig;
import autumn.core.config.ServiceConfig;
import autumn.sample.service.SomeServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/8
 */

@Slf4j
public class TrainingTest {

    @Test
    void testProvider() {
        log.info("========================");
        ServiceConfig<SomeService.Iface> fooServiceConfig = new ServiceConfig<>();
        fooServiceConfig.setInterfaceClass(SomeService.Iface.class);
        TProcessor tprocessor = new SomeService.Processor<SomeService.Iface>(new SomeServiceImpl());
        AttachableProcessor attachableProcessor = new AttachableProcessor(tprocessor);
        fooServiceConfig.setRef(attachableProcessor);

        AutumnBootstrap.getInstance().serve();

        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
