package io.github.nemoob.expression.context;

import java.util.*;

/**
 * 表达式执行上下文
 * 
 * 提供表达式执行时所需的变量和函数
 * 
 * @author Simple Flow Team
 * @since 1.0.0
 */
public class ExpressionContext {

    private final Map<String, Object> variables;
    private final Map<String, Object> functions;
    private final ExpressionContext parent;

    /**
     * 创建空的表达式上下文
     */
    public ExpressionContext() {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.parent = null;
    }

    /**
     * 创建带父上下文的表达式上下文
     * 
     * @param parent 父上下文
     */
    public ExpressionContext(ExpressionContext parent) {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.parent = parent;
    }

    /**
     * 创建带初始变量的表达式上下文
     * 
     * @param initialVariables 初始变量
     */
    public ExpressionContext(Map<String, Object> initialVariables) {
        this.parent = null;
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        
        if (initialVariables != null) {
            this.variables.putAll(initialVariables);
        }
    }

    /**
     * 创建带父上下文和初始变量的表达式上下文
     * 
     * @param parent 父上下文
     * @param initialVariables 初始变量
     */
    public ExpressionContext(ExpressionContext parent, Map<String, Object> initialVariables) {
        this.parent = parent;
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        
        if (initialVariables != null) {
            this.variables.putAll(initialVariables);
        }
    }

    /**
     * 设置变量
     * 
     * @param name 变量名
     * @param value 变量值
     * @return 当前上下文（支持链式调用）
     */
    public ExpressionContext setVariable(String name, Object value) {
        Objects.requireNonNull(name, "Variable name cannot be null");
        variables.put(name, value);
        return this;
    }

    /**
     * 批量设置变量
     * 
     * @param variables 变量映射
     * @return 当前上下文（支持链式调用）
     */
    public ExpressionContext setVariables(Map<String, Object> variables) {
        if (variables != null) {
            this.variables.putAll(variables);
        }
        return this;
    }

    /**
     * 获取变量值
     * 
     * @param name 变量名
     * @return 变量值，如果不存在则返回 null
     */
    public Object getVariable(String name) {
        Objects.requireNonNull(name, "Variable name cannot be null");
        
        // 首先在当前上下文中查找
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        
        // 如果当前上下文中没有，则在父上下文中查找
        if (parent != null) {
            return parent.getVariable(name);
        }
        
        return null;
    }

    /**
     * 获取指定类型的变量值
     * 
     * @param name 变量名
     * @param type 期望的类型
     * @param <T> 类型参数
     * @return 变量值，如果不存在或类型不匹配则返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String name, Class<T> type) {
        Object value = getVariable(name);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 检查变量是否存在
     * 
     * @param name 变量名
     * @return 如果变量存在则返回 true
     */
    public boolean hasVariable(String name) {
        Objects.requireNonNull(name, "Variable name cannot be null");
        
        if (variables.containsKey(name)) {
            return true;
        }
        
        return parent != null && parent.hasVariable(name);
    }

    /**
     * 移除变量
     * 
     * @param name 变量名
     * @return 被移除的变量值，如果不存在则返回 null
     */
    public Object removeVariable(String name) {
        Objects.requireNonNull(name, "Variable name cannot be null");
        return variables.remove(name);
    }

    /**
     * 获取所有变量名
     * 
     * @return 变量名集合
     */
    public Set<String> getVariableNames() {
        Set<String> names = new HashMap<>(variables).keySet();
        
        if (parent != null) {
            names.addAll(parent.getVariableNames());
        }
        
        return Collections.unmodifiableSet(names);
    }

