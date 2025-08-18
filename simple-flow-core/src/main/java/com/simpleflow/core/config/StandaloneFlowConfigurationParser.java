package com.simpleflow.core.config;

import com.simpleflow.api.model.FlowDefinition;
import com.simpleflow.api.model.StepDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 独立应用的流程配置解析器
 * 用于解析YAML配置并转换为FlowDefinition对象
 */
@Slf4j
public class StandaloneFlowConfigurationParser {

    /**
     * 解析流程配置
     * 
     * @param flowConfig 流程配置Map
     * @return FlowDefinition
     */
    @SuppressWarnings("unchecked")
    public FlowDefinition parseFlow(Map<String, Object> flowConfig) {
        String id = (String) flowConfig.get("id");
        String name = (String) flowConfig.get("name");
        String description = (String) flowConfig.get("description");
        String version = (String) flowConfig.get("version");
        
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Flow id is required");
        }
        
        List<Map<String, Object>> stepsConfig = (List<Map<String, Object>>) flowConfig.get("steps");
        if (stepsConfig == null || stepsConfig.isEmpty()) {
            throw new IllegalArgumentException("Flow steps are required for flow: " + id);
        }
        
        // 解析新的配置
        String threadPoolName = (String) flowConfig.get("thread-pool-name");
        Boolean sync = (Boolean) flowConfig.get("sync");
        
        // 默认值：如果没有配置sync，默认为true（同步）
        boolean syncValue = sync != null ? sync : true;
        
        List<StepDefinition> steps = new ArrayList<>();
        for (Map<String, Object> stepConfig : stepsConfig) {
            StepDefinition step = parseStep(stepConfig);
            steps.add(step);
        }
        
        FlowDefinition definition = FlowDefinition.builder()
                .id(id)
                .name(name != null ? name : id)
                .description(description)
                .version(version != null ? version : "1.0")
                .steps(steps)
                .threadPoolName(threadPoolName)
                .sync(syncValue)
                .build();
        
        log.info("解析流程配置: id={}, threadPoolName={}, sync={}", 
                id, threadPoolName, syncValue);

        return definition;
    }

    /**
     * 解析步骤配置
     * 
     * @param stepConfig 步骤配置Map
     * @return StepDefinition
     */
    @SuppressWarnings("unchecked")
    private StepDefinition parseStep(Map<String, Object> stepConfig) {
        String id = (String) stepConfig.get("id");
        String name = (String) stepConfig.get("name");
        String type = (String) stepConfig.get("type");
        String condition = (String) stepConfig.get("condition");
        String bean = (String) stepConfig.get("bean");
        String method = (String) stepConfig.get("method");
        
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Step id is required");
        }
        
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Step type is required for step: " + id);
        }
        
        // 设置参数
        Map<String, Object> parameters = new HashMap<>();
        if (bean != null) {
            parameters.put("bean", bean);
        }
        if (method != null) {
            parameters.put("method", method);
        }
        
        // 处理条件配置
        Map<String, Object> conditionalConfig = (Map<String, Object>) stepConfig.get("conditional");
        if (conditionalConfig != null) {
            parameters.put("conditional", parseConditionalConfig(conditionalConfig));
        }
        
        StepDefinition.StepType stepType = parseStepType(type);
        String executorClass = null;
        
        // 根据节点类型直接选择执行器
        if (stepType == StepDefinition.StepType.SERVICE) {
            // 执行节点，使用Bean执行器
            executorClass = "com.simpleflow.core.executor.StandaloneBeanStepExecutor";
        } else if (stepType == StepDefinition.StepType.CONDITIONAL) {
            // 条件节点，使用条件执行器
            executorClass = "com.simpleflow.core.executor.StandaloneConditionalStepExecutor";
        }
        
        return StepDefinition.builder()
                .id(id)
                .name(name != null ? name : id)
                .type(stepType)
                .condition(condition)
                .parameters(parameters)
                .executorClass(executorClass)
                .build();
    }

    /**
     * 解析条件配置
     * 
     * @param conditionalConfig 条件配置Map
     * @return 解析后的条件配置
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConditionalConfig(Map<String, Object> conditionalConfig) {
        Map<String, Object> result = new HashMap<>();
        
        // 处理 trueSteps
        List<Map<String, Object>> trueStepsConfig = (List<Map<String, Object>>) conditionalConfig.get("trueSteps");
        if (trueStepsConfig != null) {
            List<StepDefinition> trueSteps = new ArrayList<>();
            for (Map<String, Object> stepConfig : trueStepsConfig) {
                trueSteps.add(parseStep(stepConfig));
            }
            result.put("trueSteps", trueSteps);
        }
        
        // 处理 falseSteps
        List<Map<String, Object>> falseStepsConfig = (List<Map<String, Object>>) conditionalConfig.get("falseSteps");
        if (falseStepsConfig != null) {
            List<StepDefinition> falseSteps = new ArrayList<>();
            for (Map<String, Object> stepConfig : falseStepsConfig) {
                falseSteps.add(parseStep(stepConfig));
            }
            result.put("falseSteps", falseSteps);
        }
        
        // 处理 cases
        List<Map<String, Object>> casesConfig = (List<Map<String, Object>>) conditionalConfig.get("cases");
        if (casesConfig != null) {
            List<Map<String, Object>> cases = new ArrayList<>();
            for (Map<String, Object> caseConfig : casesConfig) {
                Map<String, Object> caseResult = new HashMap<>();
                caseResult.put("condition", caseConfig.get("condition"));
                
                List<Map<String, Object>> stepsConfig = (List<Map<String, Object>>) caseConfig.get("steps");
                if (stepsConfig != null) {
                    List<StepDefinition> steps = new ArrayList<>();
                    for (Map<String, Object> stepConfig : stepsConfig) {
                        steps.add(parseStep(stepConfig));
                    }
                    caseResult.put("steps", steps);
                }
                cases.add(caseResult);
            }
            result.put("cases", cases);
        }
        
        // 处理 defaultSteps
        List<Map<String, Object>> defaultStepsConfig = (List<Map<String, Object>>) conditionalConfig.get("defaultSteps");
        if (defaultStepsConfig != null) {
            List<StepDefinition> defaultSteps = new ArrayList<>();
            for (Map<String, Object> stepConfig : defaultStepsConfig) {
                defaultSteps.add(parseStep(stepConfig));
            }
            result.put("defaultSteps", defaultSteps);
        }
        
        return result;
    }

    /**
     * 解析步骤类型
     * 
     * @param type 类型字符串
     * @return StepDefinition.StepType
     */
    private StepDefinition.StepType parseStepType(String type) {
        try {
            return StepDefinition.StepType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown step type: {}, defaulting to SIMPLE", type);
            return StepDefinition.StepType.SIMPLE;
        }
    }
}