package autumn.core.pool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import autumn.core.pool.impl.ConcurrentBag;
import autumn.core.pool.impl.ConcurrentBagEntry;

public final class AutumnPool {
    private volatile static AutumnPool singleton = null;
    private ConcurrentHashMap<String, ConcurrentBag> bagMapping;

    private AutumnPool() {

    }

    private void init() {

    }

    public static AutumnPool getInstance() {
        if (singleton == null) {
            synchronized (AutumnPool.class) {
                if (singleton == null) {
                    singleton = new AutumnPool();
                    singleton.init();
                    return singleton;
                }
            }
        }
        return singleton;
    }

    public ConcurrentBagEntry borrow(String service) {
        if(!bagMapping.contains(service)) {

        }
        ConcurrentBag bag = bagMapping.get(service);
        try {
            ConcurrentBagEntry entry = bag.borrow(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void remove(String service, String ip) {
        ConcurrentBag bag = bagMapping.get(service);
        bag.remove(ip);
    }
}
