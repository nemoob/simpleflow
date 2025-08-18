package com.simpleflow.core.annotation;

import java.lang.annotation.*;

/**
 * 启用注解流程配置
 * 
 * 用于标记启用基于注解的流程配置，通常用在配置类或主类上
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableAnnotationFlow {
    
    /**
     * 扫描的包路径，如果不指定则扫描当前包及其子包
     * 
     * @return 包路径数组
     */
    String[] basePackages() default {};
    
    /**
     * 扫描的基础类，将扫描这些类所在的包及其子包
     * 
     * @return 基础类数组
     */
    Class<?>[] basePackageClasses() default {};
    
    /**
     * 是否启用自动配置
     * 
     * @return 是否启用自动配置
     */
    boolean enableAutoConfiguration() default true;
    
    /**
     * 线程池核心线程数
     * 
     * @return 核心线程数
     */
    int corePoolSize() default 5;
    
    /**
     * 线程池最大线程数
     * 
     * @return 最大线程数
     */
    int maximumPoolSize() default 20;
    
    /**
     * 线程池队列容量
     * 
     * @return 队列容量
     */
    int queueCapacity() default 100;
}