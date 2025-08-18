package com.simpleflow.expression.parser;

import com.simpleflow.expression.api.Expression;
import com.simpleflow.expression.api.ExpressionException;

import java.util.Map;

/**
 * 表达式解析器接口
 * 
 * 定义表达式解析的核心功能
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public interface ExpressionParser {

    /**
     * 解析表达式字符串
     * 
     * @param expressionString 表达式字符串
     * @return 解析后的表达式对象
     * @throws ExpressionException 如果解析失败
     */
    Expression parse(String expressionString) throws ExpressionException;

    /**
     * 验证表达式语法
     * 
     * @param expressionString 表达式字符串
     * @return 如果语法有效则返回 true
     */
    boolean validateSyntax(String expressionString);

    /**
     * 获取解析器支持的语法类型
     * 
     * @return 语法类型描述
     */
    String getSupportedSyntax();

    /**
     * 获取解析器名称
     * 
     * @return 解析器名称
     */
    String getParserName();

    /**
     * 获取解析器版本
     * 
     * @return 解析器版本
     */
    String getParserVersion();

    /**
     * 检查解析器是否支持指定的功能
     * 
     * @param feature 功能名称
     * @return 如果支持则返回 true
     */
    boolean supportsFeature(String feature);

    /**
     * 获取解析器配置
     * 
     * @return 配置映射
     */
    Map<String, Object> getConfiguration();

    /**
     * 设置解析器配置
     * 
     * @param configuration 配置映射
     */
    void setConfiguration(Map<String, Object> configuration);

    /**
     * 预编译表达式（可选优化）
     * 
     * @param expressionString 表达式字符串
     * @return 预编译的表达式对象
     * @throws ExpressionException 如果预编译失败
     */
    default Expression precompile(String expressionString) throws ExpressionException {
        return parse(expressionString);
    }

    /**
     * 批量解析表达式
     * 
     * @param expressionStrings 表达式字符串数组
     * @return 解析后的表达式对象数组
     * @throws ExpressionException 如果任何表达式解析失败
     */
    default Expression[] parseBatch(String... expressionStrings) throws ExpressionException {
        if (expressionStrings == null || expressionStrings.length == 0) {
            return new Expression[0];
        }
        
        Expression[] expressions = new Expression[expressionStrings.length];
        for (int i = 0; i < expressionStrings.length; i++) {
            expressions[i] = parse(expressionStrings[i]);
        }
        
        return expressions;
    }

    /**
     * 检查表达式是否为常量
     * 
     * @param expressionString 表达式字符串
     * @return 如果是常量则返回 true
     */
    default boolean isConstant(String expressionString) {
        try {
            Expression expression = parse(expressionString);
            return expression.isConstant();
        } catch (ExpressionException e) {
            return false;
        }
    }

    /**
     * 获取表达式中的变量名
     * 
     * @param expressionString 表达式字符串
     * @return 变量名集合
     * @throws ExpressionException 如果解析失败
     */
    default java.util.Set<String> getVariableNames(String expressionString) throws ExpressionException {
        Expression expression = parse(expressionString);
        return expression.getVariableNames();
    }
}