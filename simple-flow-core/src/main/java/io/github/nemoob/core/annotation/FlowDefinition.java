package io.github.nemoob.core.annotation;

import java.lang.annotation.*;

/**
 * 流程定义注解
 * 
 * 用于标记一个类为流程定义，支持通过注解方式配置流程
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowDefinition {
    
    /**
     * 流程ID，如果不指定则使用类名
     * 
     * @return 流程ID
     */
    String id() default "";
    
    /**
     * 流程名称
     * 
     * @return 流程名称
     */
    String name() default "";
    
    /**
     * 流程描述
     * 
     * @return 流程描述
     */
    String description() default "";
    
    /**
     * 流程版本
     * 
     * @return 流程版本
     */
    String version() default "1.0.0";
    
    /**
     * 是否启用并行执行
     * 
     * @return 是否启用并行执行
     */
    boolean enableParallel() default false;
    
    /**
     * 最大并行度
     * 
     * @return 最大并行度
     */
    int maxParallelism() default 10;
    
    /**
     * 超时时间（毫秒）
     * 
     * @return 超时时间
     */
    long timeout() default 30000L;
}