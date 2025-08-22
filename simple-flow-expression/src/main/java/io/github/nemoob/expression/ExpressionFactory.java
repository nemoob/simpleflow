package io.github.nemoob.expression;

import io.github.nemoob.expression.api.Expression;
import io.github.nemoob.expression.api.ExpressionEngine;
import io.github.nemoob.expression.api.ExpressionException;
import io.github.nemoob.expression.context.ExpressionContext;
import io.github.nemoob.expression.evaluator.*;
import io.github.nemoob.expression.parser.ExpressionParser;
import io.github.nemoob.expression.parser.JexlExpressionParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表达式工厂类
 * 
 * 提供表达式引擎和解析器的统一入口
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class ExpressionFactory {



    private static final String DEFAULT_ENGINE_TYPE = "jexl";
    
    private static final Map<String, ExpressionEngine> engineCache = new ConcurrentHashMap<>();
    private static final Map<String, ExpressionParser> parserCache = new ConcurrentHashMap<>();
    
    private static volatile ExpressionEngine defaultEngine;
    private static volatile ExpressionParser defaultParser;

    // 私有构造函数，防止实例化
    private ExpressionFactory() {
    }

    /**
     * 获取默认表达式引擎
     * 
     * @return 默认表达式引擎
     */
    public static ExpressionEngine getDefaultEngine() {
        if (defaultEngine == null) {
            synchronized (ExpressionFactory.class) {
                if (defaultEngine == null) {
                    defaultEngine = createEngine(DEFAULT_ENGINE_TYPE);
                }
            }
        }
        return defaultEngine;
    }

    /**
     * 获取默认表达式解析器
     * 
     * @return 默认表达式解析器
     */
    public static ExpressionParser getDefaultParser() {
        if (defaultParser == null) {
            synchronized (ExpressionFactory.class) {
                if (defaultParser == null) {
                    defaultParser = createParser(DEFAULT_ENGINE_TYPE);
                }
            }
        }
        return defaultParser;
    }

    /**
     * 创建表达式引擎
     * 
     * @param engineType 引擎类型
     * @return 表达式引擎
     */
    public static ExpressionEngine createEngine(String engineType) {
        return createEngine(engineType, null);
    }

    /**
     * 创建表达式引擎
     * 
     * @param engineType 引擎类型
     * @param configuration 配置
     * @return 表达式引擎
     */
    public static ExpressionEngine createEngine(String engineType, Map<String, Object> configuration) {
        if (engineType == null || engineType.trim().isEmpty()) {
            engineType = DEFAULT_ENGINE_TYPE;
        }

        final String finalEngineType = engineType;
        final Map<String, Object> finalConfiguration = configuration;
        String cacheKey = engineType + "_" + (configuration != null ? configuration.hashCode() : 0);
        
        return engineCache.computeIfAbsent(cacheKey, key -> {
            log.debug("Creating expression engine: {}", finalEngineType);
            
            switch (finalEngineType.toLowerCase()) {
                case "jexl":
                    if (finalConfiguration != null) {
                        return new JexlExpressionEngine(finalConfiguration);
                    } else {
                        return new JexlExpressionEngine();
                    }
                case "spel":
                    if (finalConfiguration != null) {
                        return new SpELExpressionEngine(finalConfiguration);
                    } else {
                        return new SpELExpressionEngine();
                    }
                case "groovy":
                case "groovy-script":
                    if (finalConfiguration != null) {
                        return new GroovyScriptExpressionEngine(finalConfiguration);
                    } else {
                        return new GroovyScriptExpressionEngine();
                    }
                case "kotlin":
                case "kotlin-script":
                case "kts":
                    if (finalConfiguration != null) {
                        return new KotlinScriptExpressionEngine(finalConfiguration);
                    } else {
                        return new KotlinScriptExpressionEngine();
                    }
                case "chain":
                case "chain-expression":
                    return new ChainExpressionEngine();
                default:
                    log.warn("Unknown engine type: {}, using default JEXL engine", finalEngineType);
                    return new JexlExpressionEngine();
            }
        });
    }

    /**
     * 创建表达式解析器
     * 
     * @param parserType 解析器类型
     * @return 表达式解析器
     */
    public static ExpressionParser createParser(String parserType) {
        return createParser(parserType, null);
    }

    /**
     * 创建表达式解析器
     * 
     * @param parserType 解析器类型
     * @param configuration 配置
     * @return 表达式解析器
     */
    public static ExpressionParser createParser(String parserType, Map<String, Object> configuration) {
        if (parserType == null || parserType.trim().isEmpty()) {
            parserType = DEFAULT_ENGINE_TYPE;
        }

        final String finalParserType = parserType;
        final Map<String, Object> finalConfiguration = configuration;
        String cacheKey = parserType + "_" + (configuration != null ? configuration.hashCode() : 0);
        
        return parserCache.computeIfAbsent(cacheKey, key -> {
            log.debug("Creating expression parser: {}", finalParserType);
            
            switch (finalParserType.toLowerCase()) {
                case "jexl":
                    if (finalConfiguration != null) {
                        return new JexlExpressionParser(finalConfiguration);
                    } else {
                        return new JexlExpressionParser();
                    }
                default:
                    log.warn("Unknown parser type: {}, using default JEXL parser", finalParserType);
                    return new JexlExpressionParser();
            }
        });
    }

    /**
     * 获取表达式引擎
     * 
     * @param engineType 引擎类型
     * @return 表达式引擎
     */
    public static ExpressionEngine getEngine(String engineType) {
        return createEngine(engineType);
    }

    /**
     * 获取表达式解析器
     * 
     * @param parserType 解析器类型
     * @return 表达式解析器
     */
    public static ExpressionParser getParser(String parserType) {
        return createParser(parserType);
    }

    /**
     * 设置默认表达式引擎
     * 
     * @param engine 表达式引擎
     */
    public static void setDefaultEngine(ExpressionEngine engine) {
        defaultEngine = engine;
        log.info("Default expression engine set to: {}", engine != null ? engine.getEngineName() : "null");
    }

    /**
     * 设置默认表达式解析器
     * 
     * @param parser 表达式解析器
     */
    public static void setDefaultParser(ExpressionParser parser) {
        defaultParser = parser;
        log.info("Default expression parser set to: {}", parser != null ? parser.getParserName() : "null");
    }

    /**
     * 解析表达式（使用默认解析器）
     * 
     * @param expressionString 表达式字符串
     * @return 解析后的表达式
     * @throws ExpressionException 如果解析失败
     */
    public static Expression parse(String expressionString) throws ExpressionException {
        return getDefaultParser().parse(expressionString);
    }

    /**
     * 解析并执行表达式（使用默认引擎）
     * 
     * @param expressionString 表达式字符串
     * @param context 上下文变量
     * @return 执行结果
     * @throws ExpressionException 如果解析或执行失败
     */
    public static Object evaluate(String expressionString, Map<String, Object> context) throws ExpressionException {
        return getDefaultEngine().evaluate(expressionString, context);
    }

    /**
     * 解析并执行表达式（使用默认引擎）
     * 
     * @param expressionString 表达式字符串
     * @param context 表达式上下文
     * @return 执行结果
     * @throws ExpressionException 如果解析或执行失败
     */
    public static Object evaluate(String expressionString, ExpressionContext context) throws ExpressionException {
        return evaluate(expressionString, context != null ? context.getAllVariables() : null);
    }

    /**
     * 解析并执行表达式，返回指定类型的结果（使用默认引擎）
     * 
     * @param expressionString 表达式字符串
     * @param context 上下文变量
     * @param expectedType 期望的返回类型
     * @param <T> 返回类型
     * @return 执行结果
     * @throws ExpressionException 如果解析或执行失败
     */
    public static <T> T evaluate(String expressionString, Map<String, Object> context, Class<T> expectedType) throws ExpressionException {
        return getDefaultEngine().evaluate(expressionString, context, expectedType);
    }

    /**
     * 解析并执行表达式，返回指定类型的结果（使用默认引擎）
     * 
     * @param expressionString 表达式字符串
     * @param context 表达式上下文
     * @param expectedType 期望的返回类型
     * @param <T> 返回类型
     * @return 执行结果
     * @throws ExpressionException 如果解析或执行失败
     */
    public static <T> T evaluate(String expressionString, ExpressionContext context, Class<T> expectedType) throws ExpressionException {
        return evaluate(expressionString, context != null ? context.getAllVariables() : null, expectedType);
    }

    /**
     * 验证表达式语法（使用默认解析器）
     * 
     * @param expressionString 表达式字符串
     * @return 如果语法有效则返回 true
     */
    public static boolean validateSyntax(String expressionString) {
        return getDefaultParser().validateSyntax(expressionString);
    }

    /**
     * 创建表达式上下文
     * 
     * @return 新的表达式上下文
     */
    public static ExpressionContext createContext() {
        return new ExpressionContext();
    }

    /**
     * 创建带初始变量的表达式上下文
     * 
     * @param initialVariables 初始变量
     * @return 新的表达式上下文
     */
    public static ExpressionContext createContext(Map<String, Object> initialVariables) {
        return new ExpressionContext(initialVariables);
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        engineCache.clear();
        parserCache.clear();
        log.info("Expression factory cache cleared");
    }

    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public static Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("engineCacheSize", engineCache.size());
        stats.put("parserCacheSize", parserCache.size());
        stats.put("engineCacheKeys", engineCache.keySet());
        stats.put("parserCacheKeys", parserCache.keySet());
        return stats;
    }

    /**
     * 获取支持的引擎类型
     * 
     * @return 支持的引擎类型数组
     */
    public static String[] getSupportedEngineTypes() {
        return new String[]{"jexl", "spel", "groovy", "groovy-script", "kotlin", "kotlin-script", "kts", "chain", "chain-expression"};
    }

    /**
     * 获取支持的解析器类型
     * 
     * @return 支持的解析器类型数组
     */
    public static String[] getSupportedParserTypes() {
        return new String[]{"jexl"};
    }

    /**
     * 检查是否支持指定的引擎类型
     * 
     * @param engineType 引擎类型
     * @return 如果支持则返回 true
     */
    public static boolean isSupportedEngineType(String engineType) {
        if (engineType == null) {
            return false;
        }
        
        for (String supportedType : getSupportedEngineTypes()) {
            if (supportedType.equalsIgnoreCase(engineType)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 检查是否支持指定的解析器类型
     * 
     * @param parserType 解析器类型
     * @return 如果支持则返回 true
     */
    public static boolean isSupportedParserType(String parserType) {
        if (parserType == null) {
            return false;
        }
        
        for (String supportedType : getSupportedParserTypes()) {
            if (supportedType.equalsIgnoreCase(parserType)) {
                return true;
            }
        }
        
        return false;
    }
}