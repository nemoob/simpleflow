package io.github.nemoob.expression.api;

import java.util.Map;
import java.util.Set;

/**
 * 表达式接口
 * 
 * 表示一个已解析的表达式，可以重复执行
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public interface Expression {

    /**
     * 执行表达式
     * 
     * @param context 上下文变量
     * @return 执行结果
     * @throws ExpressionException 表达式异常
     */
    Object execute(Map<String, Object> context) throws ExpressionException;

    /**
     * 执行表达式并返回指定类型
     * 
     * @param context 上下文变量
     * @param expectedType 期望的返回类型
     * @param <T> 返回类型
     * @return 执行结果
     * @throws ExpressionException 表达式异常
     */
    <T> T execute(Map<String, Object> context, Class<T> expectedType) throws ExpressionException;

    /**
     * 获取原始表达式字符串
     * 
     * @return 表达式字符串
     */
    String getExpressionString();

    /**
     * 获取表达式中使用的变量名
     * 
     * @return 变量名集合
     */
    Set<String> getVariableNames();

    /**
     * 检查表达式是否为常量（不依赖任何变量）
     * 
     * @return 是否为常量
     */
    boolean isConstant();

    /**
     * 获取表达式类型
     * 
     * @return 表达式类型
     */
    ExpressionType getType();

    /**
     * 获取表达式的预期返回类型
     * 
     * @return 返回类型
     */
    Class<?> getReturnType();

    /**
     * 验证表达式在给定上下文中是否可执行
     * 
     * @param context 上下文变量
     * @return 验证结果
     */
    ValidationResult validate(Map<String, Object> context);

    /**
     * 表达式类型枚举
     */
    enum ExpressionType {
        /**
         * 布尔表达式
         */
        BOOLEAN,
        
        /**
         * 数值表达式
         */
        NUMERIC,
        
        /**
         * 字符串表达式
         */
        STRING,
        
        /**
         * 对象表达式
         */
        OBJECT,
        
        /**
         * 未知类型
         */
        UNKNOWN
    }

    /**
     * 验证结果
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Set<String> missingVariables;

        public ValidationResult(boolean valid, String errorMessage, Set<String> missingVariables) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.missingVariables = missingVariables;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public static ValidationResult missingVariables(Set<String> missingVariables) {
            return new ValidationResult(false, "Missing variables: " + missingVariables, missingVariables);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Set<String> getMissingVariables() {
            return missingVariables;
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errorMessage='" + errorMessage + '\'' +
                    ", missingVariables=" + missingVariables +
                    '}';
        }
    }
}