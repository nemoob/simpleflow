package io.github.nemoob.expression.evaluator;

import io.github.nemoob.expression.api.Expression;
import io.github.nemoob.expression.api.ExpressionEngine;
import io.github.nemoob.expression.api.ExpressionException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Groovy 脚本的表达式引擎实现
 *
 * 使用 Groovy 脚本引擎来执行条件表达式
 *
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class GroovyScriptExpressionEngine implements ExpressionEngine {

    private final GroovyShell groovyShell;
    private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();
    private final boolean cacheEnabled;

    /**
     * 默认构造函数
     */
    public GroovyScriptExpressionEngine() {
        this(true);
    }

    /**
     * 构造函数
     *
     * @param cacheEnabled 是否启用脚本缓存
     */
    public GroovyScriptExpressionEngine(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        this.groovyShell = new GroovyShell();
    }

    /**
     * 构造函数
     *
     * @param configuration 配置参数
     */
    public GroovyScriptExpressionEngine(Map<String, Object> configuration) {
        Boolean cacheEnabledConfig = (Boolean) configuration.get("cacheEnabled");
        this.cacheEnabled = cacheEnabledConfig != null ? cacheEnabledConfig : true;
        this.groovyShell = new GroovyShell();
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) throws ExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionException("Expression cannot be null or empty");
        }

        try {
            // 获取或编译脚本
            Script script = getScript(expression);

            // 设置绑定变量
            Binding binding = new Binding();
            if (context != null) {
                for (Map.Entry<String, Object> entry : context.entrySet()) {
                    binding.setVariable(entry.getKey(), entry.getValue());
                }
            }
            script.setBinding(binding);

            // 执行脚本
            Object result = script.run();
            log.debug("Groovy script '{}' evaluated to: {}", expression, result);
            return result;

        } catch (Exception e) {
            log.error("Failed to evaluate Groovy script: {}", expression, e);
            throw new ExpressionException("Groovy script evaluation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T evaluate(String expression, Map<String, Object> context, Class<T> expectedType) throws ExpressionException {
        Object result = evaluate(expression, context);
        try {
            return convertType(result, expectedType);
        } catch (Exception e) {
            throw new ExpressionException("Failed to convert result to expected type: " + expectedType.getName(), e);
        }
    }

    @Override
    public boolean evaluateBoolean(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        if (result instanceof Boolean) {
            return (Boolean) result;
        } else if (result instanceof Number) {
            return ((Number) result).doubleValue() != 0.0;
        } else if (result instanceof String) {
            return Boolean.parseBoolean((String) result);
        } else {
            return result != null;
        }
    }

    @Override
    public String evaluateString(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        return result != null ? result.toString() : null;
    }

    @Override
    public Number evaluateNumber(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        if (result instanceof Number) {
            return (Number) result;
        } else if (result instanceof String) {
            try {
                return Double.parseDouble((String) result);
            } catch (NumberFormatException e) {
                throw new ExpressionException("Cannot convert string to number: " + result, e);
            }
        } else {
            throw new ExpressionException("Result is not a number: " + result);
        }
    }

    @Override
    public boolean isValidExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        try {
            groovyShell.parse(expression);
            return true;
        } catch (Exception e) {
            log.debug("Invalid Groovy script: {}", expression, e);
            return false;
        }
    }

    @Override
    public Expression parseExpression(String expression) throws ExpressionException {
        try {
            Script script = getScript(expression);
            return new GroovyScriptExpression(script, expression);
        } catch (Exception e) {
            throw new ExpressionException("Failed to parse Groovy script: " + expression, e);
        }
    }

    @Override
    public String getEngineName() {
        return "Groovy Script";
    }

    @Override
    public String getEngineVersion() {
        return groovy.lang.GroovySystem.getVersion();
    }

    @Override
    public String getSupportedSyntax() {
        return "Groovy Script Syntax - Full Groovy language support for condition evaluation";
    }

    /**
     * 获取或编译脚本
     *
     * @param expression 脚本表达式
     * @return 编译后的脚本
     */
    private Script getScript(String expression) {
        if (cacheEnabled) {
            return scriptCache.computeIfAbsent(expression, expr -> {
                try {
                    return groovyShell.parse(expr);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse Groovy script: " + expr, e);
                }
            });
        } else {
            return groovyShell.parse(expression);
        }
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
        if (value == null) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return (T) value;
        }

        if (targetType == String.class) {
            return (T) value.toString();
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            } else if (value instanceof Number) {
                return (T) Boolean.valueOf(((Number) value).doubleValue() != 0.0);
            } else {
                return (T) Boolean.valueOf(value.toString());
            }
        }

        if (Number.class.isAssignableFrom(targetType)) {
            if (value instanceof Number) {
                Number num = (Number) value;
                if (targetType == Integer.class || targetType == int.class) {
                    return (T) Integer.valueOf(num.intValue());
                } else if (targetType == Long.class || targetType == long.class) {
                    return (T) Long.valueOf(num.longValue());
                } else if (targetType == Double.class || targetType == double.class) {
                    return (T) Double.valueOf(num.doubleValue());
                } else if (targetType == Float.class || targetType == float.class) {
                    return (T) Float.valueOf(num.floatValue());
                }
            } else if (value instanceof String) {
                String str = (String) value;
                if (targetType == Integer.class || targetType == int.class) {
                    return (T) Integer.valueOf(str);
                } else if (targetType == Long.class || targetType == long.class) {
                    return (T) Long.valueOf(str);
                } else if (targetType == Double.class || targetType == double.class) {
                    return (T) Double.valueOf(str);
                } else if (targetType == Float.class || targetType == float.class) {
                    return (T) Float.valueOf(str);
                }
            }
        }

        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to " + targetType);
    }

    /**
     * 清空脚本缓存
     */
    public void clearCache() {
        scriptCache.clear();
        log.debug("Groovy script cache cleared");
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public int getCacheSize() {
        return scriptCache.size();
    }

    /**
     * Groovy 脚本表达式包装类
     */
    private static class GroovyScriptExpression implements Expression {
        private final Script script;
        private final String expressionString;

        public GroovyScriptExpression(Script script, String expressionString) {
            this.script = script;
            this.expressionString = expressionString;
        }

        @Override
        public Object execute(Map<String, Object> context) throws ExpressionException {
            try {
                Binding binding = new Binding();
                if (context != null) {
                    for (Map.Entry<String, Object> entry : context.entrySet()) {
                        binding.setVariable(entry.getKey(), entry.getValue());
                    }
                }
                script.setBinding(binding);
                return script.run();
            } catch (Exception e) {
                throw new ExpressionException("Failed to execute Groovy script: " + expressionString, e);
            }
        }

        @Override
        public <T> T execute(Map<String, Object> context, Class<T> expectedType) throws ExpressionException {
            Object result = execute(context);
            if (result == null) {
                return null;
            }
            if (expectedType.isInstance(result)) {
                return expectedType.cast(result);
            }
            throw new ExpressionException("Cannot convert result to expected type: " + expectedType.getName());
        }

        @Override
        public String getExpressionString() {
            return expressionString;
        }

        @Override
        public java.util.Set<String> getVariableNames() {
            // 简单实现：从表达式字符串中提取变量名
            // 这里可以根据需要实现更复杂的解析逻辑
            return new java.util.HashSet<>();
        }

        @Override
        public boolean isConstant() {
            return getVariableNames().isEmpty();
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.OBJECT;
        }

        @Override
        public Class<?> getReturnType() {
            return Object.class;
        }

        @Override
        public ValidationResult validate(Map<String, Object> context) {
            try {
                execute(context);
                return ValidationResult.success();
            } catch (ExpressionException e) {
                return ValidationResult.failure(e.getMessage());
            }
        }
    }
}