package io.github.nemoob.api.exception;

/**
 * 步骤执行异常
 * 
 * 当单个步骤执行过程中发生错误时抛出此异常
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public class StepExecutionException extends FlowExecutionException {

    private final String stepName;
    private final String executorClass;
    private final long executionTime;

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param stepId 步骤ID
     */
    public StepExecutionException(String message, String stepId) {
        super(message, null, null, stepId);
        this.stepName = null;
        this.executorClass = null;
        this.executionTime = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param stepId 步骤ID
     * @param cause 原因异常
     */
    public StepExecutionException(String message, String stepId, Throwable cause) {
        super(message, null, null, stepId, cause);
        this.stepName = null;
        this.executorClass = null;
        this.executionTime = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     */
    public StepExecutionException(String message, String flowId, String executionId, String stepId) {
        super(message, flowId, executionId, stepId);
        this.stepName = null;
        this.executorClass = null;
        this.executionTime = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param cause 原因异常
     */
    public StepExecutionException(String message, String flowId, String executionId, String stepId, Throwable cause) {
        super(message, flowId, executionId, stepId, cause);
        this.stepName = null;
        this.executorClass = null;
        this.executionTime = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param stepName 步骤名称
     */
    public StepExecutionException(String message, String flowId, String executionId, String stepId, String stepName) {
        super(message, flowId, executionId, stepId);
        this.stepName = stepName;
        this.executorClass = null;
        this.executionTime = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param stepName 步骤名称
     * @param executorClass 执行器类名
     */
    public StepExecutionException(String message, String flowId, String executionId, String stepId, String stepName, String executorClass) {
        super(message, flowId, executionId, stepId);
        this.stepName = stepName;
        this.executorClass = executorClass;
        this.executionTime = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param stepName 步骤名称
     * @param executorClass 执行器类名
     * @param executionTime 执行时间（毫秒）
     */
    public StepExecutionException(String message, String flowId, String executionId, String stepId, String stepName, String executorClass, long executionTime) {
        super(message, flowId, executionId, stepId);
        this.stepName = stepName;
        this.executorClass = executorClass;
        this.executionTime = executionTime;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param stepName 步骤名称
     * @param executorClass 执行器类名
     * @param executionTime 执行时间（毫秒）
     * @param cause 原因异常
     */
    public StepExecutionException(String message, String flowId, String executionId, String stepId, String stepName, String executorClass, long executionTime, Throwable cause) {
        super(message, flowId, executionId, stepId, cause);
        this.stepName = stepName;
        this.executorClass = executorClass;
        this.executionTime = executionTime;
    }

    /**
     * 获取步骤名称
     * 
     * @return 步骤名称
     */
    public String getStepName() {
        return stepName;
    }

    /**
     * 获取执行器类名
     * 
     * @return 执行器类名
     */
    public String getExecutorClass() {
        return executorClass;
    }

    /**
     * 获取执行时间
     * 
     * @return 执行时间（毫秒）
     */
    public long getExecutionTime() {
        return executionTime;
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getDetailedMessage());
        
        if (stepName != null) {
            sb.append(" [StepName: ").append(stepName).append("]");
        }
        
        if (executorClass != null) {
            sb.append(" [ExecutorClass: ").append(executorClass).append("]");
        }
        
        if (executionTime > 0) {
            sb.append(" [ExecutionTime: ").append(executionTime).append("ms]");
        }
        
        return sb.toString();
    }
}