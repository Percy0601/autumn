package autumn.core.util;

import java.util.Iterator;
import java.util.ServiceLoader;

import autumn.core.registry.Registry;
import autumn.core.registry.client.Discovery;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public class SpiUtil {

    public static Discovery discovery(){
        ServiceLoader<Discovery> load = ServiceLoader.load(Discovery.class);
        Iterator<Discovery> iterator = load.iterator();
        if(!iterator.hasNext()) {
            return null;
        }
        Discovery discovery = iterator.next();
        return discovery;
    }

    public static Registry registry(){
        ServiceLoader<Registry> load = ServiceLoader.load(Registry.class);
        Iterator<Registry> iterator = load.iterator();
        if(!iterator.hasNext()) {
            return null;
        }
        Registry registry = iterator.next();
        return registry;
    }
}
