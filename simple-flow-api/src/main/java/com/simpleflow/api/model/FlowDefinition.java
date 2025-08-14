package com.simpleflow.api.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 流程定义模型
 * 
 * 定义一个完整的工作流程，包括步骤、依赖关系和配置
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
@Data
@Builder
public class FlowDefinition {

    private String id;
    private String name;
    private String description;
    private String version;
    private List<StepDefinition> steps;
    private Map<String, List<String>> dependencies;
    private Map<String, Object> properties;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String threadPoolName;  // 线程池名称，不为空则使用线程池执行
    private boolean sync;           // 是否同步执行，true=同步，false=异步

    /**
     * 根据ID查找步骤
     */
    public Optional<StepDefinition> findStep(String stepId) {
        return steps.stream()
                .filter(step -> Objects.equals(step.getId(), stepId))
                .findFirst();
    }

    /**
     * 获取指定步骤的依赖步骤
     */
    public List<String> getStepDependencies(String stepId) {
        return dependencies.getOrDefault(stepId, Collections.emptyList());
    }

    /**
     * 获取指定步骤的后续步骤
     */
    public List<String> getStepSuccessors(String stepId) {
        return dependencies.entrySet().stream()
                .filter(entry -> entry.getValue().contains(stepId))
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 检查是否存在循环依赖
     */
    public boolean hasCyclicDependency() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (StepDefinition step : steps) {
            if (hasCyclicDependencyUtil(step.getId(), visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCyclicDependencyUtil(String stepId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(stepId)) {
            return true;
        }
        if (visited.contains(stepId)) {
            return false;
        }
        
        visited.add(stepId);
        recursionStack.add(stepId);
        
        List<String> successors = getStepSuccessors(stepId);
        for (String successor : successors) {
            if (hasCyclicDependencyUtil(successor, visited, recursionStack)) {
                return true;
            }
        }
        
        recursionStack.remove(stepId);
        return false;
    }

    /**
     * 获取流程的拓扑排序
     */
    public List<String> getTopologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        List<String> result = new ArrayList<>();
        
        // 初始化入度
        for (StepDefinition step : steps) {
            inDegree.put(step.getId(), 0);
        }
        
        // 计算入度
        for (Map.Entry<String, List<String>> entry : dependencies.entrySet()) {
            for (String dependency : entry.getValue()) {
                inDegree.put(entry.getKey(), inDegree.get(entry.getKey()) + 1);
            }
        }
        
        // 找到入度为0的节点
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        // 拓扑排序
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            
            List<String> successors = getStepSuccessors(current);
            for (String successor : successors) {
                inDegree.put(successor, inDegree.get(successor) - 1);
                if (inDegree.get(successor) == 0) {
                    queue.offer(successor);
                }
            }
        }
        
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowDefinition that = (FlowDefinition) o;
        return Objects.equals(id, that.id) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public String toString() {
        return "FlowDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", steps=" + steps.size() +
                ", threadPoolName='" + threadPoolName + '\'' +
                ", sync=" + sync +
                '}';
    }


}