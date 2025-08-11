# Simple Flow API 设计文档

## 概述

Simple Flow 提供了多种 API 方式来定义和执行流程，包括 SQL 风格语法、YAML 配置、注解配置和编程式 API。

## 1. SQL 风格语法 API（推荐）

### 1.1 基本语法

#### THEN 语句
```xml
<!-- 顺序执行多个步骤 -->
<flow name="orderProcessFlow">
    THEN(userService.validateUser, orderService.createOrder, paymentService.processPayment);
</flow>
```

#### IF 语句
```xml
<!-- 条件分支 -->
<flow name="conditionalFlow">
    IF(amount > 1000, riskService.assess, orderService.process);
</flow>
```

#### CASE-WHEN 语句
```xml
<!-- 多条件分支 -->
<flow name="userTypeFlow">
    CASE 
        WHEN userType == 'VIP' THEN vipService.process
        WHEN userType == 'PREMIUM' THEN premiumService.process
        ELSE regularService.process
    END;
</flow>
```

### 1.2 混合语法
```xml
<flow name="complexFlow">
    THEN(userService.loadUser);
    IF(user.isValid, 
        THEN(orderService.createOrder, paymentService.process),
        THEN(errorService.handleError)
    );
    THEN(auditService.log);
</flow>
```

## 2. YAML 配置 API

### 2.1 基本结构
```yaml
flows:
  - name: "orderProcessFlow"
    steps:
      - type: SERVICE
        name: "validateUser"
        service: "userService"
        method: "validate"
      - type: CONDITIONAL
        name: "checkAmount"
        condition: "amount > 1000"
        then:
          - type: SERVICE
            service: "riskService"
            method: "assess"
        else:
          - type: SERVICE
            service: "orderService"
            method: "process"
```

### 2.2 并行执行
```yaml
flows:
  - name: "parallelFlow"
    steps:
      - type: PARALLEL
        name: "parallelTasks"
        steps:
          - type: SERVICE
            service: "emailService"
            method: "send"
          - type: SERVICE
            service: "smsService"
            method: "send"
```

## 3. 注解配置 API

### 3.1 基本注解
```java
@SimpleFlow("userRegistrationFlow")
public class UserRegistrationFlow {
    
    @Step(order = 1)
    public void validateUser(FlowContext context) {
        // 验证用户信息
    }
    
    @Step(order = 2, condition = "user.age >= 18")
    public void processAdult(FlowContext context) {
        // 处理成年用户
    }
    
    @Step(order = 3, condition = "user.age < 18")
    public void processMinor(FlowContext context) {
        // 处理未成年用户
    }
}
```

### 3.2 条件步骤
```java
@SimpleFlow("conditionalFlow")
public class ConditionalFlow {
    
    @Step(order = 1)
    @Conditional(
        condition = "amount > 1000",
        thenStep = "riskAssessment",
        elseStep = "directProcess"
    )
    public void checkAmount(FlowContext context) {
        // 条件检查逻辑
    }
    
    @Step(name = "riskAssessment")
    public void assessRisk(FlowContext context) {
        // 风险评估
    }
    
    @Step(name = "directProcess")
    public void processDirectly(FlowContext context) {
        // 直接处理
    }
}
```

## 4. 编程式 API

### 4.1 流程构建器
```java
FlowDefinition flow = FlowBuilder.create("orderProcessFlow")
    .step("validateUser")
        .service("userService")
        .method("validate")
    .step("checkAmount")
        .conditional()
        .condition("amount > 1000")
        .then()
            .service("riskService")
            .method("assess")
        .otherwise()
            .service("orderService")
            .method("process")
    .build();
```

### 4.2 并行流程
```java
FlowDefinition parallelFlow = FlowBuilder.create("notificationFlow")
    .parallel()
        .step("sendEmail")
            .service("emailService")
            .method("send")
        .step("sendSMS")
            .service("smsService")
            .method("send")
    .end()
    .build();
```

## 5. 执行 API

### 5.1 基本执行
```java
// 创建执行引擎
FlowEngine engine = FlowEngineFactory.create();

// 创建执行上下文
FlowContext context = FlowContext.create()
    .put("userId", 12345)
    .put("amount", 1500.0);

// 执行流程
FlowResult result = engine.execute("orderProcessFlow", context);

// 检查执行结果
if (result.isSuccess()) {
    System.out.println("流程执行成功");
} else {
    System.out.println("流程执行失败: " + result.getError());
}
```

