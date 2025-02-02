package com.microapp.autumn.core.devops;

import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;

import com.microapp.autumn.api.ControlApi;
import com.microapp.autumn.api.config.ApplicationConfig;
import com.microapp.autumn.api.config.ProviderConfig;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.core.pool.AutumnPool;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
@Slf4j
public class ControlApiImpl implements ControlApi.Iface {

    @Override
    public String health() throws TException {
        return "OK";
    }

    @Override
    public String applicationInfo() throws TException {
        String result = ApplicationConfig.getInstance().toString();
        return result;
    }

    @Override
    public String providerInfo() throws TException {
        String result = ProviderConfig.getInstance().toString();
        return result;
    }

    @Override
    public String referenceConfig(String name) throws TException {
        ReferenceConfig<? extends TServiceClient> config = AutumnPool.getInstance().getReferenceConfig(name);
        String result = config.toString();
        return result;
    }


    @Override
    public String check(String ip, int port) throws TException{
        TTransport transport = null;
        TTransport tsocket = null;
        try {
            tsocket = new TSocket(ip, port);
            transport = new TFramedTransport(tsocket);
            TProtocol protocol = new TBinaryProtocol(transport);
            transport.open();
            TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(protocol, ControlApi.Iface.class.getName());
            ControlApi.Iface client = new ControlApi.Client(multiplexedProtocol);
            String result = client.health();
            return String.valueOf("OK".equals(result));
        } catch (TTransportException e) {
            log.warn("autumn check health connect exception: ", e);
            return String.valueOf(Boolean.FALSE);
        } catch (TException e) {
            log.warn("autumn check health exception: ", e);
            return String.valueOf(Boolean.FALSE);
        } finally {
            transport.close();
            tsocket.close();
        }
    }

}
