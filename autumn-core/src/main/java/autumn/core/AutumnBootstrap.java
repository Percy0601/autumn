package autumn.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;

import autumn.api.ControlApi;
import autumn.core.config.ApplicationConfig;
import autumn.core.config.ConsulConfig;
import autumn.core.config.ConsumerConfig;
import autumn.core.config.ProviderConfig;
import autumn.core.config.ReferenceConfig;
import autumn.core.config.ServiceConfig;
import autumn.core.devops.ControlApiImpl;
import autumn.core.extension.AttachableProcessor;
import autumn.core.registry.client.Discovery;
import autumn.core.util.AutumnException;
import autumn.core.util.CommonUtil;
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
    private TServer server;
    private ApplicationConfig applicationConfig;
    private ProviderConfig providerConfig;
    private ConsulConfig registryConfig;
    private TMultiplexedProcessor processor;
    private Map<Class, ServiceConfig> services = new ConcurrentHashMap<>();
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

    private void handleDefaultRegistryConfig(ConsulConfig registryConfig) {
        if(Objects.isNull(registryConfig)) {
            registryConfig = new ConsulConfig();
        }

        String ipAddress = CommonUtil.getHostIpAddress();
        String name = applicationConfig.getName();
        String instanceId = name.concat(":")
                .concat(ipAddress)
                .concat(":")
                .concat(providerConfig.getPort().toString());
        registryConfig.setInstanceId(instanceId);
        Integer healthCheckInterval = registryConfig.getHealthCheckInterval();
        if(Objects.isNull(healthCheckInterval)) {
            healthCheckInterval = 10;
        } else {
            healthCheckInterval = healthCheckInterval > 0? healthCheckInterval: 10;
            healthCheckInterval = healthCheckInterval < 60? healthCheckInterval: 60;
        }
        registryConfig.setHealthCheckInterval(healthCheckInterval);
    }

    public TServer getServer() {
        return server;
    }

    private void export(String name, AttachableProcessor serviceProcessor) {
        if(Objects.isNull(processor)) {
            processor = new TMultiplexedProcessor();
        }
        if(!services.containsKey(name)) {
            return;
        }
        processor.registerProcessor(name, serviceProcessor);
    }

    public void serve() {
        start();
        registry();
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

    public void start() {
        Properties properties = CommonUtil.readClasspath("application.properties");
        providerConfig = ProviderConfig.getInstance();
        providerConfig.init(properties);
        applicationConfig = ApplicationConfig.getInstance();
        applicationConfig.init(properties);

        ThreadUtil singleton = ThreadUtil.getInstance();
        ExecutorService executorService = singleton.getWorkerExecutor(providerConfig);
        try {
            TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(providerConfig.getPort());
            TThreadedSelectorServer.Args tArgs = new TThreadedSelectorServer.Args(serverTransport);
            tArgs.transportFactory(new TFramedTransport.Factory());
            tArgs.protocolFactory(new TBinaryProtocol.Factory());
            tArgs.executorService(executorService);
            tArgs.acceptQueueSizePerThread(10);
            tArgs.stopTimeoutVal(3);
            tArgs.stopTimeoutUnit(TimeUnit.SECONDS);
            tArgs.processor(processor);

            ServiceConfig<ControlApi.Iface> controlApiService = new ServiceConfig();
            controlApiService.setInterfaceClass(ControlApi.Iface.class);
            TProcessor controlProcessor = new ControlApi.Processor<ControlApi.Iface>(new ControlApiImpl());
            AttachableProcessor attachableProcessor = new AttachableProcessor(controlProcessor);
            controlApiService.setRef(attachableProcessor);
            service(controlApiService);
            server = new TThreadedSelectorServer(tArgs);
//            Thread thread = new Thread();
//            thread.
            server.serve();
            log.info("autumn server running");
        } catch (TTransportException e) {
            log.warn("autumn server start exception, exception:", e);
            throw new RuntimeException(e);
        }
    }

    private void registry() {
        if(Objects.isNull(registryConfig) || !Boolean.TRUE.equals(registryConfig) ) {
            log.info("autumn not config register-info, not registry");
        }
        if(!Boolean.TRUE.equals(registryConfig.getRegister())) {
            log.info("autumn not config register-info, not registry");
            return;
        }
        SpiUtil.registry().register();
        log.info("autumn registry finish");
    }

    public <T> AutumnBootstrap service(ServiceConfig<T> serviceConfig) {
        export(serviceConfig.getInterfaceClass().getName(), serviceConfig.getRef());
        services.put(serviceConfig.getInterfaceClass(), serviceConfig);
        return this;
    }

    public <T extends TServiceClient> AutumnBootstrap reference(ReferenceConfig<T> referenceConfig) {
        Discovery discovery = SpiUtil.discovery();
        discovery.reference(referenceConfig.getInterfaceClass(), referenceConfig);
        return this;
    }

}
