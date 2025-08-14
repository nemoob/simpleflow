package com.simpleflow.api.exception;

/**
 * 流程执行异常基类
 * 
 * 所有流程相关异常的基类
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public class FlowException extends RuntimeException {

    private final String flowId;
    private final String executionId;
    private final String errorCode;

    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public FlowException(String message) {
        super(message);
        this.flowId = null;
        this.executionId = null;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public FlowException(String message, Throwable cause) {
        super(message, cause);
        this.flowId = null;
        this.executionId = null;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     */
    public FlowException(String message, String flowId) {
        super(message);
        this.flowId = flowId;
        this.executionId = null;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param cause 原因异常
     */
    public FlowException(String message, String flowId, Throwable cause) {
        super(message, cause);
        this.flowId = flowId;
        this.executionId = null;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     */
    public FlowException(String message, String flowId, String executionId) {
        super(message);
        this.flowId = flowId;
        this.executionId = executionId;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param cause 原因异常
     */
    public FlowException(String message, String flowId, String executionId, Throwable cause) {
        super(message, cause);
        this.flowId = flowId;
        this.executionId = executionId;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param errorCode 错误代码
     */
    public FlowException(String message, String flowId, String executionId, String errorCode) {
        super(message);
        this.flowId = flowId;
        this.executionId = executionId;
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param executionId 执行ID
     * @param errorCode 错误代码
     * @param cause 原因异常
     */
    public FlowException(String message, String flowId, String executionId, String errorCode, Throwable cause) {
        super(message, cause);
        this.flowId = flowId;
        this.executionId = executionId;
        this.errorCode = errorCode;
    }

    /**
     * 获取流程ID
     * 
     * @return 流程ID
     */
    public String getFlowId() {
        return flowId;
    }

    /**
     * 获取执行ID
     * 
     * @return 执行ID
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * 获取错误代码
     * 
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取详细错误信息
     * 
     * @return 详细错误信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        
        if (flowId != null) {
            sb.append(" [FlowId: ").append(flowId).append("]");
        }
        
        if (executionId != null) {
            sb.append(" [ExecutionId: ").append(executionId).append("]");
        }
        
        if (errorCode != null) {
            sb.append(" [ErrorCode: ").append(errorCode).append("]");
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getDetailedMessage();
    }
}