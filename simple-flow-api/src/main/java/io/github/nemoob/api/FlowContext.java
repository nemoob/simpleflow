package io.github.nemoob.api;

import io.github.nemoob.api.model.StepResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 流程执行上下文接口
 *
 * 管理流程执行过程中的数据和状态
 *
 * @author Simple Flow Team
 * @since 1.0.0
 */
public interface FlowContext {

    /**
     * 获取执行ID
     *
     * @return 执行ID
     */
    String getExecutionId();

    /**
     * 获取流程ID
     *
     * @return 流程ID
     */
    String getFlowId();

    /**
     * 获取流程名称
     *
     * @return 流程名称
     */
    String getFlowName();

    /**
     * 获取执行开始时间
     *
     * @return 开始时间
     */
    LocalDateTime getStartTime();

    /**
     * 获取执行结束时间
     *
     * @return 结束时间，如果未结束则返回null
     */
    LocalDateTime getEndTime();

    /**
     * 获取执行状态
     *
     * @return 执行状态
     */
    String getStatus();

    /**
     * 设置执行状态
     *
     * @param status 执行状态
     */
    void setStatus(String status);

    /**
     * 获取上下文变量
     *
     * @param <T> 返回值类型
     * @param key 变量键
     * @return 变量值
     */
    <T> Optional<T> get(String key);

    /**
     * 获取上下文变量，如果不存在则返回默认值
     *
     * @param <T> 返回值类型
     * @param key 变量键
     * @param defaultValue 默认值
     * @return 变量值或默认值
     */
    <T> T get(String key, T defaultValue);

    /**
     * 设置上下文变量
     *
     * @param key 变量键
     * @param value 变量值
     */
    void set(String key, Object value);

    /**
     * 移除上下文变量
     *
     * @param <T> 返回值类型
     * @param key 变量键
     * @return 被移除的值
     */
    <T> Optional<T> remove(String key);

    /**
     * 检查是否包含指定键
     *
     * @param key 变量键
     * @return 是否包含
     */
    boolean contains(String key);

    /**
     * 获取所有上下文变量
     *
     * @return 变量映射的副本
     */
    Map<String, Object> getAll();

    /**
     * 批量设置上下文变量
     *
     * @param variables 变量映射
     */
    void setAll(Map<String, Object> variables);

    /**
     * 清空所有上下文变量
     */
    void clear();

    /**
     * 获取步骤执行结果
     *
     * @param stepId 步骤ID
     * @return 步骤结果
     */
    Optional<StepResult> getStepResult(String stepId);

    /**
     * 设置步骤执行结果
     *
     * @param stepId 步骤ID
     * @param result 步骤结果
     */
    void setStepResult(String stepId, StepResult result);

    /**
     * 获取所有步骤执行结果
     *
     * @return 步骤结果映射
     */
    Map<String, StepResult> getAllStepResults();

    /**
     * 获取当前执行的步骤ID
     *
     * @return 当前步骤ID
     */
    Optional<String> getCurrentStepId();

    /**
     * 设置当前执行的步骤ID
     *
     * @param stepId 步骤ID
     */
    void setCurrentStepId(String stepId);

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    Optional<Exception> getError();

    /**
     * 设置错误信息
     *
     * @param error 错误信息
     */
    void setError(Exception error);

    /**
     * 检查是否有错误
     *
     * @return 是否有错误
     */
    boolean hasError();

    /**
     * 获取重试次数
     *
     * @return 重试次数
     */
    int getRetryCount();

    /**
     * 增加重试次数
     */
    void incrementRetryCount();

    /**
     * 重置重试次数
     */
    void resetRetryCount();

    /**
     * 检查是否被取消
     *
     * @return 是否被取消
     */
    boolean isCancelled();

    /**
     * 取消执行
     */
    void cancel();

    /**
     * 检查是否暂停
     *
     * @return 是否暂停
     */
    boolean isPaused();

    /**
     * 暂停执行
     */
    void pause();

    /**
     * 恢复执行
     */
    void resume();

    /**
     * 创建子上下文
     *
     * @param stepId 步骤ID
     * @return 子上下文
     */
    FlowContext createChildContext(String stepId);

    /**
     * 获取父上下文
     *
     * @return 父上下文
     */
    Optional<FlowContext> getParentContext();

    /**
     * 复制上下文
     *
     * @return 上下文副本
     */
    FlowContext copy();

    /**
     * 获取当前步骤定义
     *
     * @return 当前步骤定义
     */
    default Optional<io.github.nemoob.api.model.StepDefinition> getCurrentStepDefinition() {
        return get("currentStepDefinition");
    }

    /**
     * 设置当前步骤定义
     *
     * @param stepDefinition 步骤定义
     */
    default void setCurrentStepDefinition(io.github.nemoob.api.model.StepDefinition stepDefinition) {
        set("currentStepDefinition", stepDefinition);
    }

    /**
     * 获取所有变量（别名方法）
     *
     * @return 变量映射
     */
    default Map<String, Object> getVariables() {
        return getAll();
    }

    /**
     * 获取变量（别名方法）
     *
     * @param <T> 返回值类型
     * @param key 变量键
     * @return 变量值
     */
    default <T> Optional<T> getVariable(String key) {
        return get(key);
    }
}