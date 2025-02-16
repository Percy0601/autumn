package com.microapp.autumn.api.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.Registry;


/**
 * SPI util
 *
 *
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public class SpiUtil {
    private static volatile Map<Class, Object> mapping = new ConcurrentHashMap<>();
    private static volatile Map<Class, List<Object>> mappings = new ConcurrentHashMap<>();

//    private static volatile Discovery discovery;
//    private static volatile Registry registry;

    private SpiUtil() {

    }

    public static <S> S load(Class<S> service) {
        if(mapping.containsKey(service)) {
            return (S)mapping.get(service);
        }
        ServiceLoader<S> load = ServiceLoader.load(service);
        Iterator<S> iterator = load.iterator();
        if(!iterator.hasNext()) {
            mapping.put(service, null);
            return null;
        }
        S s = iterator.next();
        mapping.put(service, s);
        return s;
    }

    public static <S> List<S> loads(Class<S> service) {
        if(mappings.containsKey(service)) {
            return (List<S>)mappings.get(service);
        }
        ServiceLoader<S> load = ServiceLoader.load(service);
        Iterator<S> iterator = load.iterator();
        if(!iterator.hasNext()) {
            mappings.put(service, null);
            return null;
        }
        List<Object> result = new ArrayList<>();
        while(iterator.hasNext()) {
            S s = iterator.next();
            result.add(s);
        }
        mappings.put(service, result);
        return (List<S>) result;
    }
//
//    public static Discovery discovery(){
//        if(null == discovery) {
//            ServiceLoader<Discovery> load = ServiceLoader.load(Discovery.class);
//            Iterator<Discovery> iterator = load.iterator();
//            if(!iterator.hasNext()) {
//                return null;
//            }
//            discovery = iterator.next();
//            return discovery;
//        }
//
//        return discovery;
//    }
//
//    public static Registry registry(){
//        if(null == registry) {
//            ServiceLoader<Registry> load = ServiceLoader.load(Registry.class);
//            Iterator<Registry> iterator = load.iterator();
//            if(!iterator.hasNext()) {
//                return null;
//            }
//            registry = iterator.next();
//        }
//
//        return registry;
//    }
}
