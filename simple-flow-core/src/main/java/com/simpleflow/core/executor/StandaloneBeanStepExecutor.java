package com.simpleflow.core.executor;

import com.simpleflow.api.FlowContext;
import com.simpleflow.api.StepExecutor;
import com.simpleflow.api.StepHandler;
import com.simpleflow.api.model.StepDefinition;
import com.simpleflow.api.model.StepResult;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 独立应用的Bean步骤执行器
 * 用于执行指定Bean的方法
 */
@Slf4j
public class StandaloneBeanStepExecutor implements StepExecutor {

    @Override
    public StepResult execute(FlowContext context) {
        try {
            // 从上下文获取当前步骤定义
            String currentStepId = context.getCurrentStepId().orElse(null);
            if (currentStepId == null) {
                return StepResult.failure("unknown", "unknown", "No current step ID found in context");
            }
            
            StepDefinition currentStep = (StepDefinition) context.get("currentStepDefinition").orElse(null);
            if (currentStep == null) {
                return StepResult.failure(currentStepId, "unknown", "Current step definition not found in context");
            }
            
            Map<String, Object> parameters = currentStep.getParameters();
            String beanName = (String) parameters.get("bean");
            String methodName = (String) parameters.get("method");
            
            if (beanName == null || beanName.trim().isEmpty()) {
                return StepResult.failure(currentStep.getId(), currentStep.getName(), "Bean name is required");
            }
            
            // 如果没有配置method参数，使用默认方法
            if (methodName == null || methodName.trim().isEmpty()) {
                // 获取Bean实例先检查是否实现了StepHandler接口
                Object beanInstance = StandaloneStepExecutorRegistry.getInstance().getBean(beanName);
                if (beanInstance == null) {
                    return StepResult.failure(currentStep.getId(), currentStep.getName(), "Bean not found: " + beanName);
                }
                
                if (beanInstance instanceof StepHandler) {
                    // 如果Bean实现了StepHandler接口，使用execute方法
                    log.info("Bean '{}' implements StepHandler interface, using default execute method", beanName);
                    try {
                        StepResult result = ((StepHandler) beanInstance).execute(context);
                        log.info("Successfully executed default method: {}.execute", beanName);
                        return result;
                    } catch (Exception e) {
                        log.error("Failed to execute default method: {}.execute", beanName, e);
                        return StepResult.failure(currentStep.getId(), currentStep.getName(), e);
                    }
                } else {
                    // 如果Bean没有实现StepHandler接口，尝试查找execute方法
                    methodName = "execute";
                    log.info("Bean '{}' does not implement StepHandler interface, trying default method: execute", beanName);
                }
            }
            
            // 获取Bean实例
            Object beanInstance = StandaloneStepExecutorRegistry.getInstance().getBean(beanName);
            if (beanInstance == null) {
                log.error("Bean not found: {}", beanName);
                return StepResult.failure(currentStep.getId(), currentStep.getName(), "Bean not found: " + beanName);
            }
            
            log.info("Found bean instance: {} ({})", beanName, beanInstance.getClass().getName());
            log.info("About to invoke method: {} on bean: {}", methodName, beanName);
            
            // 查找并执行方法
            Object result = invokeMethod(beanInstance, methodName, context);
            
            log.info("Successfully executed method: {}.{}, result: {}", beanName, methodName, result);
            return StepResult.success(currentStep.getId(), currentStep.getName());
            
        } catch (Exception e) {
            log.error("Failed to execute bean step", e);
            return StepResult.failure("unknown", "unknown", e);
        }
    }
    
    /**
     * 调用Bean方法
     * 
     * @param beanInstance Bean实例
     * @param methodName 方法名
     * @param context 流程上下文
     * @return 方法执行结果
     */
    private Object invokeMethod(Object beanInstance, String methodName, FlowContext context) throws Exception {
        // 如果Bean实现了StepHandler接口，优先使用execute方法
        if (beanInstance instanceof StepHandler) {
            log.debug("Bean implements StepHandler interface, using execute method");
            return ((StepHandler) beanInstance).execute(context);
        }
        
        // 如果没有指定方法名且Bean实现了StepHandler接口，使用execute方法
        if (methodName == null || methodName.trim().isEmpty()) {
            if (beanInstance instanceof StepHandler) {
                return ((StepHandler) beanInstance).execute(context);
            } else {
                throw new IllegalArgumentException("Method name is required for beans that don't implement StepHandler interface");
            }
        }
        
        Class<?> beanClass = beanInstance.getClass();
        Method[] methods = beanClass.getMethods();
        
        // 查找匹配的方法
        Method targetMethod = null;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                targetMethod = method;
                break;
            }
        }
        
        if (targetMethod == null) {
            throw new NoSuchMethodException("Method not found: " + methodName + " in class " + beanClass.getName());
        }
        
        // 准备方法参数
        Object[] args = prepareMethodArguments(targetMethod, context);
        
        // 执行方法
        log.info("Invoking method: {} on bean: {} with args: {}", methodName, beanInstance.getClass().getSimpleName(), args);
        targetMethod.setAccessible(true);
        Object result = targetMethod.invoke(beanInstance, args);
        log.info("Method {} executed successfully, result: {}", methodName, result);
        return result;
    }
    
    /**
     * 准备方法参数
     * 
     * @param method 目标方法
     * @param context 流程上下文
     * @return 方法参数数组
     */
    private Object[] prepareMethodArguments(Method method, FlowContext context) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new Object[0];
        }
        
        List<Object> args = new ArrayList<>();
        
        for (Parameter parameter : parameters) {
            Class<?> paramType = parameter.getType();
            
            if (FlowContext.class.isAssignableFrom(paramType)) {
                // 传递FlowContext
                args.add(context);
            } else if (Map.class.isAssignableFrom(paramType)) {
                // 传递上下文数据
                args.add(context.getAll());
            } else if (String.class.isAssignableFrom(paramType)) {
                // 尝试从上下文中获取字符串参数
                String paramName = parameter.getName();
                Object value = context.getAll().get(paramName);
                args.add(value != null ? value.toString() : null);
            } else if (paramType.isPrimitive() || Number.class.isAssignableFrom(paramType)) {
                // 处理基本类型和数字类型
                String paramName = parameter.getName();
                Object value = context.getAll().get(paramName);
                args.add(convertToType(value, paramType));
            } else {
                // 尝试从上下文中获取对象
                String paramName = parameter.getName();
                Object value = context.getAll().get(paramName);
                args.add(value);
            }
        }
        
        return args.toArray();
    }
    
    /**
     * 类型转换
     * 
     * @param value 原始值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    private Object convertToType(Object value, Class<?> targetType) {
        if (value == null) {
            return getDefaultValue(targetType);
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        String stringValue = value.toString();
        
        try {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(stringValue);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(stringValue);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(stringValue);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(stringValue);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(stringValue);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to convert value '{}' to type {}, using default value", value, targetType.getName());
            return getDefaultValue(targetType);
        }
        
        return value;
    }
    
    /**
     * 获取类型的默认值
     * 
     * @param type 类型
     * @return 默认值
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return 0;
        } else if (type == long.class || type == Long.class) {
            return 0L;
        } else if (type == double.class || type == Double.class) {
            return 0.0;
        } else if (type == float.class || type == Float.class) {
            return 0.0f;
        } else if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        return null;
    }
}