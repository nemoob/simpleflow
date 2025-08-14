package com.simpleflow.api;

import java.util.Map;

/**
 * 条件节点基类
 * 
 * 所有条件判断节点都应该继承此类，提供统一的条件评估接口
 * 使用Map<String, Object>作为上下文参数，返回boolean结果
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public abstract class ConditionNode {
    
    /**
     * 评估条件
     * 
     * @param context 执行上下文，包含所有必要的参数和数据
     * @return 条件评估结果，true表示条件满足，false表示条件不满足
     */
    public abstract boolean evaluate(Map<String, Object> context);
    
    /**
     * 获取条件节点名称
     * 
     * @return 节点名称
     */
    public String getName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 获取条件节点描述
     * 
     * @return 节点描述
     */
    public String getDescription() {
        return "Condition node: " + getName();
    }
    
    /**
     * 获取条件表达式描述
     * 
     * @return 条件表达式的文本描述
     */
    public String getConditionExpression() {
        return "Custom condition in " + getName();
    }
    
    /**
     * 条件评估前的准备工作
     * 
     * @param context 执行上下文
     */
    public void prepare(Map<String, Object> context) {
        // 默认实现为空，子类可以重写
    }
    
    /**
     * 验证条件节点的输入参数
     * 
     * @param context 执行上下文
     * @return 是否满足评估条件
     */
    public boolean validate(Map<String, Object> context) {
        // 默认返回true，子类可以重写
        return true;
    }
}