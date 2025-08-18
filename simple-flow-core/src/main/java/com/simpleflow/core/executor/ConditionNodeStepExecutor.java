package com.simpleflow.core.executor;

import com.simpleflow.api.ConditionNode;
import com.simpleflow.api.FlowContext;
import com.simpleflow.api.StepExecutor;
import com.simpleflow.api.model.StepDefinition;
import com.simpleflow.api.model.StepResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * ConditionNode步骤执行器
 * 用于执行继承自ConditionNode的条件判断节点，避免反射调用提升性能
 */
@Slf4j
public class ConditionNodeStepExecutor implements StepExecutor {

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
            String nodeName = (String) parameters.get("node");
            
            if (nodeName == null || nodeName.trim().isEmpty()) {
                return StepResult.failure(currentStep.getId(), currentStep.getName(), "Node name is required");
            }
            
            // 获取ConditionNode实例
            ConditionNode nodeInstance = StandaloneStepExecutorRegistry.getInstance().getConditionNode(nodeName);
            if (nodeInstance == null) {
                return StepResult.failure(currentStep.getId(), currentStep.getName(), "ConditionNode not found: " + nodeName);
            }
            
            // 准备执行上下文
            Map<String, Object> executionContext = prepareExecutionContext(context, parameters);
            
            log.info("Evaluating ConditionNode: {} for step: {}", nodeName, currentStep.getId());
            
            // 验证节点执行条件
            if (!nodeInstance.validate(executionContext)) {
                return StepResult.failure(currentStep.getId(), currentStep.getName(), "Node validation failed for: " + nodeName);
            }
            
            // 执行准备工作
            nodeInstance.prepare(executionContext);
            
            // 评估条件
            boolean conditionResult = nodeInstance.evaluate(executionContext);
            
            // 将条件结果存储到上下文中
            context.set("conditionResult", conditionResult);
            context.set(currentStep.getId() + "_result", conditionResult);
            
            log.info("ConditionNode {} evaluated to: {} for step: {}", nodeName, conditionResult, currentStep.getId());
            
            // 根据条件结果创建StepResult
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("conditionResult", conditionResult);
            outputData.put("result", String.format("Condition evaluated to: %s", conditionResult));
            
            return StepResult.success(currentStep.getId(), currentStep.getName(), outputData);
            
        } catch (Exception e) {
            log.error("Error executing ConditionNode step", e);
            String stepId = context.getCurrentStepId().orElse("unknown");
            return StepResult.failure(stepId, "unknown", "ConditionNode evaluation failed: " + e.getMessage());
        }
    }
    
    /**
     * 准备执行上下文
     * 将FlowContext中的数据转换为Map<String, Object>
     */
    private Map<String, Object> prepareExecutionContext(FlowContext flowContext, Map<String, Object> parameters) {
        Map<String, Object> executionContext = new HashMap<>();
        
        // 添加FlowContext中的所有变量
        executionContext.putAll(flowContext.getAll());
        
        // 添加步骤参数
        if (parameters != null) {
            executionContext.putAll(parameters);
        }
        
        // 添加一些元数据
        executionContext.put("_executionId", flowContext.getExecutionId());
        executionContext.put("_flowId", flowContext.getFlowId());
        executionContext.put("_flowName", flowContext.getFlowName());
        executionContext.put("_currentStepId", flowContext.getCurrentStepId().orElse(null));
        executionContext.put("_startTime", flowContext.getStartTime());
        
        return executionContext;
    }
}