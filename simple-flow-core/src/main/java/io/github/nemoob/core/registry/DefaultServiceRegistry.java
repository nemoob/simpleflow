package io.github.nemoob.core.registry;

import io.github.nemoob.api.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认服务注册器实现
 * 
 * 线程安全的服务注册器，支持按名称和类型注册服务
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Slf4j
public class DefaultServiceRegistry implements ServiceRegistry {
    
    private static final DefaultServiceRegistry INSTANCE = new DefaultServiceRegistry();
    
    // 按名称存储服务实例
    private final Map<String, Object> servicesByName = new ConcurrentHashMap<>();
    
    // 按类型存储服务实例（一个类型可能有多个实例）
    private final Map<Class<?>, Set<Object>> servicesByType = new ConcurrentHashMap<>();
    
    // 服务名称到类型的映射
    private final Map<String, Class<?>> nameToTypeMapping = new ConcurrentHashMap<>();
    
    private DefaultServiceRegistry() {
        // 私有构造函数
    }
    
    /**
     * 获取单例实例
     */
    public static DefaultServiceRegistry getInstance() {
        return INSTANCE;
    }
    
    @Override
    public <T> void registerService(String name, T service) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        if (service == null) {
            throw new IllegalArgumentException("Service instance cannot be null");
        }
        
        Class<?> serviceClass = service.getClass();
        
        // 注册到名称映射
        servicesByName.put(name, service);
        nameToTypeMapping.put(name, serviceClass);
        
        // 注册到类型映射
        servicesByType.computeIfAbsent(serviceClass, k -> ConcurrentHashMap.newKeySet()).add(service);
        
        // 同时注册所有接口和父类
        registerServiceInterfaces(service, serviceClass);
        
        log.info("Registered service: {} -> {} ({})", name, serviceClass.getSimpleName(), serviceClass.getName());
    }
    
    @Override
    public <T> void registerService(T service) {
        if (service == null) {
            throw new IllegalArgumentException("Service instance cannot be null");
        }
        
        Class<?> serviceClass = service.getClass();
        String serviceName = generateServiceName(serviceClass);
        registerService(serviceName, service);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getService(String name) {
        Object service = servicesByName.get(name);
        return service != null ? Optional.of((T) service) : Optional.empty();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getService(Class<T> serviceClass) {
        Set<Object> services = servicesByType.get(serviceClass);
        if (services != null && !services.isEmpty()) {
            // 返回第一个匹配的服务
            return Optional.of((T) services.iterator().next());
        }
        return Optional.empty();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> getServices(Class<T> serviceClass) {
        Set<Object> services = servicesByType.get(serviceClass);
        if (services != null) {
            return services.stream()
                    .map(service -> (T) service)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
    
    @Override
    public boolean hasService(String name) {
        return servicesByName.containsKey(name);
    }
    
    @Override
    public boolean hasService(Class<?> serviceClass) {
        Set<Object> services = servicesByType.get(serviceClass);
        return services != null && !services.isEmpty();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> removeService(String name) {
        Object service = servicesByName.remove(name);
        if (service != null) {
            Class<?> serviceClass = nameToTypeMapping.remove(name);
            if (serviceClass != null) {
                // 从类型映射中移除
                Set<Object> services = servicesByType.get(serviceClass);
                if (services != null) {
                    services.remove(service);
                    if (services.isEmpty()) {
                        servicesByType.remove(serviceClass);
                    }
                }
                
                // 从接口和父类映射中移除
                removeServiceFromInterfaces(service, serviceClass);
            }
            
            log.info("Removed service: {}", name);
            return Optional.of((T) service);
        }
        return Optional.empty();
    }
    
    @Override
    public Set<String> getServiceNames() {
        return new HashSet<>(servicesByName.keySet());
    }
    
    @Override
    public void clear() {
        servicesByName.clear();
        servicesByType.clear();
        nameToTypeMapping.clear();
        log.info("Cleared all registered services");
    }
    
    @Override
    public int size() {
        return servicesByName.size();
    }
    
    /**
     * 注册服务的所有接口和父类
     */
    private void registerServiceInterfaces(Object service, Class<?> serviceClass) {
        // 注册所有接口
        for (Class<?> interfaceClass : serviceClass.getInterfaces()) {
            servicesByType.computeIfAbsent(interfaceClass, k -> ConcurrentHashMap.newKeySet()).add(service);
            // 递归注册父接口
            registerServiceInterfaces(service, interfaceClass);
        }
        
        // 注册父类
        Class<?> superClass = serviceClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            servicesByType.computeIfAbsent(superClass, k -> ConcurrentHashMap.newKeySet()).add(service);
            registerServiceInterfaces(service, superClass);
        }
    }
    
    /**
     * 从接口和父类映射中移除服务
     */
    private void removeServiceFromInterfaces(Object service, Class<?> serviceClass) {
        // 从所有接口中移除
        for (Class<?> interfaceClass : serviceClass.getInterfaces()) {
            Set<Object> services = servicesByType.get(interfaceClass);
            if (services != null) {
                services.remove(service);
                if (services.isEmpty()) {
                    servicesByType.remove(interfaceClass);
                }
            }
            // 递归移除父接口
            removeServiceFromInterfaces(service, interfaceClass);
        }
        
        // 从父类中移除
        Class<?> superClass = serviceClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            Set<Object> services = servicesByType.get(superClass);
            if (services != null) {
                services.remove(service);
                if (services.isEmpty()) {
                    servicesByType.remove(superClass);
                }
            }
            removeServiceFromInterfaces(service, superClass);
        }
    }
    
    /**
     * 生成服务名称
     */
    private String generateServiceName(Class<?> serviceClass) {
        String className = serviceClass.getSimpleName();
        // 将首字母小写
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
}