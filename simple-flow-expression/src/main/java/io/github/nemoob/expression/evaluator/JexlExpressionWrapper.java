package io.github.nemoob.expression.evaluator;

import io.github.nemoob.expression.api.Expression;
import io.github.nemoob.expression.api.ExpressionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JEXL 表达式包装器
 * 
 * 将 JEXL 表达式包装为通用表达式接口
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class JexlExpressionWrapper implements Expression {



    private final String expressionString;
    private final JexlExpression jexlExpression;
    private final Set<String> variableNames;
    private final ExpressionType type;
    private final Class<?> returnType;

    /**
     * 构造函数
     * 
     * @param expressionString 表达式字符串
     * @param jexlExpression JEXL 表达式对象
     */
    public JexlExpressionWrapper(String expressionString, JexlExpression jexlExpression) {
        this.expressionString = expressionString;
        this.jexlExpression = jexlExpression;
        this.variableNames = extractVariableNames();
        this.type = determineExpressionType();
        this.returnType = determineReturnType();
    }

    @Override
    public Object execute(Map<String, Object> context) throws ExpressionException {
        try {
            JexlContext jexlContext = createJexlContext(context);
            Object result = jexlExpression.evaluate(jexlContext);
            
            log.debug("Executed expression '{}' with result: {}", expressionString, result);
            return result;
        } catch (JexlException e) {
            log.error("Failed to execute expression: {}", expressionString, e);
            throw new ExpressionException("Failed to execute expression: " + e.getMessage(), expressionString, e);
        } catch (Exception e) {
            log.error("Unexpected error executing expression: {}", expressionString, e);
            throw new ExpressionException("Unexpected error: " + e.getMessage(), expressionString, e);
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
        
        // 尝试类型转换
        try {
            return convertType(result, expectedType);
        } catch (Exception e) {
            throw new ExpressionException(
                "Cannot convert result of type " + result.getClass().getSimpleName() + 
                " to expected type " + expectedType.getSimpleName(), expressionString, e);
        }
    }

    @Override
    public String getExpressionString() {
        return expressionString;
    }

    @Override
    public Set<String> getVariableNames() {
        return Collections.unmodifiableSet(variableNames);
    }

    @Override
    public boolean isConstant() {
        return variableNames.isEmpty();
    }

    @Override
    public ExpressionType getType() {
        return type;
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public ValidationResult validate(Map<String, Object> context) {
        // 检查必需的变量是否存在
        Set<String> missingVariables = new HashSet<>();
        for (String variableName : variableNames) {
            if (context == null || !context.containsKey(variableName)) {
                missingVariables.add(variableName);
            }
        }
        
        if (!missingVariables.isEmpty()) {
            return ValidationResult.missingVariables(missingVariables);
        }
        
        // 尝试执行表达式以验证其有效性
        try {
            execute(context);
            return ValidationResult.success();
        } catch (ExpressionException e) {
            return ValidationResult.failure(e.getMessage());
        }
    }

    /**
     * 提取表达式中的变量名
     * 
     * @return 变量名集合
     */
    private Set<String> extractVariableNames() {
        Set<String> variables = new HashSet<>();
        
        try {
            // JEXL 3.x 中需要通过其他方式获取变量
            // 这里使用简单的字符串解析作为替代方案
            String expr = expressionString;
            
            // 先移除字符串字面量，避免误识别
            String cleanExpr = expr.replaceAll("'[^']*'", " ").replaceAll("\"[^\"]*\"", " ");
            
            // 移除操作符和特殊字符，保留标识符
            cleanExpr = cleanExpr.replaceAll("[>=<!?:+\\-*/()\\[\\]{}.,;]", " ");
            
            // 简单的变量名提取逻辑（可以根据需要改进）
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
            java.util.regex.Matcher matcher = pattern.matcher(cleanExpr);
            while (matcher.find()) {
                String token = matcher.group().trim();
                // 排除关键字、常量和数字
                if (!token.isEmpty() && !isKeywordOrConstant(token) && !token.matches("\\d+")) {
                    variables.add(token);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract variables from expression: {}", expressionString, e);
        }
        
        return variables;
    }
    
    /**
     * 检查是否为关键字或常量
     */
    private boolean isKeywordOrConstant(String token) {
        return "true".equals(token) || "false".equals(token) || "null".equals(token) ||
               "if".equals(token) || "else".equals(token) || "for".equals(token) ||
               "while".equals(token) || "return".equals(token) || "new".equals(token) ||
               "var".equals(token) || "function".equals(token) || "size".equals(token) ||
               "length".equals(token) || "empty".equals(token);
    }

    /**
     * 确定表达式类型
     * 
     * @return 表达式类型
     */
    private ExpressionType determineExpressionType() {
        // 简单的类型推断，基于表达式字符串的模式
        String expr = expressionString.toLowerCase().trim();
        
        // 布尔表达式模式
        if (expr.contains("==") || expr.contains("!=") || expr.contains(">=") || 
            expr.contains("<=") || expr.contains(">") || expr.contains("<") ||
            expr.contains("&&") || expr.contains("||") || expr.contains("!") ||
            expr.equals("true") || expr.equals("false")) {
            return ExpressionType.BOOLEAN;
        }
        
        // 数值表达式模式
        if (expr.matches(".*[+\\-*/].*") || expr.matches("\\d+(\\.\\d+)?")) {
            return ExpressionType.NUMERIC;
        }
        
        // 字符串表达式模式
        if (expr.startsWith("'") && expr.endsWith("'") || 
            expr.startsWith("\"") && expr.endsWith("\"")) {
            return ExpressionType.STRING;
        }
        
        return ExpressionType.UNKNOWN;
    }

    /**
     * 确定返回类型
     * 
     * @return 返回类型
     */
    private Class<?> determineReturnType() {
        switch (type) {
            case BOOLEAN:
                return Boolean.class;
            case NUMERIC:
                return Number.class;
            case STRING:
                return String.class;
            default:
                return Object.class;
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

    @Override
    public String toString() {
        return "JexlExpressionWrapper{" +
                "expression='" + expressionString + '\'' +
                ", type=" + type +
                ", variables=" + variableNames +
                '}';
    }
}