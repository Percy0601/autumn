package autumn.core.server;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
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
import autumn.core.util.CommonUtil;
import autumn.core.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/30
 */
@Slf4j
public class AutumnServer {

    public void start() {
//        Properties properties = CommonUtil.readClasspath("application.properties");
//        ProviderConfig providerConfig = ProviderConfig.getInstance();
//        providerConfig.init(properties);
//        ApplicationConfig applicationConfig = ApplicationConfig.getInstance();
//        applicationConfig.init(properties);
//
//        ThreadUtil singleton = ThreadUtil.getInstance();
//        ExecutorService executorService = singleton.getWorkerExecutor(providerConfig);
//        try {
//            TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(providerConfig.getPort());
//            TThreadedSelectorServer.Args tArgs = new TThreadedSelectorServer.Args(serverTransport);
//            tArgs.transportFactory(new TFramedTransport.Factory());
//            tArgs.protocolFactory(new TBinaryProtocol.Factory());
//            tArgs.executorService(executorService);
//            tArgs.acceptQueueSizePerThread(10);
//            tArgs.stopTimeoutVal(3);
//            tArgs.stopTimeoutUnit(TimeUnit.SECONDS);
//            tArgs.processor(processor);
//
//            ServiceConfig<ControlApi.Iface> controlApiService = new ServiceConfig();
//            controlApiService.setInterfaceClass(ControlApi.Iface.class);
//            TProcessor controlProcessor = new ControlApi.Processor<ControlApi.Iface>(new ControlApiImpl());
//            AttachableProcessor attachableProcessor = new AttachableProcessor(controlProcessor);
//            controlApiService.setRef(attachableProcessor);
//            service(controlApiService);
//            server = new TThreadedSelectorServer(tArgs);
//            server.serve();
//            log.info("autumn server running");
//        } catch (TTransportException e) {
//            log.warn("autumn server start exception, exception:", e);
//            throw new RuntimeException(e);
//        }
    }

}
