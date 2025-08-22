package io.github.nemoob.core.annotation;

import io.github.nemoob.api.model.FlowDefinition;
import io.github.nemoob.api.model.StepDefinition;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 注解流程处理器
 * 
 * 负责扫描和解析基于注解的流程定义
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class AnnotationFlowProcessor {
    
    private final Set<String> basePackages;
    private final Map<String, FlowDefinition> flowDefinitions = new HashMap<>();
    
    public AnnotationFlowProcessor(String... basePackages) {
        this.basePackages = new HashSet<>(Arrays.asList(basePackages));
    }
    
    /**
     * 扫描并处理注解配置
     * 
     * @return 流程定义映射
     */
    public Map<String, FlowDefinition> processAnnotations() {
        log.info("开始扫描注解配置，扫描包: {}", basePackages);
        
        for (String basePackage : basePackages) {
            scanPackage(basePackage);
        }
        
        log.info("注解扫描完成，共发现 {} 个流程定义", flowDefinitions.size());
        return new HashMap<>(flowDefinitions);
    }
    
    /**
     * 扫描指定包
     * 
     * @param packageName 包名
     */
    private void scanPackage(String packageName) {
        try {
            String path = packageName.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                
                if (directory.exists() && directory.isDirectory()) {
                    scanDirectory(directory, packageName);
                }
            }
        } catch (Exception e) {
            log.error("扫描包 {} 时发生错误", packageName, e);
        }
    }
    
    /**
     * 扫描目录
     * 
     * @param directory 目录
     * @param packageName 包名
     */
    private void scanDirectory(File directory, String packageName) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                processClass(className);
            }
        }
    }
    
    /**
     * 处理类
     * 
     * @param className 类名
     */
    private void processClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            
            if (clazz.isAnnotationPresent(io.github.nemoob.core.annotation.FlowDefinition.class)) {
                processFlowDefinition(clazz);
            }
        } catch (Exception e) {
            log.debug("处理类 {} 时发生错误: {}", className, e.getMessage());
        }
    }
    
    /**
     * 处理流程定义
     * 
     * @param clazz 类
     */
    private void processFlowDefinition(Class<?> clazz) {
        io.github.nemoob.core.annotation.FlowDefinition flowAnnotation = 
            clazz.getAnnotation(io.github.nemoob.core.annotation.FlowDefinition.class);
        
        String flowId = flowAnnotation.id().isEmpty() ? clazz.getSimpleName() : flowAnnotation.id();
        String flowName = flowAnnotation.name().isEmpty() ? clazz.getSimpleName() : flowAnnotation.name();
        
        log.info("发现流程定义: {} ({})", flowName, flowId);
        
        // 处理步骤定义
        List<StepDefinition> steps = processSteps(clazz);
        
        // 构建依赖关系映射
        Map<String, List<String>> dependencies = new HashMap<>();
        for (StepDefinition step : steps) {
            Map<String, Object> params = step.getParameters();
            if (params != null && params.containsKey("dependsOn")) {
                Object dependsOnObj = params.get("dependsOn");
                List<String> dependsOnList = new ArrayList<>();
                if (dependsOnObj instanceof String[]) {
                    dependsOnList.addAll(Arrays.asList((String[]) dependsOnObj));
                } else if (dependsOnObj instanceof String) {
                    dependsOnList.add((String) dependsOnObj);
                }
                dependencies.put(step.getId(), dependsOnList);
            }
        }
        
        // 创建属性映射，将注解中的配置存储到properties中
        Map<String, Object> properties = new HashMap<>();
        properties.put("enableParallel", flowAnnotation.enableParallel());
        properties.put("maxParallelism", flowAnnotation.maxParallelism());
        properties.put("timeout", flowAnnotation.timeout());
        
        FlowDefinition flowDefinition = FlowDefinition.builder()
                .id(flowId)
                .name(flowName)
                .description(flowAnnotation.description())
                .version(flowAnnotation.version())
                .steps(steps)
                .dependencies(dependencies)
                .properties(properties)
                .build();
        
        flowDefinitions.put(flowId, flowDefinition);
    }
    
    /**
     * 处理步骤定义
     * 
     * @param clazz 类
     * @return 步骤定义列表
     */
    private List<StepDefinition> processSteps(Class<?> clazz) {
        List<StepDefinition> steps = new ArrayList<>();
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(FlowStep.class)) {
                steps.add(processFlowStep(method, clazz));
            } else if (method.isAnnotationPresent(ConditionalStep.class)) {
                steps.add(processConditionalStep(method, clazz));
            }
        }
        
        // 按order排序 - 从parameters中获取order值
        steps.sort(Comparator.comparingInt(step -> {
            Object order = step.getParameters().get("order");
            return order instanceof Integer ? (Integer) order : 0;
        }));
        
        return steps;
    }
    
    /**
     * 处理流程步骤
     * 
     * @param method 方法
     * @param clazz 类
     * @return 步骤定义
     */
    private StepDefinition processFlowStep(Method method, Class<?> clazz) {
        FlowStep stepAnnotation = method.getAnnotation(FlowStep.class);
        
        String stepId = stepAnnotation.id().isEmpty() ? method.getName() : stepAnnotation.id();
        String stepName = stepAnnotation.name().isEmpty() ? method.getName() : stepAnnotation.name();
        
        // 设置参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bean", clazz.getName());
        parameters.put("method", method.getName());
        parameters.put("order", stepAnnotation.order());
        
        StepDefinition.StepDefinitionBuilder builder = StepDefinition.builder()
                .id(stepId)
                .name(stepName)
                .description(stepAnnotation.description())
                .type(convertStepType(stepAnnotation.type()))
                .timeoutMs(stepAnnotation.timeout())
                .maxRetries(stepAnnotation.retryCount())
                .retryDelayMs(stepAnnotation.retryInterval())
                .parameters(parameters);
        
        // 设置依赖
        if (stepAnnotation.dependsOn().length > 0) {
            // 注意：StepDefinition可能没有dependsOn字段，需要通过parameters传递
            parameters.put("dependsOn", Arrays.asList(stepAnnotation.dependsOn()));
        }
        
        // 设置条件
        if (!stepAnnotation.condition().isEmpty()) {
            parameters.put("condition", stepAnnotation.condition());
        }
        
        // 设置异步标志
        if (stepAnnotation.async()) {
            parameters.put("async", true);
        }
        
        StepDefinition stepDefinition = builder.build();
        
        log.debug("处理步骤: {} ({})", stepName, stepId);
        
        return stepDefinition;
    }
    
    /**
     * 处理条件步骤
     * 
     * @param method 方法
     * @param clazz 类
     * @return 步骤定义
     */
    private StepDefinition processConditionalStep(Method method, Class<?> clazz) {
        ConditionalStep stepAnnotation = method.getAnnotation(ConditionalStep.class);
        
        String stepId = stepAnnotation.id().isEmpty() ? method.getName() : stepAnnotation.id();
        String stepName = stepAnnotation.name().isEmpty() ? method.getName() : stepAnnotation.name();
        
        // 设置参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bean", clazz.getName());
        parameters.put("method", method.getName());
        parameters.put("order", stepAnnotation.order());
        
        // 设置条件分支
        if (stepAnnotation.onTrue().length > 0) {
            parameters.put("onTrue", Arrays.asList(stepAnnotation.onTrue()));
        }
        if (stepAnnotation.onFalse().length > 0) {
            parameters.put("onFalse", Arrays.asList(stepAnnotation.onFalse()));
        }
        
        StepDefinition.StepDefinitionBuilder builder = StepDefinition.builder()
                .id(stepId)
                .name(stepName)
                .description(stepAnnotation.description())
                .type(StepDefinition.StepType.CONDITIONAL)
                .timeoutMs(stepAnnotation.timeout())
                .maxRetries(stepAnnotation.retryCount())
                .retryDelayMs(stepAnnotation.retryInterval())
                .parameters(parameters);
        
        // 设置依赖
        if (stepAnnotation.dependsOn().length > 0) {
            // 注意：StepDefinition可能没有dependsOn字段，需要通过parameters传递
            parameters.put("dependsOn", Arrays.asList(stepAnnotation.dependsOn()));
        }
        
        StepDefinition stepDefinition = builder.build();
        
        log.debug("处理条件步骤: {} ({})", stepName, stepId);
        
        return stepDefinition;
    }
    
    /**
     * 转换步骤类型
     * 
     * @param annotationType 注解类型
     * @return 步骤类型
     */
    private StepDefinition.StepType convertStepType(FlowStep.StepType annotationType) {
        switch (annotationType) {
            case SERVICE:
                return StepDefinition.StepType.SERVICE;
            case CONDITIONAL:
                return StepDefinition.StepType.CONDITIONAL;
            case SIMPLE:
                return StepDefinition.StepType.SIMPLE;
            default:
                return StepDefinition.StepType.SIMPLE;
        }
    }
}