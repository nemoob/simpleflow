package io.github.nemoob.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 步骤定义模型
 * 
 * 定义工作流程中的单个步骤
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Data
@Builder
public class StepDefinition {

    /**
     * 步骤类型枚举
     */
    public enum StepType {
        SIMPLE,      // 简单步骤
        CONDITIONAL, // 条件步骤
        PARALLEL,    // 并行步骤
        LOOP,        // 循环步骤
        SCRIPT,      // 脚本步骤
        SCRIPT_CONDITIONAL, // 脚本条件步骤
        SERVICE,     // 服务调用步骤
        HUMAN_TASK,  // 人工任务步骤
        TIMER,       // 定时器步骤
        GATEWAY      // 网关步骤
    }

    private String id;
    private String name;
    private String description;
    private StepType type;
    private String executorClass;
    private Map<String, Object> parameters;
    private Map<String, Object> properties;
    private boolean retryEnabled;
    private int maxRetries;
    private long retryDelayMs;
    private long timeoutMs;
    private boolean skipOnError;
    private String condition;
    private List<StepDefinition> subSteps;
    private Map<String, String> inputMappings;
    private Map<String, String> outputMappings;
    private ParallelConfig parallelConfig;

    /**
     * 获取指定参数值
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getParameter(String key) {
        return Optional.ofNullable((T) parameters.get(key));
    }

    /**
     * 获取指定参数值，如果不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        return (T) parameters.getOrDefault(key, defaultValue);
    }

    /**
     * 获取指定属性值
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key) {
        return Optional.ofNullable((T) properties.get(key));
    }

    /**
     * 获取指定属性值，如果不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }

    /**
     * 检查是否有条件
     */
    public boolean hasCondition() {
        return condition != null && !condition.trim().isEmpty();
    }

    /**
     * 检查是否有子步骤
     */
    public boolean hasSubSteps() {
        return !subSteps.isEmpty();
    }

    /**
     * 检查是否为并行步骤
     */
    public boolean isParallel() {
        return type == StepType.PARALLEL;
    }

    /**
     * 检查是否为条件步骤
     */
    public boolean isConditional() {
        return type == StepType.CONDITIONAL || hasCondition();
    }

    /**
     * 检查是否为循环步骤
     */
    public boolean isLoop() {
        return type == StepType.LOOP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StepDefinition that = (StepDefinition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StepDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", executorClass='" + executorClass + '\'' +
                '}';
    }


}