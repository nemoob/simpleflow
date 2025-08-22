package io.github.nemoob.core.executor;

import io.github.nemoob.api.ExecutableNode;
import io.github.nemoob.api.FlowContext;
import io.github.nemoob.api.StepExecutor;
import io.github.nemoob.api.model.StepDefinition;
import io.github.nemoob.api.model.StepResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * ExecutableNode步骤执行器
 * 用于执行继承自ExecutableNode的业务节点，避免反射调用提升性能
 */
@Slf4j
public class ExecutableNodeStepExecutor implements StepExecutor {

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
            
            // 获取ExecutableNode实例
            ExecutableNode nodeInstance = StandaloneStepExecutorRegistry.getInstance().getExecutableNode(nodeName);
            if (nodeInstance == null) {
                return StepResult.failure(currentStep.getId(), currentStep.getName(), "ExecutableNode not found: " + nodeName);
            }
            
            // 准备执行上下文
            Map<String, Object> executionContext = prepareExecutionContext(context, parameters);
            
            log.info("Executing ExecutableNode: {} for step: {}", nodeName, currentStep.getId());
            
            // 验证节点执行条件
            if (!nodeInstance.validate(executionContext)) {
                return StepResult.failure(currentStep.getId(), currentStep.getName(), "Node validation failed for: " + nodeName);
            }
            
            // 执行准备工作
            nodeInstance.prepare(executionContext);
            
            // 执行节点逻辑
            nodeInstance.execute(executionContext);
            
            // 执行清理工作
            nodeInstance.cleanup(executionContext);
            
            // 将执行结果写回FlowContext
            updateFlowContextFromExecution(context, executionContext);
            
            log.info("ExecutableNode {} executed successfully for step: {}", nodeName, currentStep.getId());
            
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("result", "ExecutableNode executed successfully");
            return StepResult.success(currentStep.getId(), currentStep.getName(), outputData);
            
        } catch (Exception e) {
            log.error("Error executing ExecutableNode step", e);
            String stepId = context.getCurrentStepId().orElse("unknown");
            return StepResult.failure(stepId, "unknown", "ExecutableNode execution failed: " + e.getMessage());
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
    
    /**
     * 将执行结果更新回FlowContext
     */
    private void updateFlowContextFromExecution(FlowContext flowContext, Map<String, Object> executionContext) {
        // 更新所有非元数据的变量回FlowContext
        for (Map.Entry<String, Object> entry : executionContext.entrySet()) {
            String key = entry.getKey();
            // 跳过元数据字段
            if (!key.startsWith("_")) {
                flowContext.set(key, entry.getValue());
            }
        }
    }
}