package io.github.nemoob.expression.evaluator;

import io.github.nemoob.expression.api.Expression;
import io.github.nemoob.expression.api.ExpressionEngine;
import io.github.nemoob.expression.api.ExpressionException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 链式表达式引擎
 * 
 * 支持THEN关键字的链式语法，如：THEN(a, b, c, d)
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class ChainExpressionEngine implements ExpressionEngine {

    private static final String ENGINE_NAME = "Chain";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String SUPPORTED_SYNTAX = "THEN(component1, component2, ...), IF(condition, trueComponent, falseComponent), SWITCH(condition, case1:component1, case2:component2, default:defaultComponent)";
    
    // 链式语法模式
    private static final Pattern THEN_PATTERN = Pattern.compile("THEN\\s*\\(\\s*([^)]+)\\s*\\)");
    private static final Pattern IF_PATTERN = Pattern.compile("IF\\s*\\(\\s*([^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^)]+)\\s*\\)");
    private static final Pattern SWITCH_PATTERN = Pattern.compile("SWITCH\\s*\\(\\s*([^,]+)\\s*,\\s*(.+)\\s*\\)");
    private static final Pattern CASE_PATTERN = Pattern.compile("([^:]+)\\s*:\\s*([^,]+)");
    
    @Override
    public Object evaluate(String expression, Map<String, Object> context) throws ExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionException("Expression cannot be null or empty");
        }
        
        String trimmedExpression = expression.trim();
        
        try {
            // 处理THEN语法
            if (trimmedExpression.startsWith("THEN")) {
                return evaluateThenExpression(trimmedExpression, context);
            }
            // 处理IF语法
            else if (trimmedExpression.startsWith("IF")) {
                return evaluateIfExpression(trimmedExpression, context);
            }
            // 处理SWITCH语法
            else if (trimmedExpression.startsWith("SWITCH")) {
                return evaluateSwitchExpression(trimmedExpression, context);
            }
            // 处理简单组件调用
            else {
                return evaluateComponent(trimmedExpression, context);
            }
        } catch (Exception e) {
            log.error("Failed to evaluate chain expression: {}", expression, e);
            throw new ExpressionException("Failed to evaluate expression: " + expression, e);
        }
    }
    
    /**
     * 评估THEN表达式
     * 语法：THEN(a, b, c, d)
     */
    private Object evaluateThenExpression(String expression, Map<String, Object> context) throws ExpressionException {
        Matcher matcher = THEN_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new ExpressionException("Invalid THEN syntax: " + expression);
        }
        
        String componentsStr = matcher.group(1);
        String[] components = parseComponents(componentsStr);
        
        Object result = null;
        for (String component : components) {
            result = evaluateComponent(component.trim(), context);
            // 将前一个组件的结果作为下一个组件的输入
            if (result != null) {
                context.put("previousResult", result);
            }
        }
        
        return result;
    }
    
    /**
     * 评估IF表达式
     * 语法：IF(condition, trueComponent, falseComponent)
     */
    private Object evaluateIfExpression(String expression, Map<String, Object> context) throws ExpressionException {
        Matcher matcher = IF_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new ExpressionException("Invalid IF syntax: " + expression);
        }
        
        String condition = matcher.group(1).trim();
        String trueComponent = matcher.group(2).trim();
        String falseComponent = matcher.group(3).trim();
        
        boolean conditionResult = evaluateCondition(condition, context);
        
        if (conditionResult) {
            return evaluateComponent(trueComponent, context);
        } else {
            return evaluateComponent(falseComponent, context);
        }
    }
    
    /**
     * 评估SWITCH表达式
     * 语法：SWITCH(condition, case1:component1, case2:component2, default:defaultComponent)
     */
    private Object evaluateSwitchExpression(String expression, Map<String, Object> context) throws ExpressionException {
        Matcher matcher = SWITCH_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new ExpressionException("Invalid SWITCH syntax: " + expression);
        }
        
        String condition = matcher.group(1).trim();
        String casesStr = matcher.group(2).trim();
        
        Object conditionValue = evaluateSimpleExpression(condition, context);
        
        String[] cases = casesStr.split(",");
        String defaultComponent = null;
        
        for (String caseStr : cases) {
            caseStr = caseStr.trim();
            if (caseStr.startsWith("default:")) {
                defaultComponent = caseStr.substring(8).trim();
                continue;
            }
            
            Matcher caseMatcher = CASE_PATTERN.matcher(caseStr);
            if (caseMatcher.matches()) {
                String caseValue = caseMatcher.group(1).trim();
                String component = caseMatcher.group(2).trim();
                
                if (Objects.equals(String.valueOf(conditionValue), caseValue) || 
                    Objects.equals(conditionValue, parseValue(caseValue))) {
                    return evaluateComponent(component, context);
                }
            }
        }
        
        // 执行默认分支
        if (defaultComponent != null) {
            return evaluateComponent(defaultComponent, context);
        }
        
        return null;
    }
    
    /**
     * 评估单个组件
     */
    private Object evaluateComponent(String component, Map<String, Object> context) throws ExpressionException {
        // 如果是嵌套的链式表达式，递归处理
        if (component.contains("THEN(") || component.contains("IF(") || component.contains("SWITCH(")) {
            return evaluate(component, context);
        }
        
        // 处理Bean方法调用：beanName.methodName
        if (component.contains(".")) {
            return evaluateBeanMethod(component, context);
        }
        
        // 处理简单组件名称
        return evaluateSimpleComponent(component, context);
    }
    
    /**
     * 评估Bean方法调用
     */
    private Object evaluateBeanMethod(String component, Map<String, Object> context) throws ExpressionException {
        String[] parts = component.split("\\.", 2);
        if (parts.length != 2) {
            throw new ExpressionException("Invalid bean method syntax: " + component);
        }
        
        String beanName = parts[0].trim();
        String methodName = parts[1].trim();
        
        // 从上下文中获取Bean实例
        Object bean = context.get(beanName);
        if (bean == null) {
            throw new ExpressionException("Bean not found: " + beanName);
        }
        
        try {
            // 使用反射调用方法
            return bean.getClass().getMethod(methodName).invoke(bean);
        } catch (Exception e) {
            throw new ExpressionException("Failed to invoke method: " + component, e);
        }
    }
    
    /**
     * 评估简单组件
     */
    private Object evaluateSimpleComponent(String component, Map<String, Object> context) throws ExpressionException {
        // 从上下文中获取组件
        Object componentObj = context.get(component);
        if (componentObj == null) {
            throw new ExpressionException("Component not found: " + component);
        }
        
        // 如果是可调用对象，执行它
        if (componentObj instanceof Runnable) {
            ((Runnable) componentObj).run();
            return null;
        }
        
        return componentObj;
    }
    
    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, Map<String, Object> context) throws ExpressionException {
        // 简单的条件评估，可以扩展支持更复杂的表达式
        Object result = evaluateSimpleExpression(condition, context);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return Boolean.parseBoolean(String.valueOf(result));
    }
    
    /**
     * 评估简单表达式
     */
    private Object evaluateSimpleExpression(String expression, Map<String, Object> context) {
        // 处理变量引用
        if (context.containsKey(expression)) {
            return context.get(expression);
        }
        
        // 处理字面量
        return parseValue(expression);
    }
    
    /**
     * 解析组件列表
     */
    private String[] parseComponents(String componentsStr) {
        List<String> components = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parentheses = 0;
        
        for (char c : componentsStr.toCharArray()) {
            if (c == '(') {
                parentheses++;
            } else if (c == ')') {
                parentheses--;
            } else if (c == ',' && parentheses == 0) {
                components.add(current.toString().trim());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        
        if (current.length() > 0) {
            components.add(current.toString().trim());
        }
        
        return components.toArray(new String[0]);
    }
    
    /**
     * 解析值
     */
    private Object parseValue(String value) {
        if (value == null) {
            return null;
        }
        
        value = value.trim();
        
        // 布尔值
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        
        // 数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // 不是数字，返回字符串
        }
        
        // 字符串（去掉引号）
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        
        return value;
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
        
        // 简单类型转换
        if (expectedType == String.class) {
            return expectedType.cast(String.valueOf(result));
        }
        
        throw new ExpressionException("Cannot convert result to expected type: " + expectedType.getName());
    }
    
    @Override
    public boolean evaluateBoolean(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return Boolean.parseBoolean(String.valueOf(result));
    }
    
    @Override
    public String evaluateString(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        return result != null ? String.valueOf(result) : null;
    }
    
    @Override
    public Number evaluateNumber(String expression, Map<String, Object> context) throws ExpressionException {
        Object result = evaluate(expression, context);
        if (result instanceof Number) {
            return (Number) result;
        }
        
        try {
            String str = String.valueOf(result);
            if (str.contains(".")) {
                return Double.parseDouble(str);
            } else {
                return Long.parseLong(str);
            }
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert result to number: " + result, e);
        }
    }
    
    @Override
    public boolean isValidExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = expression.trim();
        
        // 检查THEN语法
        if (trimmed.startsWith("THEN")) {
            return THEN_PATTERN.matcher(trimmed).matches();
        }
        
        // 检查IF语法
        if (trimmed.startsWith("IF")) {
            return IF_PATTERN.matcher(trimmed).matches();
        }
        
        // 检查SWITCH语法
        if (trimmed.startsWith("SWITCH")) {
            return SWITCH_PATTERN.matcher(trimmed).matches();
        }
        
        // 简单组件名称
        return trimmed.matches("[a-zA-Z_][a-zA-Z0-9_.]*");
    }
    
    @Override
    public Expression parseExpression(String expression) throws ExpressionException {
        return new ChainExpression(expression, this);
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
     * 链式表达式实现
     */
    private static class ChainExpression implements Expression {
        private final String expression;
        private final ChainExpressionEngine engine;
        
        public ChainExpression(String expression, ChainExpressionEngine engine) {
            this.expression = expression;
            this.engine = engine;
        }
        
        @Override
        public Object execute(Map<String, Object> context) throws ExpressionException {
            return engine.evaluate(expression, context);
        }
        
        @Override
        public <T> T execute(Map<String, Object> context, Class<T> expectedType) throws ExpressionException {
            return engine.evaluate(expression, context, expectedType);
        }
        
        @Override
        public String getExpressionString() {
            return expression;
        }
        
        @Override
        public Set<String> getVariableNames() {
            // 简单实现：解析表达式中的变量名
            Set<String> variables = new HashSet<>();
            // 这里可以根据需要实现更复杂的变量解析逻辑
            return variables;
        }
        
        @Override
        public boolean isConstant() {
            // 链式表达式通常不是常量
            return false;
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
                engine.isValidExpression(expression);
                return ValidationResult.success();
            } catch (Exception e) {
                return ValidationResult.failure("Invalid chain expression: " + e.getMessage());
            }
        }
    }
}