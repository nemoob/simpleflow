package com.simpleflow.api.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 流程执行结果模型
 *
 * 包含流程执行的完整结果信息
 *
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Data
@Builder
public class FlowResult {

    /**
     * 执行状态枚举
     */
    public enum Status {
        SUCCESS,    // 成功
        FAILED,     // 失败
        CANCELLED,  // 取消
        TIMEOUT,    // 超时
        PARTIAL     // 部分成功
    }

    private String executionId;
    private String flowId;
    private String flowName;
    private Status status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;
    private Map<String, StepResult> stepResults;
    private Map<String, Object> outputData;
    private Exception error;
    private String errorMessage;
    private List<String> warnings;
    private Map<String, Object> metadata;
    private int totalSteps;
    private int successfulSteps;
    private int failedSteps;
    private int skippedSteps;

    /**
     * 获取指定步骤的结果
     *
     * @param stepId 步骤ID
     * @return 步骤结果，如果不存在则返回Optional.empty()
     */
    public Optional<StepResult> getStepResult(String stepId) {
        return Optional.ofNullable(stepResults.get(stepId));
    }

    /**
     * 获取指定输出数据
     *
     * @param <T> 返回值类型
     * @param key 数据键
     * @return 输出数据，如果不存在则返回Optional.empty()
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOutputData(String key) {
        return Optional.ofNullable((T) outputData.get(key));
    }

    /**
     * 获取指定输出数据，如果不存在则返回默认值
     *
     * @param <T> 返回值类型
     * @param key 数据键
     * @param defaultValue 默认值
     * @return 输出数据或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutputData(String key, T defaultValue) {
        return (T) outputData.getOrDefault(key, defaultValue);
    }

    /**
     * 获取指定元数据
     *
     * @param <T> 返回值类型
     * @param key 元数据键
     * @return 元数据，如果不存在则返回Optional.empty()
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key) {
        return Optional.ofNullable((T) metadata.get(key));
    }

    /**
     * 检查是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * 检查是否失败
     *
     * @return 是否失败
     */
    public boolean isFailed() {
        return status == Status.FAILED;
    }

    /**
     * 检查是否被取消
     *
     * @return 是否被取消
     */
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    /**
     * 检查是否超时
     */
    public boolean isTimeout() {
        return status == Status.TIMEOUT;
    }

    /**
     * 检查是否部分成功
     */
    public boolean isPartial() {
        return status == Status.PARTIAL;
    }

    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        return error != null || errorMessage != null;
    }

    /**
     * 检查是否有警告
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalSteps == 0) {
            return 0.0;
        }
        return (double) successfulSteps / totalSteps;
    }

    /**
     * 获取失败的步骤结果
     */
    public List<StepResult> getFailedStepResults() {
        return stepResults.values().stream()
                .filter(result -> result.getStatus() == StepResult.Status.FAILED)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 获取成功的步骤结果
     */
    public List<StepResult> getSuccessfulStepResults() {
        return stepResults.values().stream()
                .filter(result -> result.getStatus() == StepResult.Status.SUCCESS)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowResult that = (FlowResult) o;
        return Objects.equals(executionId, that.executionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId);
    }

    @Override
    public String toString() {
        return "FlowResult{" +
                "executionId='" + executionId + '\'' +
                ", flowId='" + flowId + '\'' +
                ", status=" + status +
                ", durationMs=" + durationMs +
                ", totalSteps=" + totalSteps +
                ", successfulSteps=" + successfulSteps +
                ", failedSteps=" + failedSteps +
                '}';
    }


}