package io.github.nemoob.core.example;

import io.github.nemoob.api.FlowEngine;
import io.github.nemoob.api.model.FlowResult;
import io.github.nemoob.core.annotation.AnnotationFlowConfigurationManager;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 注解流程示例
 * 
 * 演示如何使用注解方式配置和执行流程
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class AnnotationFlowExample {
    
    public static void main(String[] args) {
        log.info("开始注解流程示例");
        
        try {
            // 创建配置管理器
            AnnotationFlowConfigurationManager configManager = 
                new AnnotationFlowConfigurationManager("io.github.nemoob.core.example");
            
            // 初始化
            FlowEngine flowEngine = configManager.initialize();
            
            // 准备执行参数
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", "张三");
            variables.put("age", 25);
            variables.put("score", 85);
            
            // 执行流程
            FlowResult result = flowEngine.execute("UserProcessFlow2", variables);
            
            // 输出结果
            log.info("流程执行结果: {}", result.isSuccess());
            log.info("流程输出数据: {}", result.getOutputData());
            
            // 销毁资源
//            configManager.destroy();
            
        } catch (Exception e) {
            log.error("执行注解流程示例时发生错误", e);
        }
        
        log.info("注解流程示例结束");
    }

}