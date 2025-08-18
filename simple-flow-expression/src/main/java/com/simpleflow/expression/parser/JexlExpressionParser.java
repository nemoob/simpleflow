package com.simpleflow.expression.parser;

import com.simpleflow.expression.api.Expression;
import com.simpleflow.expression.api.ExpressionException;
import com.simpleflow.expression.evaluator.JexlExpressionWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JEXL 表达式解析器实现
 * 
 * 使用 Apache Commons JEXL 作为底层表达式引擎
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class JexlExpressionParser implements ExpressionParser {



    private static final String PARSER_NAME = "JEXL";
    private static final String PARSER_VERSION = "3.0";
    private static final String SUPPORTED_SYNTAX = "Apache Commons JEXL Expression Language";

    private final JexlEngine jexlEngine;
    private final Map<String, Object> configuration;

    /**
     * 使用默认配置创建解析器
     */
    public JexlExpressionParser() {
        this(createDefaultJexlEngine());
    }

    /**
     * 使用指定的 JEXL 引擎创建解析器
     * 
     * @param jexlEngine JEXL 引擎
     */
    public JexlExpressionParser(JexlEngine jexlEngine) {
        this.jexlEngine = Objects.requireNonNull(jexlEngine, "JexlEngine cannot be null");
        this.configuration = new HashMap<>();
        initializeConfiguration();
    }

    /**
     * 使用配置创建解析器
     * 
     * @param configuration 配置映射
     */
    public JexlExpressionParser(Map<String, Object> configuration) {
        this.configuration = new HashMap<>(configuration != null ? configuration : new HashMap<>());
        this.jexlEngine = createJexlEngineFromConfiguration(this.configuration);
    }

    @Override
    public Expression parse(String expressionString) throws ExpressionException {
        if (expressionString == null || expressionString.trim().isEmpty()) {
            throw new ExpressionException("Expression string cannot be null or empty", expressionString);
        }

        try {
            JexlExpression jexlExpression = jexlEngine.createExpression(expressionString);
            log.debug("Successfully parsed expression: {}", expressionString);
            return new JexlExpressionWrapper(expressionString, jexlExpression);
        } catch (JexlException e) {
            log.error("Failed to parse expression: {}", expressionString, e);
            
            // 提取错误位置信息
            int position = extractErrorPosition(e);
            String errorCode = extractErrorCode(e);
            
            throw new ExpressionException(
                "Failed to parse expression: " + e.getMessage(),
                expressionString,
                position,
                errorCode,
                e
            );
        } catch (Exception e) {
            log.error("Unexpected error parsing expression: {}", expressionString, e);
            throw new ExpressionException(
                "Unexpected error: " + e.getMessage(),
                expressionString,
                e
            );
        }
    }

    @Override
    public boolean validateSyntax(String expressionString) {
        if (expressionString == null || expressionString.trim().isEmpty()) {
            return false;
        }

        try {
            jexlEngine.createExpression(expressionString);
            return true;
        } catch (Exception e) {
            log.debug("Expression syntax validation failed: {}", expressionString, e);
            return false;
        }
    }

    @Override
    public String getSupportedSyntax() {
        return SUPPORTED_SYNTAX;
    }

    @Override
    public String getParserName() {
        return PARSER_NAME;
    }

    @Override
    public String getParserVersion() {
        return PARSER_VERSION;
    }

    @Override
    public boolean supportsFeature(String feature) {
        if (feature == null) {
            return false;
        }

        switch (feature.toLowerCase()) {
            case "arithmetic":
            case "comparison":
            case "logical":
            case "string":
            case "array":
            case "map":
            case "method_call":
            case "property_access":
            case "conditional":
            case "lambda":
                return true;
            case "regex":
            case "date":
            case "math_functions":
                return true;
            default:
                return false;
        }
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        if (configuration != null) {
            this.configuration.clear();
            this.configuration.putAll(configuration);
        }
    }

    @Override
    public Expression precompile(String expressionString) throws ExpressionException {
        // JEXL 表达式本身就是预编译的
        return parse(expressionString);
    }

    /**
     * 获取 JEXL 引擎
     * 
     * @return JEXL 引擎
     */
    public JexlEngine getJexlEngine() {
        return jexlEngine;
    }

    /**
     * 创建默认的 JEXL 引擎
     * 
     * @return JEXL 引擎
     */
    private static JexlEngine createDefaultJexlEngine() {
        return new JexlBuilder()
                .cache(512)  // 启用表达式缓存
                .strict(false)  // 非严格模式
                .silent(false)  // 非静默模式
                .safe(true)  // 安全模式
                .create();
    }

    /**
     * 从配置创建 JEXL 引擎
     * 
     * @param config 配置映射
     * @return JEXL 引擎
     */
    private static JexlEngine createJexlEngineFromConfiguration(Map<String, Object> config) {
        JexlBuilder builder = new JexlBuilder();

        // 缓存大小
        Object cacheSize = config.get("cacheSize");
        if (cacheSize instanceof Number) {
            builder.cache(((Number) cacheSize).intValue());
        } else {
            builder.cache(512);
        }

        // 严格模式
        Object strict = config.get("strict");
        if (strict instanceof Boolean) {
            builder.strict((Boolean) strict);
        } else {
            builder.strict(false);
        }

        // 静默模式
        Object silent = config.get("silent");
        if (silent instanceof Boolean) {
            builder.silent((Boolean) silent);
        } else {
            builder.silent(false);
        }

        // 安全模式
        Object safe = config.get("safe");
        if (safe instanceof Boolean) {
            builder.safe((Boolean) safe);
        } else {
            builder.safe(true);
        }

        return builder.create();
    }

    /**
     * 初始化配置
     */
    private void initializeConfiguration() {
        configuration.put("cacheSize", 512);
        configuration.put("strict", false);
        configuration.put("silent", false);
        configuration.put("safe", true);
        configuration.put("parserName", PARSER_NAME);
        configuration.put("parserVersion", PARSER_VERSION);
        configuration.put("supportedSyntax", SUPPORTED_SYNTAX);
    }

    /**
     * 提取错误位置
     * 
     * @param exception JEXL 异常
     * @return 错误位置，如果无法确定则返回 -1
     */
    private int extractErrorPosition(JexlException exception) {
        try {
            // 尝试从异常信息中提取位置信息
            if (exception.getInfo() != null) {
                return exception.getInfo().getColumn();
            }
        } catch (Exception e) {
            log.debug("Failed to extract error position", e);
        }
        return -1;
    }

    /**
     * 提取错误代码
     * 
     * @param exception JEXL 异常
     * @return 错误代码
     */
    private String extractErrorCode(JexlException exception) {
        if (exception instanceof JexlException.Parsing) {
            return "PARSING_ERROR";
        } else if (exception instanceof JexlException.Variable) {
            return "VARIABLE_ERROR";
        } else if (exception instanceof JexlException.Method) {
            return "METHOD_ERROR";
        } else if (exception instanceof JexlException.Property) {
            return "PROPERTY_ERROR";
        } else {
            return "UNKNOWN_ERROR";
        }
    }

    @Override
    public String toString() {
        return "JexlExpressionParser{" +
                "name='" + PARSER_NAME + '\'' +
                ", version='" + PARSER_VERSION + '\'' +
                ", cacheSize=" + configuration.get("cacheSize") +
                ", strict=" + configuration.get("strict") +
                ", safe=" + configuration.get("safe") +
                '}';
    }
}