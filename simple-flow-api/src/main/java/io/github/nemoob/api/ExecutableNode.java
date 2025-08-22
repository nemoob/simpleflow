package io.github.nemoob.api;

import java.util.Map;

/**
 * 执行节点基类
 *
 * 所有业务执行节点都应该继承此类，提供统一的执行接口
 * 使用{@code Map<String, Object>}作为上下文参数，避免复杂的FlowContext依赖
 *
 * @author Simple Flow Team
 * @since 1.0.0
 */
public abstract class ExecutableNode {

    /**
     * 执行节点逻辑
     *
     * @param context 执行上下文，包含所有必要的参数和数据
     */
    public abstract void execute(Map<String, Object> context);

    /**
     * 获取节点名称
     *
     * @return 节点名称
     */
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取节点描述
     *
     * @return 节点描述
     */
    public String getDescription() {
        return "Executable node: " + getName();
    }

    /**
     * 节点执行前的准备工作
     *
     * @param context 执行上下文
     */
    public void prepare(Map<String, Object> context) {
        // 默认实现为空，子类可以重写
    }

    /**
     * 节点执行后的清理工作
     *
     * @param context 执行上下文
     */
    public void cleanup(Map<String, Object> context) {
        // 默认实现为空，子类可以重写
    }

    /**
     * 验证节点执行条件
     *
     * @param context 执行上下文
     * @return 是否满足执行条件
     */
    public boolean validate(Map<String, Object> context) {
        // 默认返回true，子类可以重写
        return true;
    }
}