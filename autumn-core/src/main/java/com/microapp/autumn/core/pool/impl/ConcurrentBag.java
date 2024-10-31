package com.microapp.autumn.core.pool.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;

import com.microapp.autumn.api.config.ConsumerConfig;
import com.microapp.autumn.api.config.ReferenceConfig;
import com.microapp.autumn.api.util.AutumnException;

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
    private final AtomicBoolean mayScale = new AtomicBoolean(true);
    private int maxWaiters;
    private ReferenceConfig<? extends TServiceClient> config;

    public ConcurrentBag(ReferenceConfig<? extends TServiceClient> config) {
        maxWaiters = 10;
        this.config = config;
    }

    public ReferenceConfig<? extends TServiceClient> getConfig() {
        return config;
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
        if(mayScale.get()) {
            scale();
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

    private Boolean scale() {
        List<ConsumerConfig> consumerConfigs = config.getInstances();
        mayScale.set(false);
        Boolean nextScale = false;

        for(ConsumerConfig consumerConfig: consumerConfigs) {
            AtomicInteger active = consumerConfig.getActive();
            if(Objects.isNull(active)) {
                active = new AtomicInteger(1);
                consumerConfig.setActive(active);
            } else {
                active.incrementAndGet();
            }
            if(active.get() >= consumerConfig.getConnections().intValue()) {
                continue;
            }
            nextScale = true;
            ConcurrentBagEntry entry = converter(consumerConfig);
            if(Objects.isNull(entry)) {
                continue;
            }
            add(entry);
        }
        mayScale.set(nextScale);
        return nextScale;
    }

    private ConcurrentBagEntry converter(ConsumerConfig consumerConfig) {
        String ip = consumerConfig.getIp();
        Integer port = consumerConfig.getPort();
        String name = consumerConfig.getName();

        TTransport transport = null;
        TTransport tsocket = null;
        try {
            tsocket = new TSocket(ip, port);
            transport = new TFramedTransport(tsocket);
            ConcurrentBagEntry entry = new ConcurrentBagEntryImpl<>(name, ip, port, transport);
            return entry;
        } catch (TTransportException e) {
            log.warn("scale thrift connection fail, error:", e);
        }
        return null;
    }

    public void requite(final ConcurrentBagEntry bagEntry){
        if(closed || bagEntry.getState() == ConcurrentBagEntry.STATE_REMOVED) {
            log.info("ConcurrentBag has been closed.");
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
        mapping.put(entry.getId(), entry);
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
            }
        });

        handoffQueue.forEach(it -> {
            if(it.getIp().equals(ip)) {
                handoffQueue.remove(it);
            }
        });

        mapping.forEach((k, v) -> {
            if(!v.getIp().equals(ip)) {
                return;
            }
            mapping.remove(k);
            v.setState(ConcurrentBagEntry.STATE_REMOVED);
            if(v.getState() != ConcurrentBagEntry.STATE_IN_USE) {
                v.close();
            }
        });
    }

    public void join(ConsumerConfig consumerConfig) {
        List<ConsumerConfig> consumerConfigs = config.getInstances();
        long count = mapping.values().stream()
                .filter(it -> it.getService().equals(consumerConfig.getName()))
                .count();
        int size = config.getInstances().size();
        long avg = count / size;
        if(avg < 1L) {
            ConcurrentBagEntry entry = converter(consumerConfig);
            if(Objects.nonNull(entry)) {
                add(entry);
                return;
            }
        }
        for(long i = 0; i < avg; i++) {
            ConcurrentBagEntry entry = converter(consumerConfig);
            if(Objects.nonNull(entry)) {
                add(entry);
                return;
            }
        }
    }


}
