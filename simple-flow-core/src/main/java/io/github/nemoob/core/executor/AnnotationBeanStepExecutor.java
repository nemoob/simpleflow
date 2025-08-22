package io.github.nemoob.core.executor;

import io.github.nemoob.api.FlowContext;
import io.github.nemoob.api.StepExecutor;
import io.github.nemoob.api.model.StepDefinition;
import io.github.nemoob.api.model.StepResult;
import io.github.nemoob.core.annotation.AnnotationFlowConfigurationManager;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解Bean步骤执行器
 * 
 * 用于执行基于注解配置的Bean方法步骤
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class AnnotationBeanStepExecutor implements StepExecutor {
    
    private final AnnotationFlowConfigurationManager configurationManager;
    
    public AnnotationBeanStepExecutor(AnnotationFlowConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }
    
    @Override
    public StepResult execute(FlowContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        log.debug("开始执行注解Bean步骤");
        
        try {
            // 从上下文获取当前步骤定义
            String currentStepId = context.getCurrentStepId().orElse(null);
            if (currentStepId == null) {
                return StepResult.failure("unknown", "unknown", "No current step ID found in context");
            }
            
            StepDefinition stepDefinition = (StepDefinition) context.get("currentStepDefinition").orElse(null);
            if (stepDefinition == null) {
                return StepResult.failure(currentStepId, "unknown", "Current step definition not found in context");
            }
            
            // 获取Bean和方法名
            Map<String, Object> parameters = stepDefinition.getParameters();
            String beanName = (String) parameters.get("bean");
            String methodName = (String) parameters.get("method");
            
            if (beanName == null || methodName == null) {
                return StepResult.failure(stepDefinition.getId(), stepDefinition.getName(), "Bean名称和方法名称不能为空");
            }
            
            // 获取Bean实例
            Object beanInstance = getBeanInstance(beanName);
            if (beanInstance == null) {
                return StepResult.failure(stepDefinition.getId(), stepDefinition.getName(), "找不到Bean: " + beanName);
            }
            
            // 查找并执行方法
            Method method = findMethod(beanInstance.getClass(), methodName);
            if (method == null) {
                return StepResult.failure(stepDefinition.getId(), stepDefinition.getName(), "找不到方法: " + methodName);
            }
            
            Object methodResult = invokeMethod(beanInstance, method, context);
            
            // 设置结果
            if (methodResult != null) {
                context.set(stepDefinition.getId() + "_result", methodResult);
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            log.info("注解Bean步骤执行成功: {}", stepDefinition.getId());
            
            Map<String, Object> outputData = new HashMap<>();
            if (methodResult != null) {
                outputData.put("result", methodResult);
            }
            
            return StepResult.builder()
                    .stepId(stepDefinition.getId())
                    .stepName(stepDefinition.getName())
                    .status(StepResult.Status.SUCCESS)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                    .outputData(outputData)
                    .executorName(this.getClass().getSimpleName())
                    .build();
            
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            log.error("注解Bean步骤执行失败", e);
            
            return StepResult.builder()
                    .stepId("unknown")
                    .stepName("unknown")
                    .status(StepResult.Status.FAILED)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                    .error(e)
                    .errorMessage(e.getMessage())
                    .executorName(this.getClass().getSimpleName())
                    .build();
        }
    }
    
    /**
     * 获取Bean实例
     * 
     * @param beanName Bean名称
     * @return Bean实例
     */
    private Object getBeanInstance(String beanName) {
        // 先尝试直接获取
        Object bean = configurationManager.getBean(beanName);
        if (bean != null) {
            return bean;
        }
        
        // 尝试按类名获取
        try {
            Class<?> clazz = Class.forName(beanName);
            return configurationManager.getBean(clazz);
        } catch (ClassNotFoundException e) {
            log.debug("无法找到类: {}", beanName);
        }
        
        return null;
    }
    
    /**
     * 查找方法
     * 
     * @param clazz 类
     * @param methodName 方法名
     * @return 方法
     */
    private Method findMethod(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getDeclaredMethods();
        
        // 优先查找无参方法
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                method.setAccessible(true);
                return method;
            }
        }
        
        // 查找带FlowContext参数的方法
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (FlowContext.class.isAssignableFrom(paramTypes[0])) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        
        // 查找带Map参数的方法
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (Map.class.isAssignableFrom(paramTypes[0])) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 调用方法
     * 
     * @param bean Bean实例
     * @param method 方法
     * @param context 流程上下文
     * @return 方法执行结果
     */
    private Object invokeMethod(Object bean, Method method, FlowContext context) throws Exception {
        int paramCount = method.getParameterCount();
        
        if (paramCount == 0) {
            // 无参方法
            return method.invoke(bean);
        } else if (paramCount == 1) {
            Class<?> paramType = method.getParameterTypes()[0];
            
            if (FlowContext.class.isAssignableFrom(paramType)) {
                // FlowContext参数
                return method.invoke(bean, context);
            } else if (Map.class.isAssignableFrom(paramType)) {
                // Map参数
                Map<String, Object> variables = new HashMap<>();
                if (context != null) {
                    variables.putAll(context.getAll());
                }
                return method.invoke(bean, variables);
            }
        }
        
        throw new IllegalArgumentException("不支持的方法签名: " + method.getName());
    }
    
    /**
     * 检查是否支持指定的步骤
     * 
     * @param step 步骤定义
     * @return 是否支持
     */
    public boolean supports(StepDefinition step) {
        // 检查步骤是否配置了bean和method参数
        Map<String, Object> parameters = step.getParameters();
        return parameters != null && 
               parameters.containsKey("bean") && 
               parameters.containsKey("method");
    }
}