package com.simpleflow.expression.wrapper;

import com.simpleflow.expression.api.Expression;
import com.simpleflow.expression.api.ExpressionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpEL 表达式包装器
 * 
 * 包装 Spring Expression Language 的 Expression 对象
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class SpELExpressionWrapper implements Expression {

    private final org.springframework.expression.Expression spelExpression;
    private final String originalExpression;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("#(\\w+)");

    /**
     * 构造函数
     * 
     * @param spelExpression Spring Expression 对象
     * @param originalExpression 原始表达式字符串
     */
    public SpELExpressionWrapper(org.springframework.expression.Expression spelExpression, String originalExpression) {
        this.spelExpression = spelExpression;
        this.originalExpression = originalExpression;
    }

    @Override
    public Object execute(Map<String, Object> context) throws ExpressionException {
        try {
            log.debug("Evaluating SpEL expression: {}", originalExpression);
            
            EvaluationContext evaluationContext = createEvaluationContext(context);
            Object result = spelExpression.getValue(evaluationContext);
            
            log.debug("SpEL expression evaluation result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error evaluating SpEL expression: {}", originalExpression, e);
            throw new ExpressionException("Failed to evaluate SpEL expression: " + originalExpression, e);
        }
    }

    @Override
    public <T> T execute(Map<String, Object> context, Class<T> expectedType) throws ExpressionException {
        if (expectedType == null) {
            throw new ExpressionException("Expected type cannot be null");
        }

        try {
            log.debug("Evaluating SpEL expression with expected type {}: {}", expectedType.getSimpleName(), originalExpression);
            
            EvaluationContext evaluationContext = createEvaluationContext(context);
            T result = spelExpression.getValue(evaluationContext, expectedType);
            
            log.debug("SpEL expression evaluation result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error evaluating SpEL expression with type {}: {}", expectedType.getSimpleName(), originalExpression, e);
            throw new ExpressionException("Failed to evaluate SpEL expression: " + originalExpression, e);
        }
    }



    @Override
    public String getExpressionString() {
        return originalExpression;
    }



    @Override
    public Set<String> getVariableNames() {
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(originalExpression);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    @Override
    public boolean isConstant() {
        return getVariableNames().isEmpty();
    }

    @Override
    public ExpressionType getType() {
        try {
            Class<?> returnType = spelExpression.getValueType();
            if (returnType == null) {
                return ExpressionType.UNKNOWN;
            }
            
            if (Boolean.class.isAssignableFrom(returnType) || boolean.class.isAssignableFrom(returnType)) {
                return ExpressionType.BOOLEAN;
            } else if (Number.class.isAssignableFrom(returnType) || returnType.isPrimitive()) {
                return ExpressionType.NUMERIC;
            } else if (String.class.isAssignableFrom(returnType)) {
                return ExpressionType.STRING;
            } else {
                return ExpressionType.OBJECT;
            }
        } catch (Exception e) {
            return ExpressionType.UNKNOWN;
        }
    }

    @Override
    public Class<?> getReturnType() {
        try {
            return spelExpression.getValueType();
        } catch (Exception e) {
            return Object.class;
        }
    }

    @Override
    public ValidationResult validate(Map<String, Object> context) {
        try {
            EvaluationContext evaluationContext = createEvaluationContext(context);
            spelExpression.getValue(evaluationContext);
            return ValidationResult.success();
        } catch (Exception e) {
            return ValidationResult.failure(e.getMessage());
        }
    }

    /**
     * 创建评估上下文
     * 
     * @param context 上下文变量
     * @return 评估上下文
     */
    private EvaluationContext createEvaluationContext(Map<String, Object> context) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        
        if (context != null) {
            context.forEach(evaluationContext::setVariable);
        }
        
        return evaluationContext;
    }

    /**
     * 获取底层的 Spring Expression 对象
     * 
     * @return Spring Expression 对象
     */
    public org.springframework.expression.Expression getSpelExpression() {
        return spelExpression;
    }

    @Override
    public String toString() {
        return "SpELExpressionWrapper{" +
                "expression='" + originalExpression + '\'' +
                ", engineType='spel'" +
                '}';
    }
}