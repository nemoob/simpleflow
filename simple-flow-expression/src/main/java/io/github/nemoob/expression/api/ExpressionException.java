package io.github.nemoob.expression.api;

/**
 * 表达式异常
 * 
 * 表达式解析或执行过程中发生的异常
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public class ExpressionException extends Exception {

    private final String expression;
    private final int position;
    private final String errorCode;

    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public ExpressionException(String message) {
        super(message);
        this.expression = null;
        this.position = -1;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ExpressionException(String message, Throwable cause) {
        super(message, cause);
        this.expression = null;
        this.position = -1;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param expression 表达式字符串
     */
    public ExpressionException(String message, String expression) {
        super(message);
        this.expression = expression;
        this.position = -1;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param expression 表达式字符串
     * @param cause 原因异常
     */
    public ExpressionException(String message, String expression, Throwable cause) {
        super(message, cause);
        this.expression = expression;
        this.position = -1;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param expression 表达式字符串
     * @param position 错误位置
     */
    public ExpressionException(String message, String expression, int position) {
        super(message);
        this.expression = expression;
        this.position = position;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param expression 表达式字符串
     * @param position 错误位置
     * @param cause 原因异常
     */
    public ExpressionException(String message, String expression, int position, Throwable cause) {
        super(message, cause);
        this.expression = expression;
        this.position = position;
        this.errorCode = null;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param expression 表达式字符串
     * @param position 错误位置
     * @param errorCode 错误代码
     */
    public ExpressionException(String message, String expression, int position, String errorCode) {
        super(message);
        this.expression = expression;
        this.position = position;
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param expression 表达式字符串
     * @param position 错误位置
     * @param errorCode 错误代码
     * @param cause 原因异常
     */
    public ExpressionException(String message, String expression, int position, String errorCode, Throwable cause) {
        super(message, cause);
        this.expression = expression;
        this.position = position;
        this.errorCode = errorCode;
    }

    /**
     * 获取表达式字符串
     * 
     * @return 表达式字符串
     */
    public String getExpression() {
        return expression;
    }

    /**
     * 获取错误位置
     * 
     * @return 错误位置（-1表示未知）
     */
    public int getPosition() {
        return position;
    }

    /**
     * 获取错误代码
     * 
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取详细错误信息
     * 
     * @return 详细错误信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        
        if (expression != null) {
            sb.append(" [Expression: ").append(expression).append("]");
        }
        
        if (position >= 0) {
            sb.append(" [Position: ").append(position).append("]");
        }
        
        if (errorCode != null) {
            sb.append(" [ErrorCode: ").append(errorCode).append("]");
        }
        
        return sb.toString();
    }

    /**
     * 获取带位置指示的表达式
     * 
     * @return 带位置指示的表达式
     */
    public String getExpressionWithPosition() {
        if (expression == null || position < 0) {
            return expression;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(expression).append("\n");
        
        // 添加位置指示符
        for (int i = 0; i < position; i++) {
            sb.append(" ");
        }
        sb.append("^");
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getDetailedMessage();
    }
}