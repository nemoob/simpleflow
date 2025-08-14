package com.simpleflow.api.exception;

/**
 * 流程执行异常
 * 
 * 当流程执行过程中发生错误时抛出此异常
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public class FlowExecutionException extends FlowException {

    private final String stepId;
    private final int retryCount;

    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public FlowExecutionException(String message) {
        super(message);
        this.stepId = null;
        this.retryCount = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public FlowExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.stepId = null;
        this.retryCount = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     */
    public FlowExecutionException(String message, String flowId, String executionId) {
        super(message, flowId, executionId);
        this.stepId = null;
        this.retryCount = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param cause 原因异常
     */
    public FlowExecutionException(String message, String flowId, String executionId, Throwable cause) {
        super(message, flowId, executionId, cause);
        this.stepId = null;
        this.retryCount = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     */
    public FlowExecutionException(String message, String flowId, String executionId, String stepId) {
        super(message, flowId, executionId);
        this.stepId = stepId;
        this.retryCount = 0;
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
    public FlowExecutionException(String message, String flowId, String executionId, String stepId, Throwable cause) {
        super(message, flowId, executionId, cause);
        this.stepId = stepId;
        this.retryCount = 0;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param retryCount 重试次数
     */
    public FlowExecutionException(String message, String flowId, String executionId, String stepId, int retryCount) {
        super(message, flowId, executionId);
        this.stepId = stepId;
        this.retryCount = retryCount;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param retryCount 重试次数
     * @param cause 原因异常
     */
    public FlowExecutionException(String message, String flowId, String executionId, String stepId, int retryCount, Throwable cause) {
        super(message, flowId, executionId, cause);
        this.stepId = stepId;
        this.retryCount = retryCount;
    }

    /**
     * 获取步骤ID
     * 
     * @return 步骤ID
     */
    public String getStepId() {
        return stepId;
    }

    /**
     * 获取重试次数
     * 
     * @return 重试次数
     */
    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getDetailedMessage());
        
        if (stepId != null) {
            sb.append(" [StepId: ").append(stepId).append("]");
        }
        
        if (retryCount > 0) {
            sb.append(" [RetryCount: ").append(retryCount).append("]");
        }
        
        return sb.toString();
    }
}