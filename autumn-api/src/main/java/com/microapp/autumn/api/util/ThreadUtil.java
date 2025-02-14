package com.microapp.autumn.api.util;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.microapp.autumn.api.config.ProviderConfig;


/**
 * @author: baoxin.zhao
 * @date: 2024/5/8
 */
public class ThreadUtil {
    private static volatile ThreadUtil instance;
    private ExecutorService workerExecutor;
    private ScheduledExecutorService scheduledExecutorService;
    private static final String THREAD_POOL_NAME_WORKER = "autumn-thread-pool";
    private ThreadUtil() {

    }

    public ExecutorService getSingleExecutorService() {
        return scheduledExecutorService;
    }

    public static ThreadUtil getInstance() {
        if(Objects.isNull(instance)) {
            synchronized (ThreadUtil.class) {
                if(Objects.isNull(instance)) {
                    instance = new ThreadUtil();
                }
            }
        }
        instance.init();
        return instance;
    }

    private void init() {
        if (Objects.isNull(scheduledExecutorService)) {
            synchronized (this) {
                if(Objects.isNull(scheduledExecutorService)) {
                    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                }
            }
        }
    }

    public void submit(Runnable runnable) {
        scheduledExecutorService.submit(runnable);
    }

    public void scheduleWithFixedDelay(Runnable runnable, Long delay) {
        if (Objects.isNull(scheduledExecutorService)) {
            synchronized (this) {
                if(Objects.isNull(scheduledExecutorService)) {
                    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                }
            }
        }
        scheduledExecutorService.scheduleWithFixedDelay(runnable, delay, delay, TimeUnit.SECONDS);
    }

    public ExecutorService getWorkerExecutor(ProviderConfig providerConfig) {
        if(Objects.isNull(workerExecutor)) {
            synchronized (this) {
                if(Objects.isNull(workerExecutor)) {
                    Integer minThreads = providerConfig.getMinThreads();
                    Integer maxThreads = providerConfig.getMaxThreads();
                    Integer workerKeepAliveTime = providerConfig.getWorkerKeepAliveTime();
                    workerExecutor = new ThreadPoolExecutor(1,
                            300,
                            5,
                            TimeUnit.MINUTES,
                            new LinkedBlockingQueue<>(10000),
                            new ThreadFactoryWithGarbageCleanup(THREAD_POOL_NAME_WORKER));
                }

            }
        }
        return workerExecutor;
    }


    class ThreadFactoryWithGarbageCleanup implements ThreadFactory {
        private final String namePrefix;

        public ThreadFactoryWithGarbageCleanup(String threadPoolName){
            namePrefix = threadPoolName;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread newThread = new Thread(r);
            newThread.setName(namePrefix + ":Thread-" + newThread.getId());
            return newThread;
        }
    }

}
