package com.simpleflow.api;

import java.util.Optional;
import java.util.Set;

/**
 * 服务注册器接口
 *
 * 用于在非Spring环境下注册和管理服务实例
 * 支持按名称和类型注册服务
 *
 * @author Simple Flow Team
 * @since 1.0.0
 */
public interface ServiceRegistry {

    /**
     * 注册服务实例
     *
     * @param name 服务名称
     * @param service 服务实例
     * @param <T> 服务类型
     */
    <T> void registerService(String name, T service);

    /**
     * 注册服务实例（使用类名作为服务名称）
     *
     * @param service 服务实例
     * @param <T> 服务类型
     */
    <T> void registerService(T service);

    /**
     * 根据名称获取服务实例
     *
     * @param name 服务名称
     * @param <T> 服务类型
     * @return 服务实例，如果不存在则返回Optional.empty()
     */
    <T> Optional<T> getService(String name);

    /**
     * 根据类型获取服务实例
     *
     * @param serviceClass 服务类型
     * @param <T> 服务类型
     * @return 服务实例，如果不存在则返回Optional.empty()
     */
    <T> Optional<T> getService(Class<T> serviceClass);

    /**
     * 根据类型获取所有服务实例
     *
     * @param serviceClass 服务类型
     * @param <T> 服务类型
     * @return 服务实例集合
     */
    <T> Set<T> getServices(Class<T> serviceClass);

    /**
     * 检查服务是否存在
     *
     * @param name 服务名称
     * @return 是否存在
     */
    boolean hasService(String name);

    /**
     * 检查服务类型是否存在
     *
     * @param serviceClass 服务类型
     * @return 是否存在
     */
    boolean hasService(Class<?> serviceClass);

    /**
     * 移除服务
     *
     * @param <T> 服务类型
     * @param name 服务名称
     * @return 被移除的服务实例，如果不存在则返回Optional.empty()
     */
    <T> Optional<T> removeService(String name);

    /**
     * 获取所有注册的服务名称
     *
     * @return 服务名称集合
     */
    Set<String> getServiceNames();

    /**
     * 清除所有注册的服务
     */
    void clear();

    /**
     * 获取注册的服务数量
     *
     * @return 服务数量
     */
    int size();
}