package io.github.nemoob.core.executor;

import io.github.nemoob.api.ConditionNode;
import io.github.nemoob.api.ExecutableNode;
import io.github.nemoob.api.ServiceRegistry;
import io.github.nemoob.core.registry.DefaultServiceRegistry;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 独立应用的步骤执行器注册表
 * 用于管理步骤执行器和Bean实例
 */
@Slf4j
public class StandaloneStepExecutorRegistry {

    private static final StandaloneStepExecutorRegistry INSTANCE = new StandaloneStepExecutorRegistry();
    
    private final Map<String, Class<?>> executorClasses = new ConcurrentHashMap<>();
    private final Map<String, Object> beanInstances = new ConcurrentHashMap<>();
    private final Map<String, ExecutableNode> executableNodes = new ConcurrentHashMap<>();
    private final Map<String, ConditionNode> conditionNodes = new ConcurrentHashMap<>();
    private final ServiceRegistry serviceRegistry = DefaultServiceRegistry.getInstance();
    
    private StandaloneStepExecutorRegistry() {
        // 注册默认执行器
        registerDefaultExecutors();
    }
    
    public static StandaloneStepExecutorRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册默认执行器
     */
    private void registerDefaultExecutors() {
        try {
            // 注册Bean步骤执行器
            registerExecutor("SERVICE", Class.forName("io.github.nemoob.core.executor.StandaloneBeanStepExecutor"));
            // 注册条件步骤执行器
            registerExecutor("CONDITIONAL", Class.forName("io.github.nemoob.core.executor.StandaloneConditionalStepExecutor"));
            // 注册ExecutableNode步骤执行器
            registerExecutor("EXECUTABLE_NODE", Class.forName("io.github.nemoob.core.executor.ExecutableNodeStepExecutor"));
            // 注册ConditionNode步骤执行器
            registerExecutor("CONDITION_NODE", Class.forName("io.github.nemoob.core.executor.ConditionNodeStepExecutor"));
            // 注册注解Bean步骤执行器
            registerExecutor("ANNOTATION_BEAN", Class.forName("io.github.nemoob.core.executor.AnnotationBeanStepExecutor"));
            log.info("Default step executors registered successfully");
        } catch (ClassNotFoundException e) {
            log.warn("Some default step executors not found, will be registered when available", e);
        }
    }
    
    /**
     * 注册步骤执行器
     * 
     * @param stepType 步骤类型
     * @param executorClass 执行器类
     */
    public void registerExecutor(String stepType, Class<?> executorClass) {
        executorClasses.put(stepType, executorClass);
        log.info("Registered step executor for type: {} -> {}", stepType, executorClass.getName());
    }
    
    /**
     * 注册Bean实例
     * 
     * @param beanName Bean名称
     * @param beanInstance Bean实例
     */
    public void registerBean(String beanName, Object beanInstance) {
        if (beanName == null || beanName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bean name cannot be null or empty");
        }
        if (beanInstance == null) {
            throw new IllegalArgumentException("Bean instance cannot be null");
        }
        
        beanInstances.put(beanName, beanInstance);
        // 同时注册到ServiceRegistry
        serviceRegistry.registerService(beanName, beanInstance);
        log.info("Registered bean: {} -> {}", beanName, beanInstance.getClass().getName());
    }
    
    /**
     * 获取Bean实例
     * 
     * @param beanName Bean名称
     * @return Bean实例
     */
    public Object getBean(String beanName) {
        // 优先从ServiceRegistry获取
        Optional<Object> service = serviceRegistry.getService(beanName);
        if (service.isPresent()) {
            return service.get();
        }
        // 兼容旧的方式
        return beanInstances.get(beanName);
    }
    
    /**
     * 获取ServiceRegistry实例
     * 
     * @return ServiceRegistry实例
     */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    
    /**
     * 检查Bean是否存在
     * 
     * @param beanName Bean名称
     * @return 是否存在
     */
    public boolean hasBean(String beanName) {
        return beanInstances.containsKey(beanName);
    }
    
