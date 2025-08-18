package com.simpleflow.core.annotation;

import com.simpleflow.api.FlowEngine;
import com.simpleflow.core.config.ThreadPoolConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * 注解流程工厂
 * 
 * 提供便捷的方法来创建和配置基于注解的流程引擎
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class AnnotationFlowFactory {
    
    private static AnnotationFlowConfigurationManager globalConfigManager;
    
    /**
     * 创建注解流程配置管理器
     * 
     * @param basePackages 扫描的基础包
     * @return 配置管理器
     */
    public static AnnotationFlowConfigurationManager createConfigurationManager(String... basePackages) {
        return new AnnotationFlowConfigurationManager(basePackages);
    }
    
    /**
     * 创建注解流程配置管理器（带线程池配置）
     * 
     * @param threadPoolConfig 线程池配置
     * @param basePackages 扫描的基础包
     * @return 配置管理器
     */
    public static AnnotationFlowConfigurationManager createConfigurationManager(
            ThreadPoolConfig threadPoolConfig, String... basePackages) {
        return new AnnotationFlowConfigurationManager(threadPoolConfig, basePackages);
    }
    
    /**
     * 创建并初始化流程引擎
     * 
     * @param basePackages 扫描的基础包
     * @return 流程引擎
     */
    public static FlowEngine createFlowEngine(String... basePackages) {
        AnnotationFlowConfigurationManager configManager = createConfigurationManager(basePackages);
        return configManager.initialize();
    }
    
    /**
     * 创建并初始化流程引擎（带线程池配置）
     * 
     * @param threadPoolConfig 线程池配置
     * @param basePackages 扫描的基础包
     * @return 流程引擎
     */
    public static FlowEngine createFlowEngine(ThreadPoolConfig threadPoolConfig, String... basePackages) {
        AnnotationFlowConfigurationManager configManager = 
            createConfigurationManager(threadPoolConfig, basePackages);
        return configManager.initialize();
    }
    
    /**
     * 设置全局配置管理器
     * 
     * @param configManager 配置管理器
     */
    public static void setGlobalConfigurationManager(AnnotationFlowConfigurationManager configManager) {
        globalConfigManager = configManager;
        log.info("设置全局注解流程配置管理器");
    }
    
    /**
     * 获取全局配置管理器
     * 
     * @return 配置管理器
     */
    public static AnnotationFlowConfigurationManager getGlobalConfigurationManager() {
        return globalConfigManager;
    }
    
    /**
     * 获取全局流程引擎
     * 
     * @return 流程引擎
     */
    public static FlowEngine getGlobalFlowEngine() {
        if (globalConfigManager == null) {
            throw new IllegalStateException("全局配置管理器未设置，请先调用 setGlobalConfigurationManager");
        }
        return globalConfigManager.getFlowEngine();
    }
    
    /**
     * 初始化全局配置（使用默认配置）
     * 
     * @param basePackages 扫描的基础包
     * @return 流程引擎
     */
    public static FlowEngine initializeGlobal(String... basePackages) {
        if (globalConfigManager != null) {
            log.warn("全局配置管理器已存在，将销毁现有配置并重新初始化");
            globalConfigManager.destroy();
        }
        
        globalConfigManager = createConfigurationManager(basePackages);
        return globalConfigManager.initialize();
    }
    
    /**
     * 初始化全局配置（带线程池配置）
     * 
     * @param threadPoolConfig 线程池配置
     * @param basePackages 扫描的基础包
     * @return 流程引擎
     */
    public static FlowEngine initializeGlobal(ThreadPoolConfig threadPoolConfig, String... basePackages) {
        if (globalConfigManager != null) {
            log.warn("全局配置管理器已存在，将销毁现有配置并重新初始化");
            globalConfigManager.destroy();
        }
        
        globalConfigManager = createConfigurationManager(threadPoolConfig, basePackages);
        return globalConfigManager.initialize();
    }
    
    /**
     * 销毁全局配置
     */
    public static void destroyGlobal() {
        if (globalConfigManager != null) {
            globalConfigManager.destroy();
            globalConfigManager = null;
            log.info("全局注解流程配置已销毁");
        }
    }
    
    /**
     * 检查全局配置是否已初始化
     * 
     * @return 是否已初始化
     */
    public static boolean isGlobalInitialized() {
        return globalConfigManager != null;
    }
    
    /**
     * 从类路径自动检测包名
     * 
     * @param clazz 参考类
     * @return 包名
     */
    public static String detectPackage(Class<?> clazz) {
        return clazz.getPackage().getName();
    }
    
    /**
     * 从多个类路径自动检测包名
     * 
     * @param classes 参考类数组
     * @return 包名数组
     */
    public static String[] detectPackages(Class<?>... classes) {
        Set<String> packages = new HashSet<>();
        for (Class<?> clazz : classes) {
            packages.add(clazz.getPackage().getName());
        }
        return packages.toArray(new String[0]);
    }
    
    /**
     * 创建默认线程池配置
     * 
     * @return 线程池配置
     */
    public static ThreadPoolConfig createDefaultThreadPoolConfig() {
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.setCoreSize(5);
        config.setMaxSize(20);
        config.setKeepAliveSeconds(60);
        config.setQueueCapacity(100);
        return config;
    }
    
    /**
     * 创建自定义线程池配置
     * 
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param queueCapacity 队列容量
     * @return 线程池配置
     */
    public static ThreadPoolConfig createThreadPoolConfig(int corePoolSize, int maximumPoolSize, int queueCapacity) {
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.setCoreSize(corePoolSize);
        config.setMaxSize(maximumPoolSize);
        config.setQueueCapacity(queueCapacity);
        return config;
    }
}