# Simple Flow Core 核心模块

## 模块概述

Simple Flow Core 是整个工作流引擎的核心模块，提供了流程定义、执行引擎、步骤处理器等核心功能。该模块是整个系统的基础，负责工作流的核心逻辑处理。

## 核心功能

### 🎯 主要特性
- **流程引擎** - 工作流程的执行引擎
- **步骤处理器** - 各种类型步骤的执行器
- **上下文管理** - 流程执行过程中的数据上下文
- **条件判断** - 支持复杂的条件分支逻辑
- **异常处理** - 完善的错误处理和恢复机制
- **并发控制** - 支持并行和串行执行

### 📦 核心组件

#### 1. 流程引擎 (Flow Engine)
- **FlowEngine**: 主要的流程执行引擎
- **FlowExecutor**: 流程执行器，负责具体的执行逻辑
- **FlowContext**: 流程执行上下文，存储执行过程中的数据

#### 2. 步骤处理器 (Step Processors)
- **StepProcessor**: 步骤处理器接口
- **ConditionalStepProcessor**: 条件步骤处理器
- **LoopStepProcessor**: 循环步骤处理器
- **ParallelStepProcessor**: 并行步骤处理器
- **CustomStepProcessor**: 自定义步骤处理器

#### 3. 模型定义 (Model Definitions)
- **FlowDefinition**: 流程定义模型
- **StepDefinition**: 步骤定义模型
- **FlowExecution**: 流程执行实例
- **StepExecution**: 步骤执行实例

## 架构设计

### 🏗️ 模块架构

```
simple-flow-core/
├── src/main/java/com/simpleflow/core/
│   ├── engine/              # 流程引擎
│   │   ├── FlowEngine.java
│   │   ├── FlowExecutor.java
│   │   └── ExecutionContext.java
│   ├── processor/           # 步骤处理器
│   │   ├── StepProcessor.java
│   │   ├── ConditionalStepProcessor.java
│   │   ├── LoopStepProcessor.java
│   │   └── ParallelStepProcessor.java
│   ├── model/              # 数据模型
│   │   ├── FlowDefinition.java
│   │   ├── StepDefinition.java
│   │   ├── FlowExecution.java
│   │   └── StepExecution.java
│   ├── exception/          # 异常定义
│   │   ├── FlowException.java
│   │   ├── StepException.java
│   │   └── ValidationException.java
│   └── util/              # 工具类
│       ├── FlowUtils.java
│       └── ValidationUtils.java
└── src/test/java/         # 测试代码
```

### 🔄 执行流程

1. **流程解析**: 解析流程定义，构建执行计划
2. **上下文初始化**: 创建执行上下文，设置初始参数
3. **步骤执行**: 按照定义顺序执行各个步骤
4. **条件判断**: 根据条件决定执行路径
5. **异常处理**: 处理执行过程中的异常
6. **结果返回**: 返回执行结果和状态

## API 接口

### 核心接口

#### FlowEngine
```java
public interface FlowEngine {
    /**
     * 执行流程
     * @param flowDefinition 流程定义
     * @param context 执行上下文
     * @return 执行结果
     */
    FlowExecution execute(FlowDefinition flowDefinition, FlowContext context);
    
    /**
     * 暂停流程执行
     * @param executionId 执行ID
     */
    void pause(String executionId);
    
    /**
     * 恢复流程执行
     * @param executionId 执行ID
     */
    void resume(String executionId);
    
    /**
     * 停止流程执行
     * @param executionId 执行ID
     */
    void stop(String executionId);
}
```

#### StepProcessor
```java
public interface StepProcessor {
    /**
     * 处理步骤
     * @param stepDefinition 步骤定义
     * @param context 执行上下文
     * @return 步骤执行结果
     */
    StepExecution process(StepDefinition stepDefinition, FlowContext context);
    
    /**
     * 获取支持的步骤类型
     * @return 步骤类型
     */
    String getSupportedStepType();
    
    /**
     * 验证步骤定义
     * @param stepDefinition 步骤定义
     * @return 验证结果
     */
    boolean validate(StepDefinition stepDefinition);
}
```

## 配置说明

### 流程定义示例

