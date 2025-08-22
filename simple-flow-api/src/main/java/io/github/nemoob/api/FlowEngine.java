package io.github.nemoob.api;

import io.github.nemoob.api.model.FlowDefinition;
import io.github.nemoob.api.model.FlowResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 流程引擎接口
 * 
 * 提供流程执行的核心功能，包括同步和异步执行模式
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public interface FlowEngine {

    /**
     * 同步执行流程
     * 
     * @param flowDefinition 流程定义
     * @param context 执行上下文参数
     * @return 流程执行结果
     * @throws io.github.nemoob.api.exception.FlowException 流程执行异常
     */
    FlowResult execute(FlowDefinition flowDefinition, Map<String, Object> context);

    /**
     * 异步执行流程
     * 
     * @param flowDefinition 流程定义
     * @param context 执行上下文参数
     * @return 流程执行结果的Future
     */
    CompletableFuture<FlowResult> executeAsync(FlowDefinition flowDefinition, Map<String, Object> context);

    /**
     * 根据流程ID执行流程
     * 
     * @param flowId 流程ID
     * @param context 执行上下文参数
     * @return 流程执行结果
     * @throws io.github.nemoob.api.exception.FlowException 流程执行异常
     */
    FlowResult execute(String flowId, Map<String, Object> context);

    /**
     * 异步根据流程ID执行流程
     * 
     * @param flowId 流程ID
     * @param context 执行上下文参数
     * @return 流程执行结果的Future
     */
    CompletableFuture<FlowResult> executeAsync(String flowId, Map<String, Object> context);

    /**
     * 停止正在执行的流程
     * 
     * @param executionId 执行ID
     * @return 是否成功停止
     */
    boolean stopExecution(String executionId);

    /**
     * 暂停正在执行的流程
     * 
     * @param executionId 执行ID
     * @return 是否成功暂停
     */
    boolean pauseExecution(String executionId);

    /**
     * 恢复暂停的流程
     * 
     * @param executionId 执行ID
     * @return 是否成功恢复
     */
    boolean resumeExecution(String executionId);

    /**
     * 获取流程执行状态
     * 
     * @param executionId 执行ID
     * @return 执行状态
     */
    String getExecutionStatus(String executionId);

    /**
     * 注册流程定义
     * 
     * @param flowDefinition 流程定义
     * @return 流程ID
     */
    String registerFlow(FlowDefinition flowDefinition);

    /**
     * 注销流程定义
     * 
     * @param flowId 流程ID
     * @return 是否成功注销
     */
    boolean unregisterFlow(String flowId);

    /**
     * 获取流程定义
     * 
     * @param flowId 流程ID
     * @return 流程定义
     */
    FlowDefinition getFlowDefinition(String flowId);

    /**
     * 检查引擎是否健康
     * 
     * @return 健康状态
     */
    boolean isHealthy();

    /**
     * 关闭引擎
     */
    void shutdown();
}