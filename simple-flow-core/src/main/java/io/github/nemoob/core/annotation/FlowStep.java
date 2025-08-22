package io.github.nemoob.core.annotation;

import java.lang.annotation.*;

/**
 * 流程步骤注解
 * 
 * 用于标记一个方法为流程步骤，支持通过注解方式配置步骤
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowStep {
    
    /**
     * 步骤ID，如果不指定则使用方法名
     * 
     * @return 步骤ID
     */
    String id() default "";
    
    /**
     * 步骤名称
     * 
     * @return 步骤名称
     */
    String name() default "";
    
    /**
     * 步骤描述
     * 
     * @return 步骤描述
     */
    String description() default "";
    
    /**
     * 步骤类型
     * 
     * @return 步骤类型
     */
    StepType type() default StepType.SERVICE;
    
    /**
     * 步骤执行顺序，数值越小越先执行
     * 
     * @return 执行顺序
     */
    int order() default 0;
    
    /**
     * 依赖的步骤ID列表
     * 
     * @return 依赖步骤ID数组
     */
    String[] dependsOn() default {};
    
    /**
     * 条件表达式，只有当条件为true时才执行此步骤
     * 
     * @return 条件表达式
     */
    String condition() default "";
    
    /**
     * 是否异步执行
     * 
     * @return 是否异步执行
     */
    boolean async() default false;
    
    /**
     * 超时时间（毫秒）
     * 
     * @return 超时时间
     */
    long timeout() default 10000L;
    
    /**
     * 重试次数
     * 
     * @return 重试次数
     */
    int retryCount() default 0;
    
    /**
     * 重试间隔（毫秒）
     * 
     * @return 重试间隔
     */
    long retryInterval() default 1000L;
    
    /**
     * 步骤类型枚举
     */
    enum StepType {
        /**
         * 服务步骤
         */
        SERVICE,
        
        /**
         * 条件步骤
         */
        CONDITIONAL,
        
        /**
         * 简单步骤
         */
        SIMPLE
    }
}