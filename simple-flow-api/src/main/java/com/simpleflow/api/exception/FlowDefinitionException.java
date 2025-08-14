package com.simpleflow.api.exception;

/**
 * 流程定义异常
 * 
 * 当流程定义存在问题时抛出此异常，如循环依赖、无效配置等
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public class FlowDefinitionException extends FlowException {

    private final String definitionField;
    private final Object invalidValue;

    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public FlowDefinitionException(String message) {
        super(message);
        this.definitionField = null;
        this.invalidValue = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public FlowDefinitionException(String message, Throwable cause) {
        super(message, cause);
        this.definitionField = null;
        this.invalidValue = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     */
    public FlowDefinitionException(String message, String flowId) {
        super(message, flowId);
        this.definitionField = null;
        this.invalidValue = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param cause 原因异常
     */
    public FlowDefinitionException(String message, String flowId, Throwable cause) {
        super(message, flowId, cause);
        this.definitionField = null;
        this.invalidValue = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param definitionField 定义字段名
     */
    public FlowDefinitionException(String message, String flowId, String definitionField) {
        super(message, flowId);
        this.definitionField = definitionField;
        this.invalidValue = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param definitionField 定义字段名
     * @param invalidValue 无效值
     */
    public FlowDefinitionException(String message, String flowId, String definitionField, Object invalidValue) {
        super(message, flowId);
        this.definitionField = definitionField;
        this.invalidValue = invalidValue;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param flowId 流程ID
     * @param definitionField 定义字段名
     * @param invalidValue 无效值
     * @param cause 原因异常
     */
    public FlowDefinitionException(String message, String flowId, String definitionField, Object invalidValue, Throwable cause) {
        super(message, flowId, cause);
        this.definitionField = definitionField;
        this.invalidValue = invalidValue;
    }

    /**
     * 获取定义字段名
     * 
     * @return 定义字段名
     */
    public String getDefinitionField() {
        return definitionField;
    }

    /**
     * 获取无效值
     * 
     * @return 无效值
     */
    public Object getInvalidValue() {
        return invalidValue;
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getDetailedMessage());
        
        if (definitionField != null) {
            sb.append(" [Field: ").append(definitionField).append("]");
        }
        
        if (invalidValue != null) {
            sb.append(" [InvalidValue: ").append(invalidValue).append("]");
        }
        
        return sb.toString();
    }
}