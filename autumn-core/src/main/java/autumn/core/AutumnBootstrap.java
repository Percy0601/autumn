package autumn.core;

import java.util.List;
import java.util.Objects;

import org.apache.thrift.TServiceClient;

import autumn.core.config.ApplicationConfig;
import autumn.core.config.ConsumerConfig;
import autumn.core.config.ReferenceConfig;
import autumn.core.registry.client.Discovery;
import autumn.core.server.AutumnProvider;
import autumn.core.util.SpiUtil;
import autumn.core.util.ThreadUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/5/8
 */
@Slf4j
@Setter
@Getter
public class AutumnBootstrap {
    private static volatile AutumnBootstrap instance;

    private ApplicationConfig applicationConfig;
    private AutumnBootstrap() {}

    public static AutumnBootstrap getInstance() {
        if(Objects.isNull(instance)) {
            synchronized (AutumnBootstrap.class) {
                if(Objects.isNull(instance)) {
                    instance = new AutumnBootstrap();
                }
            }
        }
        return instance;
    }

    public void serve() {
        AutumnProvider provider = AutumnProvider.getInstance();
        provider.start();
        log.info("autumn registry finish");

        SpiUtil.registry().register();
        log.info("autumn registry finish");

        shutdownHook();
    }

    private void shutdownHook() {
        Runnable shutdownHook = () -> {
            SpiUtil.registry().shutdownHook();
        };
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
    }

    private void checkHealth() {
        Runnable runnable = () -> {
            Discovery discovery = SpiUtil.discovery();
            List<String> services = discovery.services();
            if(Objects.isNull(services) || services.size() < 1) {
                return;
            }
            services.forEach(it -> {
                List<ConsumerConfig> instances = discovery.getInstances(it);



            });

        };
        ThreadUtil.getInstance().scheduleWithFixedDelay(runnable, 300L);
    }

    public <T extends TServiceClient> AutumnBootstrap reference(ReferenceConfig<T> referenceConfig) {
        Discovery discovery = SpiUtil.discovery();
        discovery.reference(referenceConfig.getInterfaceClass(), referenceConfig);
        return this;
    }

}
