package com.simpleflow.api;

import com.simpleflow.api.model.FlowDefinition;
import com.simpleflow.api.model.StepDefinition;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 流程构建器接口
 * 
 * 提供流畅的API来构建流程定义
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public interface FlowBuilder {

    /**
     * 设置流程ID
     * 
     * @param flowId 流程ID
     * @return 流程构建器
     */
    FlowBuilder id(String flowId);

    /**
     * 设置流程名称
     * 
     * @param name 流程名称
     * @return 流程构建器
     */
    FlowBuilder name(String name);

    /**
     * 设置流程描述
     * 
     * @param description 流程描述
     * @return 流程构建器
     */
    FlowBuilder description(String description);

    /**
     * 设置流程版本
     * 
     * @param version 版本号
     * @return 流程构建器
     */
    FlowBuilder version(String version);

    /**
     * 添加步骤
     * 
     * @param stepDefinition 步骤定义
     * @return 流程构建器
     */
    FlowBuilder addStep(StepDefinition stepDefinition);

    /**
     * 添加简单步骤
     * 
     * @param stepId 步骤ID
     * @param stepName 步骤名称
     * @param executor 执行器
     * @return 流程构建器
     */
    FlowBuilder addStep(String stepId, String stepName, StepExecutor executor);

    /**
     * 添加条件步骤
     * 
     * @param stepId 步骤ID
     * @param stepName 步骤名称
     * @param condition 条件判断
     * @param trueExecutor 条件为真时的执行器
     * @param falseExecutor 条件为假时的执行器
     * @return 流程构建器
     */
    FlowBuilder addConditionalStep(String stepId, String stepName, 
                                   Predicate<FlowContext> condition,
                                   StepExecutor trueExecutor, 
                                   StepExecutor falseExecutor);

    /**
     * 添加并行步骤组
     * 
     * @param groupId 组ID
     * @param groupName 组名称
     * @param steps 并行执行的步骤
     * @return 流程构建器
     */
    FlowBuilder addParallelSteps(String groupId, String groupName, StepDefinition... steps);

    /**
     * 添加循环步骤
     * 
     * @param stepId 步骤ID
     * @param stepName 步骤名称
     * @param condition 循环条件
     * @param executor 循环体执行器
     * @return 流程构建器
     */
    FlowBuilder addLoopStep(String stepId, String stepName, 
                           Predicate<FlowContext> condition, 
                           StepExecutor executor);

    /**
     * 设置步骤依赖关系
     * 
     * @param fromStepId 前置步骤ID
     * @param toStepId 后置步骤ID
     * @return 流程构建器
     */
    FlowBuilder addDependency(String fromStepId, String toStepId);

    /**
     * 设置条件依赖关系
     * 
     * @param fromStepId 前置步骤ID
     * @param toStepId 后置步骤ID
     * @param condition 依赖条件
     * @return 流程构建器
     */
    FlowBuilder addConditionalDependency(String fromStepId, String toStepId, 
                                        Predicate<FlowContext> condition);

    /**
     * 设置全局错误处理器
     * 
     * @param errorHandler 错误处理器
     * @return 流程构建器
     */
    FlowBuilder onError(Function<Exception, FlowContext> errorHandler);

    /**
     * 设置流程完成回调
     * 
     * @param callback 完成回调
     * @return 流程构建器
     */
    FlowBuilder onComplete(Function<FlowContext, Void> callback);

    /**
     * 设置流程属性
     * 
     * @param properties 属性映射
     * @return 流程构建器
     */
    FlowBuilder properties(Map<String, Object> properties);

    /**
     * 设置单个属性
     * 
     * @param key 属性键
     * @param value 属性值
     * @return 流程构建器
     */
    FlowBuilder property(String key, Object value);

    /**
     * 启用并行执行
     * 
     * @return 流程构建器
     */
    FlowBuilder enableParallel();

    /**
     * 设置超时时间（毫秒）
     * 
     * @param timeoutMs 超时时间
     * @return 流程构建器
     */
    FlowBuilder timeout(long timeoutMs);

    /**
     * 启用重试机制
     * 
     * @param maxRetries 最大重试次数
     * @param retryDelayMs 重试延迟时间
     * @return 流程构建器
     */
    FlowBuilder enableRetry(int maxRetries, long retryDelayMs);

    /**
     * 验证流程定义
     * 
     * @return 验证结果
     * @throws com.simpleflow.api.exception.ValidationException 验证失败异常
     */
    boolean validate();

    /**
     * 构建流程定义
     * 
     * @return 流程定义
     * @throws com.simpleflow.api.exception.ValidationException 验证失败异常
     */
    FlowDefinition build();
}