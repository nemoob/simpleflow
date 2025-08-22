package io.github.nemoob.expression.evaluator;

import io.github.nemoob.expression.api.Expression;
import io.github.nemoob.expression.api.ExpressionEngine;
import io.github.nemoob.expression.api.ExpressionException;
import lombok.extern.slf4j.Slf4j;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Kotlin 脚本的表达式引擎实现
 *
 * 使用 Kotlin 脚本引擎来执行条件表达式
 *
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class KotlinScriptExpressionEngine implements ExpressionEngine {

    private final ScriptEngine kotlinEngine;
    private final Map<String, Object> compiledScriptCache = new ConcurrentHashMap<>();
    private final boolean cacheEnabled;

    /**
     * 默认构造函数
     */
    public KotlinScriptExpressionEngine() {
        this(true);
    }

    /**
     * 构造函数
     *
     * @param cacheEnabled 是否启用脚本缓存
     */
    public KotlinScriptExpressionEngine(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        ScriptEngineManager manager = new ScriptEngineManager();
        this.kotlinEngine = manager.getEngineByExtension("kts");

        if (this.kotlinEngine == null) {
            throw new RuntimeException("Kotlin script engine not found. Please ensure kotlin-scripting-jsr223 is in the classpath.");
        }
    }

    /**
     * 构造函数
     *
     * @param configuration 配置参数
     */
    public KotlinScriptExpressionEngine(Map<String, Object> configuration) {
        Boolean cacheEnabledConfig = (Boolean) configuration.get("cacheEnabled");
        this.cacheEnabled = cacheEnabledConfig != null ? cacheEnabledConfig : true;

        ScriptEngineManager manager = new ScriptEngineManager();
        this.kotlinEngine = manager.getEngineByExtension("kts");

        if (this.kotlinEngine == null) {
            throw new RuntimeException("Kotlin script engine not found. Please ensure kotlin-scripting-jsr223 is in the classpath.");
        }
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) throws ExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionException("Expression cannot be null or empty");
        }

        try {
            // 设置绑定变量
            Bindings bindings = kotlinEngine.createBindings();
            if (context != null) {
                bindings.putAll(context);
            }

            // 执行脚本
            Object result;
            if (cacheEnabled) {
                // 简单的缓存实现，实际项目中可能需要更复杂的缓存策略
                String cacheKey = expression + "_" + (context != null ? context.hashCode() : 0);
                result = compiledScriptCache.computeIfAbsent(cacheKey, key -> {
                    try {
                        return kotlinEngine.eval(expression, bindings);
                    } catch (ScriptException e) {
                        throw new RuntimeException("Failed to evaluate Kotlin script: " + expression, e);
                    }
                });
            } else {
                result = kotlinEngine.eval(expression, bindings);
            }

            log.debug("Kotlin script '{}' evaluated to: {}", expression, result);
            return result;

        } catch (ScriptException e) {
            log.error("Failed to evaluate Kotlin script: {}", expression, e);
            throw new ExpressionException("Kotlin script evaluation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error evaluating Kotlin script: {}", expression, e);
            throw new ExpressionException("Kotlin script evaluation failed: " + e.getMessage(), e);
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
            // 尝试编译脚本来验证语法
            kotlinEngine.eval(expression, kotlinEngine.createBindings());
            return true;
        } catch (Exception e) {
            log.debug("Invalid Kotlin script: {}", expression, e);
            return false;
        }
    }

    @Override
    public Expression parseExpression(String expression) throws ExpressionException {
        try {
            // 验证脚本语法
            if (!isValidExpression(expression)) {
                throw new ExpressionException("Invalid Kotlin script syntax: " + expression);
            }
            return new KotlinScriptExpression(expression, kotlinEngine);
        } catch (Exception e) {
            throw new ExpressionException("Failed to parse Kotlin script: " + expression, e);
        }
    }

    @Override
    public String getEngineName() {
        return "Kotlin Script";
    }

    @Override
    public String getEngineVersion() {
        return kotlinEngine != null ? kotlinEngine.getFactory().getEngineVersion() : "Unknown";
    }

    @Override
    public String getSupportedSyntax() {
        return "Kotlin Script Syntax - Full Kotlin language support for condition evaluation";
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

        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " + to " + targetType);
    }

    /**
     * 清空脚本缓存
     */
    public void clearCache() {
        compiledScriptCache.clear();
        log.debug("Kotlin script cache cleared");
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public int getCacheSize() {
        return compiledScriptCache.size();
    }

    /**
     * Kotlin 脚本表达式包装类
     */
    private static class KotlinScriptExpression implements Expression {
        private final String expressionString;
        private final ScriptEngine engine;

        public KotlinScriptExpression(String expressionString, ScriptEngine engine) {
            this.expressionString = expressionString;
            this.engine = engine;
        }

        @Override
        public Object execute(Map<String, Object> context) throws ExpressionException {
            try {
                Bindings bindings = engine.createBindings();
                if (context != null) {
                    bindings.putAll(context);
                }
                return engine.eval(expressionString, bindings);
            } catch (ScriptException e) {
                throw new ExpressionException("Failed to execute Kotlin script: " + expressionString, e);
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