```yaml
flow:
  id: "data-processing-flow"
  name: "数据处理流程"
  version: "1.0.0"
  description: "处理用户上传的数据文件"
  steps:
    - id: "validate-input"
      type: "validation"
      name: "输入验证"
      processor: "ValidationStepProcessor"
      config:
        rules:
          - field: "file"
            required: true
            type: "file"
    
    - id: "process-data"
      type: "custom"
      name: "数据处理"
      processor: "DataProcessingStepProcessor"
      config:
        batchSize: 1000
        timeout: 300
    
    - id: "save-result"
      type: "storage"
      name: "保存结果"
      processor: "StorageStepProcessor"
      config:
        destination: "database"
        table: "processed_data"
```

### 执行上下文

```java
FlowContext context = new FlowContext();
context.setVariable("inputFile", "/path/to/input.csv");
context.setVariable("outputPath", "/path/to/output/");
context.setVariable("userId", "12345");

// 执行流程
FlowExecution execution = flowEngine.execute(flowDefinition, context);
```

## 扩展开发

### 自定义步骤处理器

1. **实现 StepProcessor 接口**:
```java
@Component
public class CustomStepProcessor implements StepProcessor {
    
    @Override
    public StepExecution process(StepDefinition stepDefinition, FlowContext context) {
        // 实现自定义逻辑
        return new StepExecution();
    }
    
    @Override
    public String getSupportedStepType() {
        return "custom";
    }
    
    @Override
    public boolean validate(StepDefinition stepDefinition) {
        // 验证步骤定义
        return true;
    }
}
```

2. **注册处理器**:
```java
@Configuration
public class FlowConfiguration {
    
    @Bean
    public StepProcessorRegistry stepProcessorRegistry() {
        StepProcessorRegistry registry = new StepProcessorRegistry();
        registry.register(new CustomStepProcessor());
        return registry;
    }
}
```

### 自定义异常处理

```java
@Component
public class CustomExceptionHandler implements FlowExceptionHandler {
    
    @Override
    public void handleException(FlowException exception, FlowContext context) {
        // 自定义异常处理逻辑
        log.error("Flow execution failed", exception);
        
        // 发送告警
        alertService.sendAlert(exception.getMessage());
        
        // 记录失败信息
        context.setVariable("error", exception.getMessage());
    }
}
```

## 性能优化

### 🚀 优化建议

1. **并行执行**: 使用 ParallelStepProcessor 并行执行独立步骤
2. **缓存机制**: 缓存频繁使用的流程定义和配置
3. **资源池**: 使用线程池管理执行资源
4. **批处理**: 对大量数据使用批处理模式
5. **监控指标**: 添加性能监控和指标收集

### 配置示例

```yaml
flow:
  engine:
    threadPool:
      coreSize: 10
      maxSize: 50
      queueCapacity: 1000
    cache:
      enabled: true
      maxSize: 1000
      ttl: 3600
    monitoring:
      enabled: true
      metricsInterval: 60
```

## 测试指南

### 单元测试

```java
@ExtendWith(MockitoExtension.class)
class FlowEngineTest {
    
    @Mock
    private StepProcessorRegistry processorRegistry;
    
    @InjectMocks
    private DefaultFlowEngine flowEngine;
    
    @Test
    void testExecuteFlow() {
        // 准备测试数据
        FlowDefinition flowDefinition = createTestFlowDefinition();
        FlowContext context = new FlowContext();
        
        // 执行测试
        FlowExecution execution = flowEngine.execute(flowDefinition, context);
        
        // 验证结果
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
    }
}
```

### 集成测试

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class FlowEngineIntegrationTest {
    
    @Autowired
    private FlowEngine flowEngine;
    
    @Test
    void testCompleteFlowExecution() {
        // 完整的流程执行测试
    }
}
```

## 依赖关系

### Maven 依赖

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- 表达式引擎 -->
    <dependency>
        <groupId>com.simpleflow</groupId>
        <artifactId>simple-flow-expression</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- 工具库 -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
</dependencies>
```

## 版本历史

- **v1.0.0** - 初始版本，基础流程引擎功能
- **v1.1.0** - 添加并行执行支持
- **v1.2.0** - 增强异常处理机制
- **v1.3.0** - 性能优化和监控功能

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

MIT License - 详见 [LICENSE](../LICENSE) 文件