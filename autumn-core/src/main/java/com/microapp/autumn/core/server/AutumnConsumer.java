package com.microapp.autumn.core.server;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.api.util.SpiUtil;
import com.microapp.autumn.core.pool.AutumnPool;
import com.microapp.autumn.core.pool.impl.ConcurrentBagEntry;
import com.microapp.autumn.core.pool.impl.ConcurrentBagEntryImpl;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/31
 */
public class AutumnConsumer {
    private volatile static AutumnConsumer singleton = null;

    public static AutumnConsumer provider() {
        if (singleton == null) {
            synchronized (AutumnProvider.class) {
                if (singleton == null) {
                    singleton = new AutumnConsumer();
                    return singleton;
                }
            }
        }
        return singleton;
    }

    public <T extends TServiceClient> ConcurrentBagEntry reference(ReferenceConfig<T> referenceConfig) {
        Discovery discovery = SpiUtil.discovery();
        discovery.reference(referenceConfig.getInterfaceClass(), referenceConfig);
        discovery.discovery();


        AutumnPool.getInstance().referenceConfig(referenceConfig);
        ConcurrentBagEntry entry = AutumnPool.getInstance().borrow(referenceConfig.getName());
        return entry;
    }

    public static void requite(ConcurrentBagEntry bagEntry) {
        AutumnPool.getInstance().requite(bagEntry);
    }


}
