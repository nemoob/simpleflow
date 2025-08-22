package io.github.nemoob.core.engine;

import io.github.nemoob.api.model.FlowDefinition;
import io.github.nemoob.api.model.FlowResult;
import io.github.nemoob.api.model.StepDefinition;
import io.github.nemoob.api.model.StepResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流程执行实例
 * 
 * 负责单个流程的执行过程管理
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class FlowExecution {



    private final String executionId;
    private final FlowDefinition flowDefinition;
    private final Map<String, Object> input;
    private final AtomicReference<String> status = new AtomicReference<>("RUNNING");
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final LocalDateTime startTime;
    private volatile LocalDateTime endTime;

    /**
     * 构造函数
     * 
     * @param executionId 执行ID
     * @param flowDefinition 流程定义
     * @param input 输入参数
     */
    public FlowExecution(String executionId, FlowDefinition flowDefinition, Map<String, Object> input) {
        this.executionId = executionId;
        this.flowDefinition = flowDefinition;
        this.input = input;
        this.startTime = LocalDateTime.now();
    }

    /**
     * 执行流程
     * 
     * @return 流程执行结果
     */
    public FlowResult execute() {
        log.info("Starting execution of flow '{}' with execution ID '{}'", 
                   flowDefinition.getId(), executionId);

        try {
            // 这里是简化的执行逻辑，实际实现会更复杂
            // 包括步骤依赖解析、并行执行、错误处理等
            
            Map<String, StepResult> stepResults = new HashMap<>();
            
            // 检查是否被停止
            if (stopped.get()) {
                status.set("CANCELLED");
                endTime = LocalDateTime.now();
                return FlowResult.builder()
                    .executionId(executionId)
                    .flowId(flowDefinition.getId())
                    .flowName(flowDefinition.getName())
                    .startTime(startTime)
                    .status(FlowResult.Status.CANCELLED)
                    .endTime(endTime)
                    .stepResults(stepResults)
                    .build();
            }

            // 模拟执行步骤
            log.info("Flow has {} steps to execute", flowDefinition.getSteps().size());
            
            for (StepDefinition step : flowDefinition.getSteps()) {
                log.info("Processing step: {} with type: {}", step.getId(), step.getType());
                
                if (stopped.get()) {
                    status.set("CANCELLED");
                    break;
                }

                // 等待暂停状态解除
                while (paused.get() && !stopped.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        stopped.set(true);
                        break;
                    }
                }

                if (stopped.get()) {
                    status.set("CANCELLED");
                    break;
                }

                // 执行步骤
                log.info("About to call executeStep for: {}", step.getId());
                StepResult stepResult = executeStep(step);
                log.info("executeStep returned for {}: {}", step.getId(), stepResult.getStatus());
                stepResults.put(step.getId(), stepResult);

                if (stepResult.getStatus() == StepResult.Status.FAILED) {
                    status.set("FAILED");
                    break;
                }
            }

            if (status.get().equals("RUNNING")) {
                status.set("SUCCESS");
            }

            endTime = LocalDateTime.now();
            
            FlowResult.Status finalStatus;
            switch (status.get()) {
                case "SUCCESS":
                    finalStatus = FlowResult.Status.SUCCESS;
                    break;
                case "FAILED":
                    finalStatus = FlowResult.Status.FAILED;
                    break;
                case "CANCELLED":
                    finalStatus = FlowResult.Status.CANCELLED;
                    break;
                default:
                    finalStatus = FlowResult.Status.FAILED;
            }

            FlowResult result = FlowResult.builder()
                .executionId(executionId)
                .flowId(flowDefinition.getId())
                .flowName(flowDefinition.getName())
                .startTime(startTime)
                .status(finalStatus)
                .endTime(endTime)
                .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                .error(status.get().equals("FAILED") ? new RuntimeException("Execution failed") : null)
                .stepResults(stepResults)
                .build();

            log.info("Completed execution of flow '{}' with execution ID '{}', status: {}", 
                       flowDefinition.getId(), executionId, result.getStatus());

            return result;

        } catch (Exception e) {
            log.error("Error during execution of flow '{}' with execution ID '{}'", 
                        flowDefinition.getId(), executionId, e);
            
            status.set("FAILED");
            endTime = LocalDateTime.now();
            
            return FlowResult.builder()
                .executionId(executionId)
                .flowId(flowDefinition.getId())
                .flowName(flowDefinition.getName())
                .status(FlowResult.Status.FAILED)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                .error(e)
                .errorMessage(e.getMessage())
                .stepResults(Collections.emptyMap())
                .build();
        }
    }

    /**
     * 执行单个步骤
     * 
     * @param stepDefinition 步骤定义
     * @return 步骤执行结果
     */
    private StepResult executeStep(StepDefinition stepDefinition) {
        log.info("Executing step '{}' in flow '{}' with type: {}", stepDefinition.getId(), flowDefinition.getId(), stepDefinition.getType());
        
        LocalDateTime stepStartTime = LocalDateTime.now();
        
        try {
            // 根据步骤类型调用相应的执行器
            String executorClass = stepDefinition.getExecutorClass();
            if (executorClass == null) {
                // 根据bean名称获取实际对象，判断其类型来选择执行器
                executorClass = determineExecutorByBeanType(stepDefinition);
            }
            
            log.info("Using executor class: {} for step: {}", executorClass, stepDefinition.getId());
            
            // 创建执行器实例
            Class<?> clazz = Class.forName(executorClass);
            io.github.nemoob.api.StepExecutor executor = (io.github.nemoob.api.StepExecutor) clazz.getDeclaredConstructor().newInstance();
            
            log.info("Created executor instance: {} for step: {}", executor.getClass().getSimpleName(), stepDefinition.getId());
            
            // 创建流程上下文
            io.github.nemoob.api.FlowContext context = createFlowContext(stepDefinition);
            
            log.info("About to execute step: {} with parameters: {}", stepDefinition.getId(), stepDefinition.getParameters());
            
            // 执行步骤
            StepResult result = executor.execute(context);
            
            log.info("Step execution completed: {} with status: {}", stepDefinition.getId(), result.getStatus());
            
            LocalDateTime stepEndTime = LocalDateTime.now();
            
            long duration = java.time.Duration.between(stepStartTime, stepEndTime).toMillis();
            return StepResult.builder()
                .stepId(stepDefinition.getId())
                .stepName(stepDefinition.getName())
                .status(result.getStatus())
                .startTime(stepStartTime)
                .endTime(stepEndTime)
                .durationMs(duration)
                .outputData(result.getOutputData() != null ? result.getOutputData() : Collections.emptyMap())
                .logs(Collections.emptyList())
                .metadata(Collections.emptyMap())
                .retryCount(0)
                .skipped(false)
                .build();
                
        } catch (Exception e) {
            log.error("Error executing step '{}' in flow '{}'", 
                        stepDefinition.getId(), flowDefinition.getId(), e);
            
            LocalDateTime failureTime = LocalDateTime.now();
            long duration = java.time.Duration.between(stepStartTime, failureTime).toMillis();
            return StepResult.builder()
                .stepId(stepDefinition.getId())
                .stepName(stepDefinition.getName())
                .status(StepResult.Status.FAILED)
                .startTime(stepStartTime)
                .endTime(failureTime)
                .durationMs(duration)
                .outputData(Collections.emptyMap())
                .error(e)
                .errorMessage(e.getMessage())
                .logs(Collections.emptyList())
                .metadata(Collections.emptyMap())
                .retryCount(0)
                .skipped(false)
                .build();
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
        } else if (stepDefinition.getType() == StepDefinition.StepType.SCRIPT_CONDITIONAL) {
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
        io.github.nemoob.core.executor.StandaloneStepExecutorRegistry registry = 
            io.github.nemoob.core.executor.StandaloneStepExecutorRegistry.getInstance();
        
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
        io.github.nemoob.core.executor.StandaloneStepExecutorRegistry registry = 
            io.github.nemoob.core.executor.StandaloneStepExecutorRegistry.getInstance();
        
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

    /**
     * 创建流程上下文
     * 
     * @param stepDefinition 步骤定义
     * @return 流程上下文
     */
    private io.github.nemoob.api.FlowContext createFlowContext(StepDefinition stepDefinition) {
        // 这里需要创建一个FlowContext实例
        // 由于没有具体的实现类，我们需要创建一个简单的实现
        SimpleFlowContext context = new SimpleFlowContext(stepDefinition, input);
        // 将当前步骤定义添加到上下文中，供StandaloneBeanStepExecutor使用
        context.set("currentStepDefinition", stepDefinition);
        return context;
    }
    
    /**
     * 简单的FlowContext实现
     */
    private static class SimpleFlowContext implements io.github.nemoob.api.FlowContext {
        private final Map<String, Object> data = new java.util.concurrent.ConcurrentHashMap<>();
        private final StepDefinition currentStep;
        private String status = "RUNNING";
        private Exception error;
        private int retryCount = 0;
        private boolean cancelled = false;
        private boolean paused = false;
        
        public SimpleFlowContext(StepDefinition currentStep, Map<String, Object> input) {
            this.currentStep = currentStep;
            if (input != null) {
                this.data.putAll(input);
            }
        }
        
        @Override
        public String getExecutionId() {
            return "simple-exec";
        }
        
        @Override
        public String getFlowId() {
            return "simple-flow";
        }
        
        @Override
        public String getFlowName() {
            return "Simple Flow";
        }
        
        @Override
        public LocalDateTime getStartTime() {
            return LocalDateTime.now();
        }
        
        @Override
        public LocalDateTime getEndTime() {
            return null;
        }
        
        @Override
        public String getStatus() {
            return status;
        }
        
        @Override
        public void setStatus(String status) {
            this.status = status;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> java.util.Optional<T> get(String key) {
            return java.util.Optional.ofNullable((T) data.get(key));
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key, T defaultValue) {
            return (T) data.getOrDefault(key, defaultValue);
        }
        
        @Override
        public void set(String key, Object value) {
            data.put(key, value);
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> java.util.Optional<T> remove(String key) {
            return java.util.Optional.ofNullable((T) data.remove(key));
        }
        
        @Override
        public boolean contains(String key) {
            return data.containsKey(key);
        }
        
        @Override
        public Map<String, Object> getAll() {
            return new HashMap<>(data);
        }
        
        @Override
        public void setAll(Map<String, Object> variables) {
            if (variables != null) {
                data.putAll(variables);
            }
        }
        
        @Override
        public void clear() {
            data.clear();
        }
        
        @Override
        public java.util.Optional<StepResult> getStepResult(String stepId) {
            return java.util.Optional.empty();
        }
        
        @Override
        public void setStepResult(String stepId, StepResult result) {
            // 简单实现，不存储步骤结果
        }
        
        @Override
        public Map<String, StepResult> getAllStepResults() {
            return new HashMap<>();
        }
        
        @Override
        public java.util.Optional<String> getCurrentStepId() {
            return java.util.Optional.ofNullable(currentStep != null ? currentStep.getId() : null);
        }
        
        @Override
        public void setCurrentStepId(String stepId) {
            // 在这个简单实现中，我们不需要修改当前步骤ID
        }
        
        @Override
        public java.util.Optional<Exception> getError() {
            return java.util.Optional.ofNullable(error);
        }
        
        @Override
        public void setError(Exception error) {
            this.error = error;
        }
        
        @Override
        public boolean hasError() {
            return error != null;
        }
        
        @Override
        public int getRetryCount() {
            return retryCount;
        }
        
        @Override
        public void incrementRetryCount() {
            retryCount++;
        }
        
        @Override
        public void resetRetryCount() {
            retryCount = 0;
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled;
        }
        
        @Override
        public void cancel() {
            cancelled = true;
        }
        
        @Override
        public boolean isPaused() {
            return paused;
        }
        
        @Override
        public void pause() {
            paused = true;
        }
        
        @Override
        public void resume() {
            paused = false;
        }
        
        @Override
        public io.github.nemoob.api.FlowContext createChildContext(String stepId) {
            return new SimpleFlowContext(currentStep, data);
        }
        
        @Override
        public java.util.Optional<io.github.nemoob.api.FlowContext> getParentContext() {
            return java.util.Optional.empty();
        }
        
        @Override
        public io.github.nemoob.api.FlowContext copy() {
            SimpleFlowContext copy = new SimpleFlowContext(this.currentStep, null);
            copy.data.putAll(this.data);
            return copy;
        }
    }

    /**
     * 停止执行
     * 
     * @return 是否成功停止
     */
    public boolean stop() {
        log.info("Stopping execution: {}", executionId);
        stopped.set(true);
        paused.set(false); // 解除暂停状态
        return true;
    }

    /**
     * 暂停执行
     * 
     * @return 是否成功暂停
     */
    public boolean pause() {
        if (stopped.get()) {
            return false;
        }
        
        log.info("Pausing execution: {}", executionId);
        paused.set(true);
        return true;
    }

    /**
     * 恢复执行
     * 
     * @return 是否成功恢复
     */
    public boolean resume() {
        if (stopped.get()) {
            return false;
        }
        
        log.info("Resuming execution: {}", executionId);
        paused.set(false);
        return true;
    }

    /**
     * 获取执行状态
     * 
     * @return 执行状态
     */
    public String getStatus() {
        if (paused.get()) {
            return "PAUSED";
        }
        return status.get();
    }

    /**
     * 获取执行ID
     * 
     * @return 执行ID
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * 获取流程定义
     * 
     * @return 流程定义
     */
    public FlowDefinition getFlowDefinition() {
        return flowDefinition;
    }

    /**
     * 获取输入参数
     * 
     * @return 输入参数
     */
    public Map<String, Object> getInput() {
        return input;
    }

    /**
     * 获取开始时间
     * 
     * @return 开始时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * 获取结束时间
     * 
     * @return 结束时间
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
}