    /**
     * 获取当前上下文的变量映射
     * 
     * @return 变量映射的只读视图
     */
    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * 获取所有变量（包括父上下文）
     * 
     * @return 所有变量的映射
     */
    public Map<String, Object> getAllVariables() {
        Map<String, Object> allVariables = new HashMap<>();
        
        // 首先添加父上下文的变量
        if (parent != null) {
            allVariables.putAll(parent.getAllVariables());
        }
        
        // 然后添加当前上下文的变量（会覆盖父上下文中的同名变量）
        allVariables.putAll(variables);
        
        return allVariables;
    }

    /**
     * 设置函数
     * 
     * @param name 函数名
     * @param function 函数对象
     * @return 当前上下文（支持链式调用）
     */
    public ExpressionContext setFunction(String name, Object function) {
        Objects.requireNonNull(name, "Function name cannot be null");
        Objects.requireNonNull(function, "Function cannot be null");
        functions.put(name, function);
        return this;
    }

    /**
     * 获取函数
     * 
     * @param name 函数名
     * @return 函数对象，如果不存在则返回 null
     */
    public Object getFunction(String name) {
        Objects.requireNonNull(name, "Function name cannot be null");
        
        // 首先在当前上下文中查找
        if (functions.containsKey(name)) {
            return functions.get(name);
        }
        
        // 如果当前上下文中没有，则在父上下文中查找
        if (parent != null) {
            return parent.getFunction(name);
        }
        
        return null;
    }

    /**
     * 检查函数是否存在
     * 
     * @param name 函数名
     * @return 如果函数存在则返回 true
     */
    public boolean hasFunction(String name) {
        Objects.requireNonNull(name, "Function name cannot be null");
        
        if (functions.containsKey(name)) {
            return true;
        }
        
        return parent != null && parent.hasFunction(name);
    }

    /**
     * 移除函数
     * 
     * @param name 函数名
     * @return 被移除的函数对象，如果不存在则返回 null
     */
    public Object removeFunction(String name) {
        Objects.requireNonNull(name, "Function name cannot be null");
        return functions.remove(name);
    }

    /**
     * 获取所有函数名
     * 
     * @return 函数名集合
     */
    public Set<String> getFunctionNames() {
        Set<String> names = new HashMap<>(functions).keySet();
        
        if (parent != null) {
            names.addAll(parent.getFunctionNames());
        }
        
        return Collections.unmodifiableSet(names);
    }

    /**
     * 获取当前上下文的函数映射
     * 
     * @return 函数映射的只读视图
     */
    public Map<String, Object> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    /**
     * 创建子上下文
     * 
     * @return 新的子上下文
     */
    public ExpressionContext createChild() {
        return new ExpressionContext(this);
    }

    /**
     * 创建带初始变量的子上下文
     * 
     * @param initialVariables 初始变量
     * @return 新的子上下文
     */
    public ExpressionContext createChild(Map<String, Object> initialVariables) {
        return new ExpressionContext(this, initialVariables);
    }

    /**
     * 获取父上下文
     * 
     * @return 父上下文，如果没有则返回 null
     */
    public ExpressionContext getParent() {
        return parent;
    }

    /**
     * 清空当前上下文的所有变量和函数
     */
    public void clear() {
        variables.clear();
        functions.clear();
    }

    /**
     * 检查上下文是否为空
     * 
     * @return 如果没有变量和函数则返回 true
     */
    public boolean isEmpty() {
        return variables.isEmpty() && functions.isEmpty() && 
               (parent == null || parent.isEmpty());
    }

    /**
     * 获取上下文大小（变量和函数的总数）
     * 
     * @return 上下文大小
     */
    public int size() {
        int size = variables.size() + functions.size();
        if (parent != null) {
            size += parent.size();
        }
        return size;
    }

    @Override
    public String toString() {
        return "ExpressionContext{" +
                "variables=" + variables.keySet() +
                ", functions=" + functions.keySet() +
                ", hasParent=" + (parent != null) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpressionContext that = (ExpressionContext) o;
        return Objects.equals(variables, that.variables) &&
                Objects.equals(functions, that.functions) &&
                Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variables, functions, parent);
    }
}