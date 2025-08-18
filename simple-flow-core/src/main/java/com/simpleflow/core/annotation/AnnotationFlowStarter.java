package com.simpleflow.core.annotation;

import com.simpleflow.api.FlowEngine;
import com.simpleflow.api.model.FlowResult;
import com.simpleflow.core.config.ThreadPoolConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解流程启动器
 * 
 * 提供简单的静态方法来快速启动基于注解的流程
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class AnnotationFlowStarter {
    
    /**
     * 快速启动注解流程
     * 
     * @param mainClass 主类（用于检测包名）
     * @return 流程引擎
     */
    public static FlowEngine start(Class<?> mainClass) {
        String packageName = AnnotationFlowFactory.detectPackage(mainClass);
        log.info("启动注解流程，扫描包: {}", packageName);
        return AnnotationFlowFactory.initializeGlobal(packageName);
    }
    
    /**
     * 快速启动注解流程（指定包名）
     * 
     * @param basePackages 扫描的基础包
     * @return 流程引擎
     */
    public static FlowEngine start(String... basePackages) {
        log.info("启动注解流程，扫描包: {}", Arrays.toString(basePackages));
        return AnnotationFlowFactory.initializeGlobal(basePackages);
    }
    
    /**
     * 快速启动注解流程（带线程池配置）
     * 
     * @param mainClass 主类（用于检测包名）
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param queueCapacity 队列容量
     * @return 流程引擎
     */
    public static FlowEngine start(Class<?> mainClass, int corePoolSize, int maximumPoolSize, int queueCapacity) {
        String packageName = AnnotationFlowFactory.detectPackage(mainClass);
        ThreadPoolConfig config = AnnotationFlowFactory.createThreadPoolConfig(corePoolSize, maximumPoolSize, queueCapacity);
        log.info("启动注解流程，扫描包: {}，线程池配置: core={}, max={}, queue={}", 
                packageName, corePoolSize, maximumPoolSize, queueCapacity);
        return AnnotationFlowFactory.initializeGlobal(config, packageName);
    }
    
    /**
     * 快速启动注解流程（带线程池配置和指定包名）
     * 
     * @param threadPoolConfig 线程池配置
     * @param basePackages 扫描的基础包
     * @return 流程引擎
     */
    public static FlowEngine start(ThreadPoolConfig threadPoolConfig, String... basePackages) {
        log.info("启动注解流程，扫描包: {}，线程池配置: {}", Arrays.toString(basePackages), threadPoolConfig);
        return AnnotationFlowFactory.initializeGlobal(threadPoolConfig, basePackages);
    }
    
    /**
     * 停止注解流程
     */
    public static void stop() {
        log.info("停止注解流程");
        AnnotationFlowFactory.destroyGlobal();
    }
    
    /**
     * 重启注解流程
     * 
     * @param mainClass 主类（用于检测包名）
     * @return 流程引擎
     */
    public static FlowEngine restart(Class<?> mainClass) {
        log.info("重启注解流程");
        stop();
        return start(mainClass);
    }
    
    /**
     * 重启注解流程（指定包名）
     * 
     * @param basePackages 扫描的基础包
     * @return 流程引擎
     */
    public static FlowEngine restart(String... basePackages) {
        log.info("重启注解流程");
        stop();
        return start(basePackages);
    }
    
    /**
     * 获取当前流程引擎
     * 
     * @return 流程引擎
     */
    public static FlowEngine getEngine() {
        return AnnotationFlowFactory.getGlobalFlowEngine();
    }
    
    /**
     * 检查是否已启动
     * 
     * @return 是否已启动
     */
    public static boolean isStarted() {
        return AnnotationFlowFactory.isGlobalInitialized();
    }
    
    /**
     * 启动并运行指定流程
     * 
     * @param mainClass 主类
     * @param flowId 流程ID
     * @return 执行结果
     */
    public static Object startAndRun(Class<?> mainClass, String flowId) {
        FlowEngine engine = start(mainClass);
        try {
            return engine.execute(flowId, new HashMap<>());
        } finally {
            stop();
        }
    }
    
    /**
     * 启动并运行指定流程（带参数）
     * 
     * @param mainClass 主类
     * @param flowId 流程ID
     * @param parameters 流程参数
     * @return 执行结果
     */
    public static Object startAndRun(Class<?> mainClass, String flowId, Object parameters) {
        FlowEngine engine = start(mainClass);
        try {
            if (parameters instanceof Map) {
                return engine.execute(flowId, (Map<String, Object>) parameters);
            } else {
                Map<String, Object> context = new HashMap<>();
                if (parameters != null) {
                    context.put("input", parameters);
                }
                return engine.execute(flowId, context);
            }
        } catch (Exception e) {
            log.error("执行流程失败: {}", flowId, e);
            throw new RuntimeException("执行流程失败: " + flowId, e);
        }
    }
    
    /**
     * 主方法入口点（用于支持注解驱动的应用启动）
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("请提供主类名作为参数");
            System.exit(1);
        }
        
        try {
            Class<?> mainClass = Class.forName(args[0]);
            FlowEngine engine = start(mainClass);
            log.info("注解流程引擎启动成功");
            
            // 如果提供了流程ID，则执行该流程
            if (args.length > 1) {
                String flowId = args[1];
                log.info("执行流程: {}", flowId);
                FlowResult result = engine.execute(flowId, new HashMap<>());
                log.info("流程执行完成，结果: {}", result);
            }
            
        } catch (ClassNotFoundException e) {
            log.error("找不到主类: {}", args[0], e);
            System.exit(1);
        } catch (Exception e) {
            log.error("启动失败", e);
            System.exit(1);
        }
    }
    
    /**
     * 添加关闭钩子，确保应用关闭时清理资源
     */
    public static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("应用关闭，清理注解流程资源");
            stop();
        }));
    }
    
    /**
     * 启动并添加关闭钩子
     * 
     * @param mainClass 主类
     * @return 流程引擎
     */
    public static FlowEngine startWithShutdownHook(Class<?> mainClass) {
        FlowEngine engine = start(mainClass);
        addShutdownHook();
        return engine;
    }
    
    /**
     * 启动并添加关闭钩子（指定包名）
     * 
     * @param basePackages 扫描的基础包
     * @return 流程引擎
     */
    public static FlowEngine startWithShutdownHook(String... basePackages) {
        FlowEngine engine = start(basePackages);
        addShutdownHook();
        return engine;
    }
}