# Simple Flow

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-1.0.0--SNAPSHOT-green.svg)](https://search.maven.org/)

一个轻量级、易用的流程编排框架，专为简化复杂业务流程而设计。

## ✨ 特性

- 🚀 **轻量级**: 零依赖核心，可独立运行或集成Spring Boot
- 📝 **多种定义方式**: 支持注解、YAML配置、编程式API
- 🔀 **丰富的执行模式**: 串行、并行、条件分支、脚本条件、循环执行
- 🛠️ **灵活的集成**: 支持Spring Boot自动配置和独立使用
- 📊 **完善的监控**: 内置执行日志和状态跟踪
- 🔧 **易于扩展**: 插件化架构，支持自定义执行器

## 🚀 快速开始

### 环境要求
- JDK 8+
- Maven 3.6+

### 安装

#### Maven
```xml
<dependency>
    <groupId>io.github.nemoob</groupId>
    <artifactId>simple-flow-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Spring Boot
```xml
<dependency>
    <groupId>io.github.nemoob</groupId>
    <artifactId>simple-flow-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本使用

#### 1. 注解方式（推荐）

```java
@Component
public class UserService {
    
    @SimpleFlow("user-registration")
    public void registerUser() {
        validateUser();
        saveUser();
        sendWelcomeEmail();
    }
    
    @Step("validate-user")
    public void validateUser() {
        // 用户验证逻辑
    }
    
    @Step("save-user")
    public void saveUser() {
        // 保存用户逻辑
    }
    
    @Step("send-email")
    public void sendWelcomeEmail() {
        // 发送邮件逻辑
    }
}
```

#### 2. YAML配置方式

```yaml
flows:
  user-registration:
    name: "用户注册流程"
    steps:
      - id: "validate-user"
        type: "SERVICE"
        beanName: "userService"
        methodName: "validateUser"
      - id: "save-user"
        type: "SERVICE"
        beanName: "userService"
        methodName: "saveUser"
      - id: "send-email"
        type: "SERVICE"
        beanName: "userService"
        methodName: "sendWelcomeEmail"
```

#### 3. 编程式API

```java
// 创建流程
FlowDefinition flow = FlowDefinition.builder()
    .id("user-registration")
    .name("用户注册流程")
    .addStep(StepDefinition.serviceStep("validate-user", "userService", "validateUser"))
    .addStep(StepDefinition.serviceStep("save-user", "userService", "saveUser"))
    .addStep(StepDefinition.serviceStep("send-email", "userService", "sendWelcomeEmail"))
    .build();

// 执行流程
FlowEngine engine = new FlowEngine();
FlowExecutionResult result = engine.execute(flow, new FlowContext());
```

## 🔥 高级特性

### 条件分支
```yaml
steps:
  - id: "age-check"
    type: "SCRIPT_CONDITIONAL"
    scriptType: "kotlin"
    script: |
      val age = context.get("age") as Int
      age >= 18
    trueSteps:
      - id: "adult-process"
        type: "SERVICE"
        beanName: "userService"
        methodName: "processAdult"
    falseSteps:
      - id: "minor-process"
        type: "SERVICE"
        beanName: "userService"
        methodName: "processMinor"
```

### 并行执行
```yaml
steps:
  - id: "parallel-tasks"
    type: "PARALLEL"
    steps:
      - id: "send-email"
        type: "SERVICE"
        beanName: "notificationService"
        methodName: "sendEmail"
      - id: "send-sms"
        type: "SERVICE"
        beanName: "notificationService"
        methodName: "sendSms"
```

## 📚 文档

- [快速开始指南](docs/quick-start.md)
- [API 文档](docs/api.md)
- [配置参考](docs/configuration.md)
- [示例项目](examples/)

## 🗺️ 开发计划

### ✅ 已完成
- 基础流程引擎
- 注解和配置支持
- 条件分支和循环
- Spring Boot集成

### 🚧 进行中
- 并行执行优化
- 监控和日志

### 📋 计划中
- 可视化界面
- 持久化支持
- 分布式执行

## 🤝 贡献

我们欢迎所有形式的贡献！

### 如何贡献
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 贡献类型
- 🐛 Bug 修复
- ✨ 新功能
- 📝 文档改进
- 🎨 代码优化
- 🧪 测试用例

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源协议。

## 🙏 致谢

感谢所有贡献者的努力！

---

如果这个项目对你有帮助，请给我们一个 ⭐️！