### 5.2 异步执行
```java
// 异步执行
CompletableFuture<FlowResult> future = engine.executeAsync("orderProcessFlow", context);

// 处理结果
future.thenAccept(result -> {
    if (result.isSuccess()) {
        System.out.println("异步流程执行成功");
    }
});
```

## 6. 服务集成 API

### 6.1 StepHandler 接口
```java
@Component
public class WeatherService implements StepHandler {
    
    @Override
    public void execute(FlowContext context) {
        // 获取天气信息
        String weather = getWeatherInfo();
        context.put("weather", weather);
    }
    
    private String getWeatherInfo() {
        return "Sunny";
    }
}
```

### 6.2 服务方法调用
```java
@Service
public class UserService {
    
    public void validateUser(FlowContext context) {
        Long userId = context.get("userId");
        // 验证用户逻辑
        context.put("userValid", true);
    }
    
    public User loadUser(FlowContext context) {
        Long userId = context.get("userId");
        User user = userRepository.findById(userId);
        context.put("user", user);
        return user;
    }
}
```

## 7. 配置 API

### 7.1 Spring Boot 配置
```yaml
simple-flow:
  enabled: true
  thread-pool:
    core-size: 10
    max-size: 50
    queue-capacity: 100
  monitoring:
    enabled: true
    metrics-enabled: true
  expression:
    engine: spel  # spel, groovy, custom
```

### 7.2 独立应用配置
```java
FlowEngineConfig config = FlowEngineConfig.builder()
    .threadPoolSize(20)
    .enableMonitoring(true)
    .expressionEngine(ExpressionEngine.SPEL)
    .build();
    
FlowEngine engine = FlowEngineFactory.create(config);
```

## 8. 监控和调试 API

### 8.1 执行监听器
```java
public class FlowExecutionListener implements FlowListener {
    
    @Override
    public void onFlowStart(FlowEvent event) {
        System.out.println("流程开始: " + event.getFlowName());
    }
    
    @Override
    public void onStepStart(StepEvent event) {
        System.out.println("步骤开始: " + event.getStepName());
    }
    
    @Override
    public void onStepComplete(StepEvent event) {
        System.out.println("步骤完成: " + event.getStepName());
    }
    
    @Override
    public void onFlowComplete(FlowEvent event) {
        System.out.println("流程完成: " + event.getFlowName());
    }
}
```

### 8.2 性能监控
```java
// 获取执行统计
FlowMetrics metrics = engine.getMetrics();
System.out.println("总执行次数: " + metrics.getTotalExecutions());
System.out.println("平均执行时间: " + metrics.getAverageExecutionTime());
System.out.println("成功率: " + metrics.getSuccessRate());
```

## 9. 错误处理 API

### 9.1 异常处理
```java
try {
    FlowResult result = engine.execute("orderProcessFlow", context);
} catch (FlowExecutionException e) {
    System.err.println("流程执行异常: " + e.getMessage());
    System.err.println("失败步骤: " + e.getFailedStep());
} catch (FlowConfigurationException e) {
    System.err.println("流程配置异常: " + e.getMessage());
}
```

### 9.2 重试机制
```yaml
flows:
  - name: "retryFlow"
    steps:
      - type: SERVICE
        service: "externalService"
        method: "call"
        retry:
          max-attempts: 3
          delay: 1000
          backoff-multiplier: 2.0
```

## 10. 扩展 API

### 10.1 自定义执行器
```java
@Component
public class CustomStepExecutor implements StepExecutor {
    
    @Override
    public boolean supports(StepDefinition step) {
        return "CUSTOM".equals(step.getType());
    }
    
    @Override
    public void execute(StepDefinition step, FlowContext context) {
        // 自定义执行逻辑
    }
}
```

### 10.2 自定义表达式引擎
```java
@Component
public class CustomExpressionEngine implements ExpressionEngine {
    
    @Override
    public boolean evaluate(String expression, FlowContext context) {
        // 自定义表达式评估逻辑
        return true;
    }
    
    @Override
    public <T> T evaluateExpression(String expression, FlowContext context, Class<T> returnType) {
        // 自定义表达式计算逻辑
        return null;
    }
}
```

## 总结

Simple Flow 提供了丰富的 API 来满足不同场景的需求：

- **SQL 风格语法**: 简洁直观，适合复杂流程定义
- **YAML 配置**: 结构化配置，适合配置驱动的场景
- **注解配置**: 代码即配置，适合 Java 开发者
- **编程式 API**: 灵活强大，适合动态流程构建

选择合适的 API 方式可以大大提高开发效率和代码可维护性。