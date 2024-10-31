package com.microapp.autumn.api.util;

import java.util.Iterator;
import java.util.ServiceLoader;

import com.microapp.autumn.api.Discovery;
import com.microapp.autumn.api.Registry;


/**
 * @author: baoxin.zhao
 * @date: 2024/10/28
 */
public class SpiUtil {
    private static Discovery discovery;
    private static Registry registry;

    public static Discovery discovery(){
        if(null == discovery) {
            ServiceLoader<Discovery> load = ServiceLoader.load(Discovery.class);
            Iterator<Discovery> iterator = load.iterator();
            if(!iterator.hasNext()) {
                return null;
            }
            discovery = iterator.next();
            return discovery;
        }

        return discovery;
    }

    public static Registry registry(){
        if(null == registry) {
            ServiceLoader<Registry> load = ServiceLoader.load(Registry.class);
            Iterator<Registry> iterator = load.iterator();
            if(!iterator.hasNext()) {
                return null;
            }
            registry = iterator.next();
        }

        return registry;
    }
}
