package com.simpleflow.core.annotation;

import java.lang.annotation.*;

/**
 * 条件步骤注解
 * 
 * 用于标记一个方法为条件判断步骤，该方法应该返回boolean值
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConditionalStep {
    
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
     * 当条件为true时执行的步骤ID列表
     * 
     * @return 步骤ID数组
     */
    String[] onTrue() default {};
    
    /**
     * 当条件为false时执行的步骤ID列表
     * 
     * @return 步骤ID数组
     */
    String[] onFalse() default {};
    
    /**
     * 超时时间（毫秒）
     * 
     * @return 超时时间
     */
    long timeout() default 5000L;
    
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
}