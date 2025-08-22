package io.github.nemoob.api;

import io.github.nemoob.api.model.StepResult;

/**
 * 步骤执行器接口
 * 
 * 定义单个步骤的执行逻辑
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@FunctionalInterface
public interface StepExecutor {

    /**
     * 执行步骤
     * 
     * @param context 流程上下文
     * @return 步骤执行结果
     * @throws Exception 执行异常
     */
    StepResult execute(FlowContext context) throws Exception;

    /**
     * 获取执行器名称
     * 
     * @return 执行器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取执行器描述
     * 
     * @return 执行器描述
     */
    default String getDescription() {
        return "Step executor: " + getName();
    }

    /**
     * 检查是否支持重试
     * 
     * @return 是否支持重试
     */
    default boolean supportsRetry() {
        return true;
    }

    /**
     * 检查是否支持并行执行
     * 
     * @return 是否支持并行执行
     */
    default boolean supportsParallel() {
        return true;
    }

    /**
     * 获取预估执行时间（毫秒）
     * 
     * @return 预估执行时间，-1表示未知
     */
    default long getEstimatedExecutionTime() {
        return -1;
    }

    /**
     * 执行前的准备工作
     * 
     * @param context 流程上下文
     * @throws Exception 准备异常
     */
    default void prepare(FlowContext context) throws Exception {
        // 默认无需准备
    }

    /**
     * 执行后的清理工作
     * 
     * @param context 流程上下文
     * @param result 执行结果
     */
    default void cleanup(FlowContext context, StepResult result) {
        // 默认无需清理
    }

    /**
     * 验证执行前提条件
     * 
     * @param context 流程上下文
     * @return 验证结果
     */
    default boolean validate(FlowContext context) {
        return true;
    }

    /**
     * 创建简单的步骤执行器
     * 
     * @param executor 执行逻辑
     * @return 步骤执行器
     */
    static StepExecutor of(StepExecutor executor) {
        return executor;
    }

    /**
     * 创建带名称的步骤执行器
     * 
     * @param name 执行器名称
     * @param executor 执行逻辑
     * @return 步骤执行器
     */
    static StepExecutor of(String name, StepExecutor executor) {
        return new StepExecutor() {
            @Override
            public StepResult execute(FlowContext context) throws Exception {
                return executor.execute(context);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    /**
     * 创建带名称和描述的步骤执行器
     * 
     * @param name 执行器名称
     * @param description 执行器描述
     * @param executor 执行逻辑
     * @return 步骤执行器
     */
    static StepExecutor of(String name, String description, StepExecutor executor) {
        return new StepExecutor() {
            @Override
            public StepResult execute(FlowContext context) throws Exception {
                return executor.execute(context);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }
}