package com.microapp.core.devops;

import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;

import com.microapp.api.ControlApi;
import com.microapp.core.config.ApplicationConfig;
import com.microapp.core.config.ProviderConfig;
import com.microapp.core.config.ReferenceConfig;
import com.microapp.core.pool.AutumnPool;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
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

}