    /**
     * 创建步骤执行器实例
     * 
     * @param stepType 步骤类型
     * @return 执行器实例
     */
    public Object createExecutor(String stepType) {
        Class<?> executorClass = executorClasses.get(stepType);
        if (executorClass == null) {
            throw new IllegalArgumentException("No executor registered for step type: " + stepType);
        }
        
        try {
            Constructor<?> constructor = executorClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            log.error("Failed to create executor instance for type: {}", stepType, e);
            throw new RuntimeException("Failed to create executor instance", e);
        }
    }
    
    /**
     * 获取所有注册的执行器类型
     * 
     * @return 执行器类型集合
     */
    public java.util.Set<String> getRegisteredExecutorTypes() {
        return executorClasses.keySet();
    }
    
    /**
     * 获取所有注册的Bean名称
     * 
     * @return Bean名称集合
     */
    public java.util.Set<String> getRegisteredBeanNames() {
        return beanInstances.keySet();
    }
    
    /**
     * 注册ExecutableNode实例
     * 
     * @param nodeName 节点名称
     * @param nodeInstance 节点实例
     */
    public void registerExecutableNode(String nodeName, ExecutableNode nodeInstance) {
        if (nodeName != null && !nodeName.trim().isEmpty() && nodeInstance != null) {
            executableNodes.put(nodeName, nodeInstance);
            log.info("ExecutableNode registered: {} -> {}", nodeName, nodeInstance.getClass().getSimpleName());
        }
    }
    
    /**
     * 获取ExecutableNode实例
     * 
     * @param nodeName 节点名称
     * @return ExecutableNode实例，如果不存在则返回null
     */
    public ExecutableNode getExecutableNode(String nodeName) {
        return executableNodes.get(nodeName);
    }
    
    /**
     * 注册ConditionNode实例
     * 
     * @param nodeName 节点名称
     * @param nodeInstance 节点实例
     */
    public void registerConditionNode(String nodeName, ConditionNode nodeInstance) {
        if (nodeName != null && !nodeName.trim().isEmpty() && nodeInstance != null) {
            conditionNodes.put(nodeName, nodeInstance);
            log.info("ConditionNode registered: {} -> {}", nodeName, nodeInstance.getClass().getSimpleName());
        }
    }
    
    /**
     * 获取ConditionNode实例
     * 
     * @param nodeName 节点名称
     * @return ConditionNode实例，如果不存在则返回null
     */
    public ConditionNode getConditionNode(String nodeName) {
        return conditionNodes.get(nodeName);
    }
    
    /**
     * 检查是否存在指定的ExecutableNode
     * 
     * @param nodeName 节点名称
     * @return 是否存在
     */
    public boolean hasExecutableNode(String nodeName) {
        return executableNodes.containsKey(nodeName);
    }
    
    /**
     * 检查是否存在指定的ConditionNode
     * 
     * @param nodeName 节点名称
     * @return 是否存在
     */
    public boolean hasConditionNode(String nodeName) {
        return conditionNodes.containsKey(nodeName);
    }
    
    /**
     * 获取所有注册的ExecutableNode名称
     * 
     * @return ExecutableNode名称集合
     */
    public java.util.Set<String> getRegisteredExecutableNodeNames() {
        return java.util.Collections.unmodifiableSet(executableNodes.keySet());
    }
    
    /**
     * 获取所有注册的ConditionNode名称
     * 
     * @return ConditionNode名称集合
     */
    public java.util.Set<String> getRegisteredConditionNodeNames() {
        return java.util.Collections.unmodifiableSet(conditionNodes.keySet());
    }
    
    /**
      * 清空所有注册的执行器和Bean
      */
     public void clear() {
         executorClasses.clear();
         beanInstances.clear();
         executableNodes.clear();
         conditionNodes.clear();
         registerDefaultExecutors();
         log.info("Registry cleared and default executors re-registered");
     }
}