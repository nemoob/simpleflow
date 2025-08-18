package com.simpleflow.expression.api;

import java.util.Map;

/**
 * 表达式引擎接口
 * 
 * 提供表达式解析和执行功能
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public interface ExpressionEngine {

    /**
     * 评估表达式
     * 
     * @param expression 表达式字符串
     * @param context 上下文变量
     * @return 评估结果
     * @throws ExpressionException 表达式异常
     */
    Object evaluate(String expression, Map<String, Object> context) throws ExpressionException;

    /**
     * 评估表达式并返回指定类型
     * 
     * @param expression 表达式字符串
     * @param context 上下文变量
     * @param expectedType 期望的返回类型
     * @param <T> 返回类型
     * @return 评估结果
     * @throws ExpressionException 表达式异常
     */
    <T> T evaluate(String expression, Map<String, Object> context, Class<T> expectedType) throws ExpressionException;

    /**
     * 评估布尔表达式
     * 
     * @param expression 表达式字符串
     * @param context 上下文变量
     * @return 布尔结果
     * @throws ExpressionException 表达式异常
     */
    boolean evaluateBoolean(String expression, Map<String, Object> context) throws ExpressionException;

    /**
     * 评估字符串表达式
     * 
     * @param expression 表达式字符串
     * @param context 上下文变量
     * @return 字符串结果
     * @throws ExpressionException 表达式异常
     */
    String evaluateString(String expression, Map<String, Object> context) throws ExpressionException;

    /**
     * 评估数值表达式
     * 
     * @param expression 表达式字符串
     * @param context 上下文变量
     * @return 数值结果
     * @throws ExpressionException 表达式异常
     */
    Number evaluateNumber(String expression, Map<String, Object> context) throws ExpressionException;

    /**
     * 验证表达式语法
     * 
     * @param expression 表达式字符串
     * @return 是否有效
     */
    boolean isValidExpression(String expression);

    /**
     * 解析表达式（预编译）
     * 
     * @param expression 表达式字符串
     * @return 解析后的表达式对象
     * @throws ExpressionException 表达式异常
     */
    Expression parseExpression(String expression) throws ExpressionException;

    /**
     * 获取表达式引擎名称
     * 
     * @return 引擎名称
     */
    String getEngineName();

    /**
     * 获取表达式引擎版本
     * 
     * @return 引擎版本
     */
    String getEngineVersion();

    /**
     * 获取支持的表达式语法
     * 
     * @return 语法描述
     */
    String getSupportedSyntax();
}