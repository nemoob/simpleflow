package com.simpleflow.expression.evaluator;

import com.simpleflow.expression.api.Expression;
import com.simpleflow.expression.api.ExpressionEngine;
import com.simpleflow.expression.api.ExpressionException;
import com.simpleflow.expression.wrapper.SpELExpressionWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Spring Expression Language (SpEL) 的表达式引擎实现
 * 
 * 使用 Spring Expression Language 作为底层表达式解析和执行引擎
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class SpELExpressionEngine implements ExpressionEngine {

    private static final String ENGINE_NAME = "SpEL";
    private static final String ENGINE_VERSION = "5.2.0";
    private static final String SUPPORTED_SYNTAX = "Spring Expression Language (SpEL)";

    private final ExpressionParser spelParser;
    private final Map<String, org.springframework.expression.Expression> expressionCache = new ConcurrentHashMap<>();
    private final boolean cacheEnabled;

    /**
     * 默认构造函数
     */
    public SpELExpressionEngine() {
        this(true);
    }

    /**
     * 构造函数
     * 
     * @param cacheEnabled 是否启用表达式缓存
     */
    public SpELExpressionEngine(boolean cacheEnabled) {
        this.spelParser = new SpelExpressionParser();
        this.cacheEnabled = cacheEnabled;
        log.debug("SpEL expression engine initialized with cache: {}", cacheEnabled);
    }

    /**
     * 构造函数
     * 
     * @param configuration 配置参数
     */
    public SpELExpressionEngine(Map<String, Object> configuration) {
        this.spelParser = new SpelExpressionParser();
        this.cacheEnabled = configuration != null && 
            Boolean.parseBoolean(configuration.getOrDefault("cacheEnabled", "true").toString());
        log.debug("SpEL expression engine initialized with configuration: {}", configuration);
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) throws ExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionException("Expression cannot be null or empty");
        }

        try {
            log.debug("Evaluating SpEL expression: {}", expression);
            
            org.springframework.expression.Expression spelExpression = getCompiledExpression(expression);
            StandardEvaluationContext evaluationContext = createEvaluationContext(context);
            
            Object result = spelExpression.getValue(evaluationContext);
            log.debug("SpEL expression evaluation result: {}", result);
            
            return result;
        } catch (Exception e) {
            log.error("Error evaluating SpEL expression: {}", expression, e);
            throw new ExpressionException("Failed to evaluate SpEL expression: " + expression, e);
        }
    }

    @Override
    public <T> T evaluate(String expression, Map<String, Object> context, Class<T> expectedType) throws ExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionException("Expression cannot be null or empty");
        }
        if (expectedType == null) {
            throw new ExpressionException("Expected type cannot be null");
        }

        try {
            log.debug("Evaluating SpEL expression with expected type {}: {}", expectedType.getSimpleName(), expression);
            
            org.springframework.expression.Expression spelExpression = getCompiledExpression(expression);
            StandardEvaluationContext evaluationContext = createEvaluationContext(context);
            
            T result = spelExpression.getValue(evaluationContext, expectedType);
            log.debug("SpEL expression evaluation result: {}", result);
            
            return result;
        } catch (Exception e) {
            log.error("Error evaluating SpEL expression with type {}: {}", expectedType.getSimpleName(), expression, e);
            throw new ExpressionException("Failed to evaluate SpEL expression: " + expression, e);
        }
    }

    @Override
    public boolean evaluateBoolean(String expression, Map<String, Object> context) throws ExpressionException {
        return evaluate(expression, context, Boolean.class);
    }

    @Override
    public String evaluateString(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        return result != null ? result.toString() : null;
    }

    @Override
    public Number evaluateNumber(String expression, Map<String, Object> context) throws ExpressionException {
        return evaluate(expression, context, Number.class);
    }

    @Override
    public boolean isValidExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        try {
            spelParser.parseExpression(expression);
            return true;
        } catch (Exception e) {
            log.debug("Invalid SpEL expression: {}", expression, e);
            return false;
        }
    }

    @Override
    public Expression parseExpression(String expression) throws ExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionException("Expression cannot be null or empty");
        }

        try {
            org.springframework.expression.Expression spelExpression = getCompiledExpression(expression);
            return new SpELExpressionWrapper(spelExpression, expression);
        } catch (Exception e) {
            log.error("Error parsing SpEL expression: {}", expression, e);
            throw new ExpressionException("Failed to parse SpEL expression: " + expression, e);
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public String getSupportedSyntax() {
        return SUPPORTED_SYNTAX;
    }

    /**
     * 获取编译后的表达式
     * 
     * @param expression 表达式字符串
     * @return 编译后的表达式
     */
    private org.springframework.expression.Expression getCompiledExpression(String expression) {
        if (cacheEnabled) {
            return expressionCache.computeIfAbsent(expression, key -> {
                log.debug("Compiling and caching SpEL expression: {}", key);
                return spelParser.parseExpression(key);
            });
        } else {
            log.debug("Compiling SpEL expression (no cache): {}", expression);
            return spelParser.parseExpression(expression);
        }
    }

    /**
     * 创建评估上下文
     * 
     * @param context 上下文变量
     * @return 评估上下文
     */
    private StandardEvaluationContext createEvaluationContext(Map<String, Object> context) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        
        if (context != null) {
            context.forEach(evaluationContext::setVariable);
        }
        
        return evaluationContext;
    }

    /**
     * 清除表达式缓存
     */
    public void clearCache() {
        if (cacheEnabled) {
            expressionCache.clear();
            log.debug("SpEL expression cache cleared");
        }
    }

    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("cacheEnabled", cacheEnabled);
        stats.put("cacheSize", expressionCache.size());
        return stats;
    }
}