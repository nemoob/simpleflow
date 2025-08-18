package com.simpleflow.core.config;

import com.simpleflow.api.FlowEngine;
import com.simpleflow.api.model.FlowDefinition;
import com.simpleflow.api.model.FlowResult;
import com.simpleflow.core.engine.DefaultFlowEngine;
import com.simpleflow.core.executor.StandaloneStepExecutorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 独立Java应用的流程配置管理器
 * 用于非Spring Boot环境下的流程配置和执行
 */
@Slf4j
public class StandaloneFlowConfiguration {

    private final FlowEngine flowEngine;
    private final StandaloneFlowConfigurationParser parser;
    private final Map<String, FlowDefinition> flowDefinitions = new HashMap<>();
    private final ExecutorService executorService;
    private ThreadPoolConfig threadPoolConfig;

    public StandaloneFlowConfiguration() {
        this.threadPoolConfig = new ThreadPoolConfig();
        this.executorService = threadPoolConfig.createThreadPoolExecutor();
        this.flowEngine = new DefaultFlowEngine(executorService);
        this.parser = new StandaloneFlowConfigurationParser();
    }

    public StandaloneFlowConfiguration(ExecutorService executorService) {
        this.executorService = executorService;
        this.flowEngine = new DefaultFlowEngine(executorService);
        this.parser = new StandaloneFlowConfigurationParser();
    }
    
    public StandaloneFlowConfiguration(ThreadPoolConfig threadPoolConfig) {
        this.threadPoolConfig = threadPoolConfig;
        this.executorService = threadPoolConfig.createThreadPoolExecutor();
        this.flowEngine = new DefaultFlowEngine(executorService);
        this.parser = new StandaloneFlowConfigurationParser();
    }

    /**
     * 从YAML文件加载流程配置
     * 
     * @param yamlFilePath YAML文件路径
     */
    public void loadFromYaml(String yamlFilePath) {
        System.out.println("loadFromYaml called with: " + yamlFilePath);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(yamlFilePath)) {
            System.out.println("InputStream: " + inputStream);
            if (inputStream == null) {
                throw new IllegalArgumentException("YAML file not found: " + yamlFilePath);
            }
            
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            System.out.println("Loaded config: " + config);
            log.info("Loaded YAML config: {}", config);
            
            parseConfiguration(config);
            log.info("Successfully loaded flow configurations from: {}", yamlFilePath);
        } catch (Exception e) {
            System.out.println("Exception in loadFromYaml: " + e.getMessage());
            log.error("Failed to load flow configurations from: {}", yamlFilePath, e);
            e.printStackTrace();
            throw new RuntimeException("Failed to load flow configurations", e);
        }
    }

    /**
     * 从配置Map加载流程配置
     * 
     * @param config 配置Map
     */
    public void loadFromConfig(Map<String, Object> config) {
        parseConfiguration(config);
    }

    /**
     * 解析配置
     * 
     * @param config 配置Map
     */
    @SuppressWarnings("unchecked")
    private void parseConfiguration(Map<String, Object> config) {
        System.out.println("parseConfiguration called with config: " + config);
        log.info("Parsing configuration: {}", config.keySet());
        Map<String, Object> simpleFlowConfig = (Map<String, Object>) config.get("simple-flow");
        if (simpleFlowConfig == null) {
            log.warn("No 'simple-flow' configuration found");
            return;
        }

        // 解析线程池配置（覆盖默认配置）
        ThreadPoolConfig parsedThreadPoolConfig = ThreadPoolConfig.parseFromConfig(simpleFlowConfig);
        if (parsedThreadPoolConfig != null) {
            this.threadPoolConfig = parsedThreadPoolConfig;
        }

        log.info("Simple flow config keys: {}", simpleFlowConfig.keySet());
        List<Map<String, Object>> flows = (List<Map<String, Object>>) simpleFlowConfig.get("flows");
        if (flows == null || flows.isEmpty()) {
            log.warn("No flows configuration found");
            return;
        }

        log.info("Found {} flows to parse", flows.size());
        for (Map<String, Object> flowConfig : flows) {
            try {
                log.info("Parsing flow config: {}", flowConfig);
                FlowDefinition flowDefinition = parser.parseFlow(flowConfig);
                flowDefinitions.put(flowDefinition.getId(), flowDefinition);
                log.info("Successfully loaded flow: {}", flowDefinition.getId());
            } catch (Exception e) {
                log.error("Failed to parse flow configuration: {}", flowConfig.get("id"), e);
            }
        }
    }

    /**
     * 注册步骤执行器
     * 
     * @param stepType 步骤类型
     * @param executorClass 执行器类
     */
    public void registerStepExecutor(String stepType, Class<?> executorClass) {
        StandaloneStepExecutorRegistry.getInstance().registerExecutor(stepType, executorClass);
    }

    /**
     * 注册Bean实例
     * 
     * @param beanName Bean名称
     * @param beanInstance Bean实例
     */
    public void registerBean(String beanName, Object beanInstance) {
        StandaloneStepExecutorRegistry.getInstance().registerBean(beanName, beanInstance);
    }

    /**
     * 执行指定的流程
     * 
     * @param flowId 流程ID
     * @param context 执行上下文
     * @return 流程执行结果
     */
    public FlowResult executeFlow(String flowId, Map<String, Object> context) {
        log.info("Available flows: {}", flowDefinitions.keySet());
        FlowDefinition flowDefinition = flowDefinitions.get(flowId);
        if (flowDefinition == null) {
            throw new IllegalArgumentException("Flow not found: " + flowId);
        }

        log.info("Executing flow: {} with context: {}", flowId, context);
        return flowEngine.execute(flowDefinition, context != null ? context : new HashMap<>());
    }

    /**
     * 执行指定的流程（无上下文）
     * 
     * @param flowId 流程ID
     * @return 流程执行结果
     */
    public FlowResult executeFlow(String flowId) {
        return executeFlow(flowId, new HashMap<>());
    }

    /**
     * 获取所有已加载的流程定义
     * 
     * @return 流程定义映射
     */
    public Map<String, FlowDefinition> getFlowDefinitions() {
        return new HashMap<>(flowDefinitions);
    }

    /**
     * 检查流程是否存在
     * 
     * @param flowId 流程ID
     * @return 是否存在
     */
    public boolean hasFlow(String flowId) {
        return flowDefinitions.containsKey(flowId);
    }

    /**
     * 获取流程引擎
     * 
     * @return 流程引擎
     */
    public FlowEngine getFlowEngine() {
        return flowEngine;
    }
    
    /**
     * 获取线程池配置
     */
    public ThreadPoolConfig getThreadPoolConfig() {
        return threadPoolConfig;
    }
    
    /**
     * 关闭配置管理器，释放资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            log.info("StandaloneFlowConfiguration shutdown completed");
        }
    }
}