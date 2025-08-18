package com.simpleflow.core.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置类
 * 用于解析和创建线程池
 */
@Data
@Slf4j
public class ThreadPoolConfig {
    
    private int coreSize = 4;                    // 核心线程数
    private int maxSize = 8;                     // 最大线程数
    private int keepAliveSeconds = 60;           // 线程空闲时间（秒）
    private int queueCapacity = 100;             // 队列容量
    private String threadNamePrefix = "simple-flow-"; // 线程名前缀
    
    /**
     * 从配置Map中解析线程池配置
     */
    @SuppressWarnings("unchecked")
    public static ThreadPoolConfig parseFromConfig(Map<String, Object> config) {
        ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig();
        
        if (config == null) {
            log.info("未找到线程池配置，使用默认配置");
            return threadPoolConfig;
        }
        
        Map<String, Object> threadPoolMap = (Map<String, Object>) config.get("thread-pool");
        if (threadPoolMap == null) {
            log.info("未找到thread-pool配置，使用默认配置");
            return threadPoolConfig;
        }
        
        // 解析各个配置项
        Object coreSizeObj = threadPoolMap.get("core-size");
        if (coreSizeObj instanceof Number) {
            threadPoolConfig.setCoreSize(((Number) coreSizeObj).intValue());
        }
        
        Object maxSizeObj = threadPoolMap.get("max-size");
        if (maxSizeObj instanceof Number) {
            threadPoolConfig.setMaxSize(((Number) maxSizeObj).intValue());
        }
        
        Object keepAliveObj = threadPoolMap.get("keep-alive-seconds");
        if (keepAliveObj instanceof Number) {
            threadPoolConfig.setKeepAliveSeconds(((Number) keepAliveObj).intValue());
        }
        
        Object queueCapacityObj = threadPoolMap.get("queue-capacity");
        if (queueCapacityObj instanceof Number) {
            threadPoolConfig.setQueueCapacity(((Number) queueCapacityObj).intValue());
        }
        
        Object threadNamePrefixObj = threadPoolMap.get("thread-name-prefix");
        if (threadNamePrefixObj instanceof String) {
            threadPoolConfig.setThreadNamePrefix((String) threadNamePrefixObj);
        }
        
        log.info("解析线程池配置: coreSize={}, maxSize={}, keepAliveSeconds={}, queueCapacity={}, threadNamePrefix={}",
                threadPoolConfig.getCoreSize(), threadPoolConfig.getMaxSize(), 
                threadPoolConfig.getKeepAliveSeconds(), threadPoolConfig.getQueueCapacity(),
                threadPoolConfig.getThreadNamePrefix());
        
        return threadPoolConfig;
    }
    
    /**
     * 创建线程池执行器
     */
    public ThreadPoolExecutor createThreadPoolExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreSize,
                maxSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName(threadNamePrefix + thread.getId());
                    thread.setDaemon(false);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者运行
        );
        
        log.info("创建线程池执行器: coreSize={}, maxSize={}, keepAliveSeconds={}, queueCapacity={}",
                coreSize, maxSize, keepAliveSeconds, queueCapacity);
        
        return executor;
    }
}