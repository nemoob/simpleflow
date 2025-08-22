package io.github.nemoob.core.executor;

import io.github.nemoob.api.FlowContext;
import io.github.nemoob.api.StepExecutor;
import io.github.nemoob.api.model.StepDefinition;
import io.github.nemoob.api.model.StepResult;
import io.github.nemoob.expression.ExpressionFactory;
import io.github.nemoob.expression.api.ExpressionEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 独立应用的条件步骤执行器
 * 用于处理条件分支逻辑
 */
@Slf4j
public class StandaloneConditionalStepExecutor implements StepExecutor {

    private final ExpressionEngine expressionEngine;
    
    public StandaloneConditionalStepExecutor() {
        // 默认使用JEXL表达式引擎
        this.expressionEngine = ExpressionFactory.getDefaultEngine();
    }
    
    public StandaloneConditionalStepExecutor(ExpressionEngine expressionEngine) {
        this.expressionEngine = expressionEngine;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(FlowContext context) {
        try {
            // 从上下文获取当前步骤ID，然后获取步骤定义
            String currentStepId = context.getCurrentStepId().orElse(null);
            if (currentStepId == null) {
                return StepResult.failure("unknown", "unknown", "No current step ID found in context");
            }
            
            // 这里需要从某个地方获取步骤定义，暂时使用参数传递
            // 在实际实现中，可能需要从流程定义中获取
            StepDefinition currentStep = (StepDefinition) context.get("currentStepDefinition").orElse(null);
            if (currentStep == null) {
                return StepResult.failure(currentStepId, "unknown", "Current step definition not found in context");
            }
            
            String condition = currentStep.getCondition();
            Map<String, Object> parameters = currentStep.getParameters();
            Map<String, Object> conditionalConfig = (Map<String, Object>) parameters.get("conditional");
            
            if (conditionalConfig == null) {
                return StepResult.failure(currentStep.getId(), currentStep.getName(), "Conditional configuration is required");
            }
            
            // 评估条件表达式
            boolean conditionResult = evaluateCondition(condition, context);
            log.info("Condition '{}' evaluated to: {}", condition, conditionResult);
            
            // 根据条件结果执行相应的步骤
            List<StepDefinition> stepsToExecute = getStepsToExecute(conditionalConfig, conditionResult, context);
            
            if (stepsToExecute == null || stepsToExecute.isEmpty()) {
                log.info("No steps to execute for condition result: {}", conditionResult);
                return StepResult.success(currentStep.getId(), currentStep.getName());
            }
            
            // 执行选定的步骤
            StepResult lastResult = null;
            for (StepDefinition step : stepsToExecute) {
                StepResult stepResult = executeStep(step, context);
                if (!stepResult.isSuccess()) {
                    return stepResult; // 如果任何步骤失败，立即返回失败结果
                }
                lastResult = stepResult;
            }
            
            // 返回最后一个步骤的结果，或者创建一个成功结果
            if (lastResult != null) {
                return lastResult;
            } else {
                return StepResult.success(currentStep.getId(), currentStep.getName());
            }
            
        } catch (Exception e) {
            log.error("Error executing conditional step", e);
            return StepResult.failure("unknown", "unknown", e);
        }
    }
    
    /**
     * 评估条件表达式
     * 
     * @param condition 条件表达式
     * @param context 流程上下文
     * @return 条件结果
     */
    private boolean evaluateCondition(String condition, FlowContext context) {
        if (condition == null || condition.trim().isEmpty()) {
            return true; // 空条件默认为true
        }
        
        try {
            Object result = expressionEngine.evaluate(condition, context.getAll());
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result != null) {
                // 非null值视为true
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.warn("Failed to evaluate condition '{}', defaulting to false", condition, e);
            return false;
        }
    }
    
    /**
     * 根据条件结果获取要执行的步骤
     * 
     * @param conditionalConfig 条件配置
     * @param conditionResult 条件结果
     * @param context 流程上下文
     * @return 要执行的步骤列表
     */
    @SuppressWarnings("unchecked")
    private List<StepDefinition> getStepsToExecute(Map<String, Object> conditionalConfig, 
                                                   boolean conditionResult, 
                                                   FlowContext context) {
        
        // 检查是否有cases配置（多条件分支）
        List<Map<String, Object>> cases = (List<Map<String, Object>>) conditionalConfig.get("cases");
        if (cases != null && !cases.isEmpty()) {
            return getStepsFromCases(cases, context);
        }
        
        // 简单的true/false分支
        if (conditionResult) {
            return (List<StepDefinition>) conditionalConfig.get("trueSteps");
        } else {
            return (List<StepDefinition>) conditionalConfig.get("falseSteps");
        }
    }
    
    /**
     * 从cases配置中获取要执行的步骤
     * 
     * @param cases cases配置
     * @param context 流程上下文
     * @return 要执行的步骤列表
     */
    @SuppressWarnings("unchecked")
    private List<StepDefinition> getStepsFromCases(List<Map<String, Object>> cases, FlowContext context) {
        for (Map<String, Object> caseConfig : cases) {
            String caseCondition = (String) caseConfig.get("condition");
            if (evaluateCondition(caseCondition, context)) {
                return (List<StepDefinition>) caseConfig.get("steps");
            }
        }
        
        // 如果没有匹配的case，返回默认步骤
        StepDefinition currentStep = (StepDefinition) context.get("currentStepDefinition").orElse(null);
        if (currentStep == null) {
            return null;
        }
        Map<String, Object> conditionalConfig = (Map<String, Object>) currentStep.getParameters().get("conditional");
        return (List<StepDefinition>) conditionalConfig.get("defaultSteps");
    }
    
    /**
     * 执行单个步骤
     * 
     * @param step 步骤定义
     * @param context 流程上下文
     * @return 步骤执行结果
     */
    private StepResult executeStep(StepDefinition step, FlowContext context) {
        try {
            // 设置当前步骤ID
            context.setCurrentStepId(step.getId());
            context.set("currentStepDefinition", step);
            
            // 根据步骤类型获取执行器
            String executorClass = step.getExecutorClass();
            if (executorClass == null) {
                // 根据bean名称获取实际对象，判断其类型来选择执行器
                executorClass = determineExecutorByBeanType(step);
            }
            
            // 创建执行器实例
            Class<?> clazz = Class.forName(executorClass);
            StepExecutor executor = (StepExecutor) clazz.getDeclaredConstructor().newInstance();
            
            // 执行步骤
            return executor.execute(context);
            
        } catch (Exception e) {
            log.error("Failed to execute step: {}", step.getId(), e);
            return StepResult.failure(step.getId(), step.getName(), e);
        }
    }
    
    /**
     * 根据bean类型确定执行器类
     * 
     * @param stepDefinition 步骤定义
     * @return 执行器类名
     */
    private String determineExecutorByBeanType(StepDefinition stepDefinition) {
        Map<String, Object> parameters = stepDefinition.getParameters();
        if (parameters == null) {
            throw new IllegalArgumentException("Step parameters cannot be null for step: " + stepDefinition.getId());
        }
        
        // 获取bean名称
        String beanName = (String) parameters.get("bean");
        String nodeName = (String) parameters.get("node");
        
        // 优先使用node参数（新的ExecutableNode/ConditionNode方式）
        if (nodeName != null && !nodeName.trim().isEmpty()) {
            return determineExecutorByNodeType(nodeName);
        }
        
        // 兼容旧的bean参数方式
        if (beanName != null && !beanName.trim().isEmpty()) {
            return determineExecutorByBeanName(beanName);
        }
        
        // 如果都没有，根据步骤类型选择默认执行器
        if (stepDefinition.getType() == StepDefinition.StepType.SERVICE) {
            return "io.github.nemoob.core.executor.StandaloneBeanStepExecutor";
        } else if (stepDefinition.getType() == StepDefinition.StepType.CONDITIONAL) {
            return "io.github.nemoob.core.executor.StandaloneConditionalStepExecutor";
        } else {
            throw new IllegalArgumentException("Unsupported step type: " + stepDefinition.getType());
        }
    }
    
    /**
     * 根据节点名称确定执行器类（新的ExecutableNode/ConditionNode方式）
     * 
     * @param nodeName 节点名称
     * @return 执行器类名
     */
    private String determineExecutorByNodeType(String nodeName) {
        StandaloneStepExecutorRegistry registry = StandaloneStepExecutorRegistry.getInstance();
        
        // 检查是否为ExecutableNode
        if (registry.hasExecutableNode(nodeName)) {
            return "io.github.nemoob.core.executor.ExecutableNodeStepExecutor";
        }
        
        // 检查是否为ConditionNode
        if (registry.hasConditionNode(nodeName)) {
            return "io.github.nemoob.core.executor.ConditionNodeStepExecutor";
        }
        
        throw new IllegalArgumentException("Node not found in registry: " + nodeName);
    }
    
    /**
     * 根据bean名称确定执行器类（兼容旧方式）
     * 
     * @param beanName bean名称
     * @return 执行器类名
     */
    private String determineExecutorByBeanName(String beanName) {
        StandaloneStepExecutorRegistry registry = StandaloneStepExecutorRegistry.getInstance();
        
        // 获取bean实例
        Object beanInstance = registry.getBean(beanName);
        if (beanInstance == null) {
            throw new IllegalArgumentException("Bean not found in registry: " + beanName);
        }
        
        // 检查bean类型
        if (beanInstance instanceof io.github.nemoob.api.ExecutableNode) {
            return "io.github.nemoob.core.executor.ExecutableNodeStepExecutor";
        } else if (beanInstance instanceof io.github.nemoob.api.ConditionNode) {
            return "io.github.nemoob.core.executor.ConditionNodeStepExecutor";
        } else {
            // 默认使用StandaloneBeanStepExecutor处理其他类型的bean
            return "io.github.nemoob.core.executor.StandaloneBeanStepExecutor";
        }
    }
}