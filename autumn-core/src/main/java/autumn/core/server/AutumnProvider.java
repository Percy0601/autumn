package autumn.core.server;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;

import autumn.api.ControlApi;
import autumn.core.config.ApplicationConfig;
import autumn.core.config.ProviderConfig;
import autumn.core.config.ServiceConfig;
import autumn.core.devops.ControlApiImpl;
import autumn.core.extension.AttachableProcessor;
import autumn.core.pool.AutumnPool;
import autumn.core.util.CommonUtil;
import autumn.core.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/30
 */
@Slf4j
public class AutumnProvider {
    private volatile static AutumnProvider singleton = null;
    private TMultiplexedProcessor processor;
    private Map<Class, ServiceConfig> services;

    private AutumnProvider() {

    }
    public static AutumnProvider getInstance() {
        if (singleton == null) {
            synchronized (AutumnPool.class) {
                if (singleton == null) {
                    singleton = new AutumnProvider();
                    singleton.init();
                    return singleton;
                }
            }
        }
        return singleton;
    }

    private void export(String name, AttachableProcessor serviceProcessor) {

        if(!services.containsKey(name)) {
            return;
        }
        processor.registerProcessor(name, serviceProcessor);
    }

    public <T> AutumnProvider service(ServiceConfig<T> serviceConfig) {
        export(serviceConfig.getInterfaceClass().getName(), serviceConfig.getRef());
        services.put(serviceConfig.getInterfaceClass(), serviceConfig);
        return this;
    }

    private void init() {
        Properties properties = CommonUtil.readClasspath("application.properties");
        ProviderConfig providerConfig = ProviderConfig.getInstance();
        providerConfig.init(properties);
        ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
        applicationConfig.init(properties);

        services = new ConcurrentHashMap<>();
        processor = new TMultiplexedProcessor();
        ServiceConfig<ControlApi.Iface> controlApiService = new ServiceConfig();
        controlApiService.setInterfaceClass(ControlApi.Iface.class);
        TProcessor controlProcessor = new ControlApi.Processor<ControlApi.Iface>(new ControlApiImpl());
        AttachableProcessor attachableProcessor = new AttachableProcessor(controlProcessor);
        controlApiService.setRef(attachableProcessor);
        service(controlApiService);

    }

    public void start() {
        Runnable runnable = () -> {
            try {
                Properties properties = CommonUtil.readClasspath("application.properties");
                ProviderConfig providerConfig = ProviderConfig.getInstance();
                providerConfig.init(properties);

                ThreadUtil singleton = ThreadUtil.getInstance();
                ExecutorService executorService = singleton.getWorkerExecutor(providerConfig);

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
                TServer server = new TThreadedSelectorServer(tArgs);
                server.serve();
                log.info("autumn server running");
            } catch (TTransportException e) {
                log.warn("autumn server start exception, exception:", e);
                throw new RuntimeException(e);
            }
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

}
