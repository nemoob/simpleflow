package com.simpleflow.core.engine;

import com.simpleflow.api.FlowEngine;
import com.simpleflow.api.exception.FlowDefinitionException;
import com.simpleflow.api.exception.FlowException;
import com.simpleflow.api.exception.FlowExecutionException;
import com.simpleflow.api.model.FlowDefinition;
import com.simpleflow.api.model.FlowResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认流程引擎实现
 * 
 * 提供基本的流程执行功能
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class DefaultFlowEngine implements FlowEngine {



    private final Map<String, FlowDefinition> flowDefinitions = new ConcurrentHashMap<>();
    private final Map<String, FlowExecution> activeExecutions = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final AtomicLong executionIdGenerator = new AtomicLong(0);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /**
     * 构造函数
     */
    public DefaultFlowEngine() {
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "SimpleFlow-Worker-" + Thread.currentThread().getId());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 构造函数
     * 
     * @param executorService 自定义线程池
     */
    public DefaultFlowEngine(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public FlowResult execute(FlowDefinition flowDefinition, Map<String, Object> input) {
        checkShutdown();
        
        String executionId = generateExecutionId();
        log.info("Starting synchronous execution of flow '{}' with execution ID '{}'", flowDefinition.getId(), executionId);

        try {
            FlowExecution execution = new FlowExecution(executionId, flowDefinition, input);
            activeExecutions.put(executionId, execution);
            
            FlowResult result = execution.execute();
            
            log.info("Completed synchronous execution of flow '{}' with execution ID '{}', status: {}", 
                       flowDefinition.getId(), executionId, result.getStatus());
            
            return result;
        } catch (Exception e) {
            log.error("Failed to execute flow '{}' with execution ID '{}'", flowDefinition.getId(), executionId, e);
            throw new FlowExecutionException("Flow execution failed: " + e.getMessage(), flowDefinition.getId(), executionId, e);
        } finally {
            activeExecutions.remove(executionId);
        }
    }

    @Override
    public FlowResult execute(String flowId, Map<String, Object> input) throws FlowException {
        checkShutdown();
        
        FlowDefinition flowDefinition = getFlowDefinition(flowId);
        if (flowDefinition == null) {
            throw new FlowDefinitionException("Flow definition not found: " + flowId, flowId);
        }

        return execute(flowDefinition, input);
    }

    @Override
    public CompletableFuture<FlowResult> executeAsync(FlowDefinition flowDefinition, Map<String, Object> input) {
        checkShutdown();
        
        String executionId = generateExecutionId();
        log.info("Starting asynchronous execution of flow '{}' with execution ID '{}'", flowDefinition.getId(), executionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                FlowExecution execution = new FlowExecution(executionId, flowDefinition, input);
                activeExecutions.put(executionId, execution);
                
                FlowResult result = execution.execute();
                
                log.info("Completed asynchronous execution of flow '{}' with execution ID '{}', status: {}", 
                           flowDefinition.getId(), executionId, result.getStatus());
                
                return result;
            } catch (Exception e) {
                log.error("Failed to execute flow '{}' with execution ID '{}'", flowDefinition.getId(), executionId, e);
                throw new FlowExecutionException("Flow execution failed: " + e.getMessage(), flowDefinition.getId(), executionId, e);
            } finally {
                activeExecutions.remove(executionId);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<FlowResult> executeAsync(String flowId, Map<String, Object> input) throws FlowException {
        checkShutdown();
        
        FlowDefinition flowDefinition = getFlowDefinition(flowId);
        if (flowDefinition == null) {
            throw new FlowDefinitionException("Flow definition not found: " + flowId, flowId);
        }

        return executeAsync(flowDefinition, input);
    }

    @Override
    public boolean stopExecution(String executionId) throws FlowException {
        checkShutdown();
        
        FlowExecution execution = activeExecutions.get(executionId);
        if (execution == null) {
            log.warn("Execution not found: {}", executionId);
            return false;
        }

        log.info("Stopping execution: {}", executionId);
        boolean stopped = execution.stop();
        
        if (stopped) {
            activeExecutions.remove(executionId);
            log.info("Successfully stopped execution: {}", executionId);
        } else {
            log.warn("Failed to stop execution: {}", executionId);
        }
        
        return stopped;
    }

    @Override
    public boolean pauseExecution(String executionId) throws FlowException {
        checkShutdown();
        
        FlowExecution execution = activeExecutions.get(executionId);
        if (execution == null) {
            log.warn("Execution not found: {}", executionId);
            return false;
        }

        log.info("Pausing execution: {}", executionId);
        return execution.pause();
    }

    @Override
    public boolean resumeExecution(String executionId) throws FlowException {
        checkShutdown();
        
        FlowExecution execution = activeExecutions.get(executionId);
        if (execution == null) {
            log.warn("Execution not found: {}", executionId);
            return false;
        }

        log.info("Resuming execution: {}", executionId);
        return execution.resume();
    }

    @Override
    public String getExecutionStatus(String executionId) throws FlowException {
        FlowExecution execution = activeExecutions.get(executionId);
        if (execution == null) {
            return "NOT_FOUND";
        }
        return execution.getStatus();
    }

    @Override
    public String registerFlow(FlowDefinition flowDefinition) throws FlowException {
        checkShutdown();
        
        if (flowDefinition == null) {
            throw new FlowDefinitionException("Flow definition cannot be null");
        }
        
        if (flowDefinition.getId() == null || flowDefinition.getId().trim().isEmpty()) {
            throw new FlowDefinitionException("Flow ID cannot be null or empty");
        }

        // 验证流程定义
        validateFlowDefinition(flowDefinition);

        flowDefinitions.put(flowDefinition.getId(), flowDefinition);
        log.info("Registered flow definition: {} (version: {})", 
                   flowDefinition.getId(), flowDefinition.getVersion());
        
        return flowDefinition.getId();
    }

    @Override
    public boolean unregisterFlow(String flowId) throws FlowException {
        checkShutdown();
        
        if (flowId == null || flowId.trim().isEmpty()) {
            throw new FlowDefinitionException("Flow ID cannot be null or empty");
        }

        FlowDefinition removed = flowDefinitions.remove(flowId);
        if (removed != null) {
            log.info("Unregistered flow definition: {}", flowId);
            return true;
        } else {
            log.warn("Flow definition not found for unregistration: {}", flowId);
            return false;
        }
    }

    @Override
    public FlowDefinition getFlowDefinition(String flowId) throws FlowException {
        if (flowId == null || flowId.trim().isEmpty()) {
            throw new FlowDefinitionException("Flow ID cannot be null or empty");
        }
        return flowDefinitions.get(flowId);
    }

    @Override
    public boolean isHealthy() {
        return !shutdown.get() && !executorService.isShutdown();
    }

    @Override
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            log.info("Shutting down flow engine...");
            
            // 停止所有活跃的执行
            activeExecutions.values().forEach(execution -> {
                try {
                    execution.stop();
                } catch (Exception e) {
                    log.warn("Error stopping execution during shutdown: {}", execution.getExecutionId(), e);
                }
            });
            
            // 关闭线程池
            executorService.shutdown();
            
            log.info("Flow engine shutdown completed");
        }
    }

    /**
     * 检查引擎是否已关闭
     */
    private void checkShutdown() {
        if (shutdown.get()) {
            throw new FlowException("Flow engine has been shutdown");
        }
    }

    /**
     * 生成执行ID
     * 
     * @return 执行ID
     */
    private String generateExecutionId() {
        return "exec-" + System.currentTimeMillis() + "-" + executionIdGenerator.incrementAndGet();
    }

    /**
     * 验证流程定义
     * 
     * @param flowDefinition 流程定义
     * @throws FlowDefinitionException 验证失败时抛出
     */
    private void validateFlowDefinition(FlowDefinition flowDefinition) throws FlowDefinitionException {
        // 检查循环依赖
        if (flowDefinition.hasCyclicDependency()) {
            throw new FlowDefinitionException("Flow definition contains cyclic dependencies", flowDefinition.getId());
        }
        
        // 检查步骤定义
        if (flowDefinition.getSteps() == null || flowDefinition.getSteps().isEmpty()) {
            throw new FlowDefinitionException("Flow definition must contain at least one step", flowDefinition.getId());
        }
        
        // 可以添加更多验证逻辑
    }

    /**
     * 获取活跃执行数量
     * 
     * @return 活跃执行数量
     */
    public int getActiveExecutionCount() {
        return activeExecutions.size();
    }

    /**
     * 获取已注册流程数量
     * 
     * @return 已注册流程数量
     */
    public int getRegisteredFlowCount() {
        return flowDefinitions.size();
    }
}