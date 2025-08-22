package io.github.nemoob.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并行执行配置
 * 
 * 用于配置步骤的并行执行策略和线程池参数
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Data
@Builder
public class ParallelConfig {
    
    /**
     * 执行模式
     */
    public enum ExecutionMode {
        SEQUENTIAL,  // 串行执行
        PARALLEL     // 并行执行
    }
    
    @Builder.Default
    private ExecutionMode mode = ExecutionMode.SEQUENTIAL;
    @Builder.Default
    private int corePoolSize = Runtime.getRuntime().availableProcessors();
    @Builder.Default
    private int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    @Builder.Default
    private long keepAliveTime = 60L;
    @Builder.Default
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    @Builder.Default
    private int queueCapacity = 100;
    @Builder.Default
    private String threadNamePrefix = "simple-flow-";
    @Builder.Default
    private boolean allowCoreThreadTimeOut = false;
    @Builder.Default
    private long awaitTerminationSeconds = 60;
    

    

    
    /**
     * 创建线程池执行器
     * 
     * @return ThreadPoolExecutor实例
     */
    public ThreadPoolExecutor createThreadPoolExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            timeUnit,
            new LinkedBlockingQueue<>(queueCapacity),
            new SimpleFlowThreadFactory(threadNamePrefix)
        );
        
        executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        return executor;
    }
    
    /**
     * 创建默认的串行配置
     */
    public static ParallelConfig sequential() {
        return builder().mode(ExecutionMode.SEQUENTIAL).build();
    }
    
    /**
     * 创建默认的并行配置
     */
    public static ParallelConfig parallel() {
        return builder().mode(ExecutionMode.PARALLEL).build();
    }
    

    
    /**
     * 自定义线程工厂
     */
    private static class SimpleFlowThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        SimpleFlowThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}