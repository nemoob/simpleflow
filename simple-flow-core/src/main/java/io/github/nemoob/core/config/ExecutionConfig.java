package io.github.nemoob.core.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 执行配置类
 * 用于解析和管理全局执行参数
 */
@Data
@Slf4j
public class ExecutionConfig {
    
    private long defaultTimeoutMs = 30000L;      // 默认超时时间（毫秒）
    private int defaultRetryCount = 3;           // 默认重试次数
    private long defaultRetryDelayMs = 1000L;    // 默认重试延迟（毫秒）
    private boolean parallelEnabled = true;      // 是否启用并行执行
    private int maxParallelism = 4;              // 最大并行度
    
    /**
     * 从配置Map中解析执行配置
     */
    @SuppressWarnings("unchecked")
    public static ExecutionConfig parseFromConfig(Map<String, Object> config) {
        ExecutionConfig executionConfig = new ExecutionConfig();
        
        if (config == null) {
            log.info("未找到执行配置，使用默认配置");
            return executionConfig;
        }
        
        Map<String, Object> executionMap = (Map<String, Object>) config.get("execution");
        if (executionMap == null) {
            log.info("未找到execution配置，使用默认配置");
            return executionConfig;
        }
        
        // 解析各个配置项
        Object defaultTimeoutObj = executionMap.get("default-timeout-ms");
        if (defaultTimeoutObj instanceof Number) {
            executionConfig.setDefaultTimeoutMs(((Number) defaultTimeoutObj).longValue());
        }
        
        Object defaultRetryCountObj = executionMap.get("default-retry-count");
        if (defaultRetryCountObj instanceof Number) {
            executionConfig.setDefaultRetryCount(((Number) defaultRetryCountObj).intValue());
        }
        
        Object defaultRetryDelayObj = executionMap.get("default-retry-delay-ms");
        if (defaultRetryDelayObj instanceof Number) {
            executionConfig.setDefaultRetryDelayMs(((Number) defaultRetryDelayObj).longValue());
        }
        
        Object parallelEnabledObj = executionMap.get("parallel-enabled");
        if (parallelEnabledObj instanceof Boolean) {
            executionConfig.setParallelEnabled((Boolean) parallelEnabledObj);
        }
        
        Object maxParallelismObj = executionMap.get("max-parallelism");
        if (maxParallelismObj instanceof Number) {
            executionConfig.setMaxParallelism(((Number) maxParallelismObj).intValue());
        }
        
        log.info("解析执行配置: defaultTimeoutMs={}, defaultRetryCount={}, defaultRetryDelayMs={}, parallelEnabled={}, maxParallelism={}",
                executionConfig.getDefaultTimeoutMs(), executionConfig.getDefaultRetryCount(),
                executionConfig.getDefaultRetryDelayMs(), executionConfig.isParallelEnabled(),
                executionConfig.getMaxParallelism());
        
        return executionConfig;
    }
}