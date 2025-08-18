package com.simpleflow.core.annotation;

import com.simpleflow.api.FlowEngine;
import com.simpleflow.api.model.FlowDefinition;
import com.simpleflow.core.config.ThreadPoolConfig;
import com.simpleflow.core.engine.DefaultFlowEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注解流程配置管理器
 * 
 * 负责管理基于注解的流程配置，提供注解驱动的流程引擎初始化
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class AnnotationFlowConfigurationManager {
    
    private final AnnotationFlowProcessor processor;
    private final Map<String, Object> beanRegistry = new HashMap<>();
    private final ThreadPoolConfig threadPoolConfig;
    private FlowEngine flowEngine;
    private ExecutorService executorService;
    private boolean initialized = false;
    
    public AnnotationFlowConfigurationManager(String... basePackages) {
        this.processor = new AnnotationFlowProcessor(basePackages);
        this.threadPoolConfig = new ThreadPoolConfig();
    }
    
    public AnnotationFlowConfigurationManager(ThreadPoolConfig threadPoolConfig, String... basePackages) {
        this.processor = new AnnotationFlowProcessor(basePackages);
        this.threadPoolConfig = threadPoolConfig != null ? threadPoolConfig : new ThreadPoolConfig();
    }
    
    /**
     * 初始化注解配置
     * 
     * @return 流程引擎
     */
    public FlowEngine initialize() {
        if (initialized) {
            log.warn("注解配置管理器已经初始化，跳过重复初始化");
            return flowEngine;
        }
        
        log.info("开始初始化注解流程配置管理器");
        
        try {
            // 初始化线程池
            initializeThreadPool();
            
            // 处理注解配置
            Map<String, FlowDefinition> flowDefinitions = processor.processAnnotations();
            
            // 创建流程引擎
            flowEngine = new DefaultFlowEngine(executorService);
            
            // 注册流程定义
            for (Map.Entry<String, FlowDefinition> entry : flowDefinitions.entrySet()) {
                flowEngine.registerFlow(entry.getValue());
                log.info("注册流程定义: {}", entry.getKey());
            }
            
            // 实例化并注册Bean
            instantiateAndRegisterBeans(flowDefinitions);
            
            initialized = true;
            log.info("注解流程配置管理器初始化完成，共注册 {} 个流程定义", flowDefinitions.size());
            
            return flowEngine;
            
        } catch (Exception e) {
            log.error("初始化注解流程配置管理器时发生错误", e);
            throw new RuntimeException("Failed to initialize annotation flow configuration manager", e);
        }
    }
    
    /**
     * 初始化线程池
     */
    private void initializeThreadPool() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "SimpleFlow-AnnotationConfig-" + threadNumber.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };
        
        this.executorService = Executors.newFixedThreadPool(threadPoolConfig.getCoreSize(), threadFactory);
        log.info("初始化线程池，核心线程数: {}", threadPoolConfig.getCoreSize());
    }
    
    /**
     * 实例化并注册Bean
     * 
     * @param flowDefinitions 流程定义
     */
    private void instantiateAndRegisterBeans(Map<String, FlowDefinition> flowDefinitions) {
        Set<String> beanClassNames = new HashSet<>();
        
        // 收集所有需要实例化的Bean类名
        for (FlowDefinition flowDefinition : flowDefinitions.values()) {
            if (flowDefinition.getSteps() != null) {
                for (com.simpleflow.api.model.StepDefinition step : flowDefinition.getSteps()) {
                    if (step.getParameters() != null && step.getParameters().containsKey("bean")) {
                        String beanClassName = (String) step.getParameters().get("bean");
                        beanClassNames.add(beanClassName);
                    }
                }
            }
        }
        
        // 实例化Bean
        for (String className : beanClassNames) {
            try {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.newInstance();
                
                // 注册到本地beanRegistry
                beanRegistry.put(className, instance);
                
                // 同时以类名（不含包名）注册
                String simpleName = clazz.getSimpleName();
                if (!beanRegistry.containsKey(simpleName)) {
                    beanRegistry.put(simpleName, instance);
                }
                
                // 注册到StandaloneStepExecutorRegistry（这是关键！）
                com.simpleflow.core.executor.StandaloneStepExecutorRegistry.getInstance().registerBean(className, instance);
                com.simpleflow.core.executor.StandaloneStepExecutorRegistry.getInstance().registerBean(simpleName, instance);
                
                // 同时以流程定义ID注册（如果这个类是流程定义类）
                for (FlowDefinition flowDef : flowDefinitions.values()) {
                    if (flowDef.getSteps() != null && !flowDef.getSteps().isEmpty()) {
                        String firstStepBean = (String) flowDef.getSteps().get(0).getParameters().get("bean");
                        if (className.equals(firstStepBean)) {
                            beanRegistry.put(flowDef.getId(), instance);
                            com.simpleflow.core.executor.StandaloneStepExecutorRegistry.getInstance().registerBean(flowDef.getId(), instance);
                            log.debug("注册流程定义Bean: {} -> {}", flowDef.getId(), instance.getClass().getSimpleName());
                        }
                    }
                }
                
                log.debug("实例化并注册Bean: {} -> {}", className, instance.getClass().getSimpleName());
                
            } catch (Exception e) {
                log.error("实例化Bean {} 时发生错误", className, e);
            }
        }
        
        log.info("共实例化并注册 {} 个Bean", beanRegistry.size());
    }
    
    /**
     * 注册Bean实例
     * 
     * @param name Bean名称
     * @param bean Bean实例
     */
    public void registerBean(String name, Object bean) {
        beanRegistry.put(name, bean);
        log.debug("手动注册Bean: {} -> {}", name, bean.getClass().getSimpleName());
    }
    
    /**
     * 获取Bean实例
     * 
     * @param name Bean名称
     * @return Bean实例
     */
    public Object getBean(String name) {
        return beanRegistry.get(name);
    }
    
    /**
     * 获取Bean实例
     * 
     * @param clazz Bean类型
     * @param <T> Bean类型
     * @return Bean实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> clazz) {
        // 先尝试按类名查找
        Object bean = beanRegistry.get(clazz.getName());
        if (bean != null && clazz.isInstance(bean)) {
            return (T) bean;
        }
        
        // 再尝试按简单类名查找
        bean = beanRegistry.get(clazz.getSimpleName());
        if (bean != null && clazz.isInstance(bean)) {
            return (T) bean;
        }
        
        // 遍历所有Bean查找匹配的类型
        for (Object registeredBean : beanRegistry.values()) {
            if (clazz.isInstance(registeredBean)) {
                return (T) registeredBean;
            }
        }
        
        return null;
    }
    
    /**
     * 检查Bean是否存在
     * 
     * @param name Bean名称
     * @return 是否存在
     */
    public boolean containsBean(String name) {
        return beanRegistry.containsKey(name);
    }
    
    /**
     * 获取所有Bean名称
     * 
     * @return Bean名称集合
     */
    public Set<String> getBeanNames() {
        return new HashSet<>(beanRegistry.keySet());
    }
    
    /**
     * 获取流程引擎
     * 
     * @return 流程引擎
     */
    public FlowEngine getFlowEngine() {
        if (!initialized) {
            throw new IllegalStateException("Configuration manager not initialized. Call initialize() first.");
        }
        return flowEngine;
    }
    
    /**
     * 销毁资源
     */
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            log.info("线程池已关闭");
        }
        
        beanRegistry.clear();
        initialized = false;
        log.info("注解流程配置管理器已销毁");
    }
    
    /**
     * 获取线程池配置
     * 
     * @return 线程池配置
     */
    public ThreadPoolConfig getThreadPoolConfig() {
        return threadPoolConfig;
    }
}