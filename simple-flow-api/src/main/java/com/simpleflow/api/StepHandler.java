package com.simpleflow.api;

import com.simpleflow.api.model.StepResult;

/**
 * 步骤处理器接口
 * 
 * 所有业务步骤处理器都应该实现此接口，统一方法名为execute
 * 这样可以避免在配置中指定method参数
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public interface StepHandler {
    
    /**
     * 执行步骤逻辑
     * 
     * @param context 流程上下文
     * @return 步骤执行结果
     */
    StepResult execute(FlowContext context);
}