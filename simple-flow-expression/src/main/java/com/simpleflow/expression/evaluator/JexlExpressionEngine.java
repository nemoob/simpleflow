package com.simpleflow.expression.evaluator;

import com.simpleflow.expression.api.Expression;
import com.simpleflow.expression.api.ExpressionEngine;
import com.simpleflow.expression.api.ExpressionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 JEXL 的表达式引擎实现
 * 
 * 使用 Apache Commons JEXL 作为底层表达式解析和执行引擎
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class JexlExpressionEngine implements ExpressionEngine {



    private final JexlEngine jexlEngine;
    private final Map<String, JexlExpression> expressionCache = new ConcurrentHashMap<>();
    private final boolean cacheEnabled;

    /**
     * 默认构造函数
     */
    public JexlExpressionEngine() {
        this(true);
    }

    /**
     * 构造函数
     * 
     * @param cacheEnabled 是否启用表达式缓存
     */
    public JexlExpressionEngine(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        this.jexlEngine = new JexlBuilder()
                .cache(512)
                .strict(false)
                .silent(false)
                .create();
    }

    /**
     * 构造函数
     * 
     * @param jexlEngine 自定义 JEXL 引擎
     * @param cacheEnabled 是否启用表达式缓存
     */
    public JexlExpressionEngine(JexlEngine jexlEngine, boolean cacheEnabled) {
        this.jexlEngine = jexlEngine;
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * 构造函数
     * 
     * @param configuration 配置参数
     */
    public JexlExpressionEngine(Map<String, Object> configuration) {
        Boolean cacheEnabledConfig = (Boolean) configuration.get("cacheEnabled");
        this.cacheEnabled = cacheEnabledConfig != null ? cacheEnabledConfig : true;
        
        JexlBuilder builder = new JexlBuilder()
                .cache(512)
                .strict(false)
                .silent(false);
        
        // 可以根据配置参数调整 JEXL 引擎设置
        Integer cacheSize = (Integer) configuration.get("cacheSize");
        if (cacheSize != null) {
            builder.cache(cacheSize);
        }
        
        Boolean strict = (Boolean) configuration.get("strict");
        if (strict != null) {
            builder.strict(strict);
        }
        
        Boolean silent = (Boolean) configuration.get("silent");
        if (silent != null) {
            builder.silent(silent);
        }
        
        this.jexlEngine = builder.create();
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) throws ExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionException("Expression cannot be null or empty");
        }

        try {
            JexlExpression jexlExpression = getJexlExpression(expression);
            JexlContext jexlContext = createJexlContext(context);
            
            Object result = jexlExpression.evaluate(jexlContext);
            log.debug("Evaluated expression '{}' with result: {}", expression, result);
            
            return result;
        } catch (JexlException e) {
            log.error("Failed to evaluate expression: {}", expression, e);
            throw new ExpressionException("Failed to evaluate expression: " + e.getMessage(), expression, e);
        } catch (Exception e) {
            log.error("Unexpected error evaluating expression: {}", expression, e);
            throw new ExpressionException("Unexpected error: " + e.getMessage(), expression, e);
        }
    }

    @Override
    public <T> T evaluate(String expression, Map<String, Object> context, Class<T> expectedType) throws ExpressionException {
        Object result = evaluate(expression, context);
        
        if (result == null) {
            return null;
        }
        
        if (expectedType.isInstance(result)) {
            return expectedType.cast(result);
        }
        
        // 尝试类型转换
        try {
            return convertType(result, expectedType);
        } catch (Exception e) {
            throw new ExpressionException(
                "Cannot convert result of type " + result.getClass().getSimpleName() + 
                " to expected type " + expectedType.getSimpleName(), expression, e);
        }
    }

    @Override
    public boolean evaluateBoolean(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        
        if (result == null) {
            return false;
        }
        
        // 转换为布尔值
        if (result instanceof Number) {
            return ((Number) result).doubleValue() != 0.0;
        }
        
        if (result instanceof String) {
            String str = (String) result;
            return !str.isEmpty() && !"false".equalsIgnoreCase(str) && !"0".equals(str);
        }
        
        return true; // 非空对象视为 true
    }

    @Override
    public String evaluateString(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        return result == null ? null : result.toString();
    }

    @Override
    public Number evaluateNumber(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        
        if (result instanceof Number) {
            return (Number) result;
        }
        
        if (result instanceof String) {
            try {
                return Double.parseDouble((String) result);
            } catch (NumberFormatException e) {
                throw new ExpressionException("Cannot convert string '" + result + "' to number", expression, e);
            }
        }
        
        throw new ExpressionException("Result is not a number: " + result, expression);
    }

    @Override
    public boolean isValidExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            jexlEngine.createExpression(expression);
            return true;
        } catch (JexlException e) {
            log.debug("Invalid expression: {}", expression, e);
            return false;
        }
    }

    @Override
    public Expression parseExpression(String expression) throws ExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionException("Expression cannot be null or empty");
        }
        
        try {
            JexlExpression jexlExpression = getJexlExpression(expression);
            return new JexlExpressionWrapper(expression, jexlExpression);
        } catch (JexlException e) {
            throw new ExpressionException("Failed to parse expression: " + e.getMessage(), expression, e);
        }
    }

    @Override
    public String getEngineName() {
        return "JEXL";
    }

    @Override
    public String getEngineVersion() {
        return "3.x";
    }

    @Override
    public String getSupportedSyntax() {
        return "Apache Commons JEXL Expression Language";
    }

    /**
     * 获取 JEXL 表达式对象
     * 
     * @param expression 表达式字符串
     * @return JEXL 表达式对象
     */
    private JexlExpression getJexlExpression(String expression) {
        if (cacheEnabled) {
            return expressionCache.computeIfAbsent(expression, jexlEngine::createExpression);
        } else {
            return jexlEngine.createExpression(expression);
        }
    }

    /**
     * 创建 JEXL 上下文
     * 
     * @param context 上下文变量
     * @return JEXL 上下文
     */
    private JexlContext createJexlContext(Map<String, Object> context) {
        JexlContext jexlContext = new MapContext();
        if (context != null) {
            context.forEach(jexlContext::set);
        }
        return jexlContext;
    }

    /**
     * 类型转换
     * 
     * @param value 原始值
     * @param targetType 目标类型
     * @param <T> 目标类型
     * @return 转换后的值
     */
    @SuppressWarnings("unchecked")
    private <T> T convertType(Object value, Class<T> targetType) {
        if (targetType == String.class) {
            return (T) value.toString();
        }
        
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            }
            if (value instanceof Number) {
                return (T) Boolean.valueOf(((Number) value).doubleValue() != 0.0);
            }
            if (value instanceof String) {
                return (T) Boolean.valueOf(Boolean.parseBoolean((String) value));
            }
        }
        
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            }
            if (value instanceof String) {
                return (T) Integer.valueOf(Integer.parseInt((String) value));
            }
        }
        
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return (T) Long.valueOf(((Number) value).longValue());
            }
            if (value instanceof String) {
                return (T) Long.valueOf(Long.parseLong((String) value));
            }
        }
        
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            }
            if (value instanceof String) {
                return (T) Double.valueOf(Double.parseDouble((String) value));
            }
        }
        
        throw new ClassCastException("Cannot convert " + value.getClass() + " to " + targetType);
    }

    /**
     * 清理表达式缓存
     */
    public void clearCache() {
        expressionCache.clear();
        log.info("Expression cache cleared");
    }

    /**
     * 获取缓存大小
     * 
     * @return 缓存大小
     */
    public int getCacheSize() {
        return expressionCache.size();
    }
}