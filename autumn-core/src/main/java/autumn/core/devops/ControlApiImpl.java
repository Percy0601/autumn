package autumn.core.devops;

import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;

import autumn.api.ControlApi;
import autumn.core.config.ApplicationConfig;
import autumn.core.config.ProviderConfig;
import autumn.core.config.ReferenceConfig;
import autumn.core.pool.AutumnPool;

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
