package com.simpleflow.api.exception;

/**
 * 验证异常
 *
 * 当流程定义、步骤配置或其他组件验证失败时抛出此异常
 *
 * @author Simple Flow Team
 * @since 1.0.0
 */
public class ValidationException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造验证异常
     *
     * @param message 错误消息
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * 构造验证异常
     *
     * @param message 错误消息
     * @param cause 原因异常
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造验证异常
     *
     * @param cause 原因异常
     */
    public ValidationException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
