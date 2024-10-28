package autumn.core.registry.client;

import java.util.List;

import org.apache.thrift.TServiceClient;

import autumn.core.config.ConsumerConfig;
import autumn.core.config.ReferenceConfig;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public interface Discovery {

    <T extends TServiceClient> void reference(Class<T> classType, ReferenceConfig referenceConfig);

    List<ConsumerConfig> getInstances(String name);
}
