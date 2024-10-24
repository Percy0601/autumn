package autumn.core.pool.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.thrift.TServiceClient;

import autumn.core.config.ConsumerConfig;
import autumn.core.config.ReferenceConfig;
import autumn.core.util.AutumnException;
import lombok.extern.slf4j.Slf4j;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;

@Slf4j
public class ConcurrentBag implements AutoCloseable {
    private volatile boolean closed;
    private final ConcurrentLinkedQueue<ConcurrentBagEntry> sharedList = new ConcurrentLinkedQueue<>();
    private final SynchronousQueue<ConcurrentBagEntry> handoffQueue = new SynchronousQueue<>(true);
    private final AtomicInteger waiters = new AtomicInteger(0);
    private final Map<String, ConcurrentBagEntry> mapping = new ConcurrentHashMap<>();
    private int maxWaiters;
    private ReferenceConfig<? extends TServiceClient> config;

    public ConcurrentBag(ReferenceConfig<? extends TServiceClient> config) {
        maxWaiters = 10;
        this.config = config;
    }

    public ConcurrentBagEntry borrow(long timeout, final TimeUnit timeUnit) throws InterruptedException {
        ConcurrentBagEntry entry = sharedList.poll();
        if(null != entry) {
            entry.compareAndSet(ConcurrentBagEntry.STATE_NOT_IN_USE, ConcurrentBagEntry.STATE_IN_USE);
            return entry;
        }
        int waiting = waiters.get();
        if(waiting >= maxWaiters) {
            throw new AutumnException("autumn pool waiters over max!");
        }
        waiters.incrementAndGet();
        entry = scale();
        if(null != entry) {
            entry.compareAndSet(ConcurrentBagEntry.STATE_NOT_IN_USE, ConcurrentBagEntry.STATE_IN_USE);
            waiters.decrementAndGet();
            return entry;
        }

        try {
            timeout = timeUnit.toNanos(timeout);
            do {
                final long start = currentTime();
                final ConcurrentBagEntry bagEntry = handoffQueue.poll(timeout, NANOSECONDS);
                if (bagEntry == null || bagEntry.compareAndSet(ConcurrentBagEntry.STATE_NOT_IN_USE, ConcurrentBagEntry.STATE_IN_USE)) {
                    return bagEntry;
                }
                timeout -= elapsedNanos(start);
            } while (timeout > 0);

            return null;
        } finally {
            waiters.decrementAndGet();
        }
    }

    private ConcurrentBagEntry scale() {
        Integer total = config.getConnections();
        if(mapping.size() >= total) {
            return null;
        }
        List<ConsumerConfig> consumerConfigs = config.getInstances();
//        ConcurrentBagEntry entry = new ConcurrentBagEntryImpl<>();

        return null;
    }

    private ConsumerConfig getScaleConsumer(ReferenceConfig<? extends TServiceClient> config) {
        List<ConsumerConfig> consumerConfigs = config.getInstances();


//        Map<String, Integer> ipConnectionMapping = consumerConfigs.stream()
//                .collect(Collectors.groupingBy(ConsumerConfig::getIp,
//                        Collectors.summingInt(ConsumerConfig::getConnections)));
//
//        Map<String, Long> group = mapping.values().stream()
//                .collect(Collectors.groupingBy(ConcurrentBagEntry::getIp, Collectors.counting()));
//


        return null;
    }

    public void requite(final ConcurrentBagEntry bagEntry){
        if(bagEntry.getState() == ConcurrentBagEntry.STATE_REMOVED) {
            bagEntry.close();
            return;
        }
        bagEntry.setState(ConcurrentBagEntry.STATE_NOT_IN_USE);

        for (int i = 0; waiters.get() > 0; i++) {
            if (bagEntry.getState() != ConcurrentBagEntry.STATE_NOT_IN_USE || handoffQueue.offer(bagEntry)) {
                return;
            } else if ((i & 0xff) == 0xff) {
                parkNanos(MICROSECONDS.toNanos(10));
            } else {
                Thread.yield();
            }
        }
        sharedList.offer(bagEntry);
    }

    public void add(ConcurrentBagEntry entry) {
        if(closed) {
            log.info("ConcurrentBag has been closed, ignoring add()");
            return;
        }
        // spin until a thread takes it or none are waiting
        while (waiters.get() > 0 &&
                entry.getState() == ConcurrentBagEntry.STATE_NOT_IN_USE &&
                !handoffQueue.offer(entry)) {
            Thread.yield();
        }
        sharedList.add(entry);
    }

    @Override
    public void close() throws Exception {
        closed = true;
        mapping.forEach((k, v) -> {
            if(v.getState() != ConcurrentBagEntry.STATE_IN_USE) {
                v.setState(ConcurrentBagEntry.STATE_REMOVED);
                v.close();
                return;
            }
            v.setState(ConcurrentBagEntry.STATE_REMOVED);
        });

        mapping.clear();
        sharedList.clear();
        handoffQueue.clear();
        waiters.set(0);
    }

    private long elapsedNanos(final long startTime) {
        return System.nanoTime() - startTime;
    }

    private long currentTime() {
        return System.nanoTime();
    }

    public void remove(String ip) {
        sharedList.forEach(it -> {
            if(it.getIp().equals(ip)) {
                sharedList.remove(it);
                if(it.getState() == ConcurrentBagEntry.STATE_IN_USE) {
                    it.setState(ConcurrentBagEntry.STATE_REMOVED);
                    return;
                }
                it.setState(ConcurrentBagEntry.STATE_REMOVED);
                it.close();
            }
        });

        handoffQueue.forEach(it -> {
            if(it.getIp().equals(ip)) {
                handoffQueue.remove(it);
                if(it.getState() == ConcurrentBagEntry.STATE_IN_USE) {
                    it.setState(ConcurrentBagEntry.STATE_REMOVED);
                    return;
                }
                it.setState(ConcurrentBagEntry.STATE_REMOVED);
                it.close();
            }
        });

        mapping.forEach((k, v) -> {
            if(!v.getIp().equals(ip)) {
                return;
            }
            mapping.remove(k);
            if(v.getState() != ConcurrentBagEntry.STATE_IN_USE) {
                v.setState(ConcurrentBagEntry.STATE_REMOVED);
                v.close();
                return;
            }
            v.setState(ConcurrentBagEntry.STATE_REMOVED);
        });
    }
}
