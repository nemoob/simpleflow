# Simple Flow Storage 存储模块

## 模块概述

Simple Flow Storage 是工作流引擎的数据持久化模块，负责流程定义、执行记录、状态信息等数据的存储和管理。支持多种数据库类型，提供统一的数据访问接口。

## 核心功能

### 🎯 主要特性
- **多数据库支持** - 支持 MySQL、PostgreSQL、H2 等数据库
- **流程定义存储** - 流程定义的持久化管理
- **执行记录管理** - 流程执行历史和状态跟踪
- **事务管理** - 完整的事务支持和数据一致性
- **数据迁移** - 数据库版本升级和迁移工具
- **性能优化** - 索引优化和查询性能调优

### 📦 核心组件

#### 1. 数据访问层 (Data Access Layer)
- **FlowDefinitionRepository**: 流程定义数据访问
- **FlowExecutionRepository**: 流程执行记录访问
- **StepExecutionRepository**: 步骤执行记录访问
- **FlowMetricsRepository**: 流程指标数据访问

#### 2. 实体模型 (Entity Models)
- **FlowDefinitionEntity**: 流程定义实体
- **FlowExecutionEntity**: 流程执行实体
- **StepExecutionEntity**: 步骤执行实体
- **FlowMetricsEntity**: 流程指标实体

#### 3. 数据服务 (Data Services)
- **FlowStorageService**: 流程存储服务
- **ExecutionStorageService**: 执行记录存储服务
- **MetricsStorageService**: 指标数据存储服务

## 架构设计

### 🏗️ 模块架构

```
simple-flow-storage/
├── src/main/java/com/simpleflow/storage/
│   ├── entity/              # 实体类
│   │   ├── FlowDefinitionEntity.java
│   │   ├── FlowExecutionEntity.java
│   │   ├── StepExecutionEntity.java
│   │   └── FlowMetricsEntity.java
│   ├── repository/          # 数据访问层
│   │   ├── FlowDefinitionRepository.java
│   │   ├── FlowExecutionRepository.java
│   │   ├── StepExecutionRepository.java
│   │   └── FlowMetricsRepository.java
│   ├── service/            # 数据服务
│   │   ├── FlowStorageService.java
│   │   ├── ExecutionStorageService.java
│   │   └── MetricsStorageService.java
│   ├── mapper/             # MyBatis Mapper
│   │   ├── FlowDefinitionMapper.java
│   │   ├── FlowExecutionMapper.java
│   │   └── StepExecutionMapper.java
│   ├── config/             # 配置类
│   │   ├── DatabaseConfig.java
│   │   └── MyBatisConfig.java
│   └── migration/          # 数据库迁移
│       ├── V1__Initial_Schema.sql
│       ├── V2__Add_Metrics_Table.sql
│       └── V3__Add_Indexes.sql
└── src/main/resources/
    ├── mapper/             # MyBatis XML 映射文件
    │   ├── FlowDefinitionMapper.xml
    │   ├── FlowExecutionMapper.xml
    │   └── StepExecutionMapper.xml
    ├── schema-h2.sql       # H2 数据库表结构
    ├── schema-mysql.sql    # MySQL 数据库表结构
    ├── data-h2.sql        # H2 测试数据
    └── application-storage.yml
```

### 🗄️ 数据库设计

#### 核心表结构

**flow_definition (流程定义表)**
```sql
CREATE TABLE flow_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    version VARCHAR(50) NOT NULL,
    definition_content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    INDEX idx_flow_id (flow_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

**flow_execution (流程执行表)**
```sql
CREATE TABLE flow_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(100) NOT NULL UNIQUE,
    flow_id VARCHAR(100) NOT NULL,
    flow_version VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration BIGINT,
    input_data TEXT,
    output_data TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_execution_id (execution_id),
    INDEX idx_flow_id (flow_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    FOREIGN KEY (flow_id) REFERENCES flow_definition(flow_id)
);
```

**step_execution (步骤执行表)**
```sql
CREATE TABLE step_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(100) NOT NULL,
    step_id VARCHAR(100) NOT NULL,
    step_name VARCHAR(200),
    step_type VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration BIGINT,
    input_data TEXT,
    output_data TEXT,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_execution_id (execution_id),
    INDEX idx_step_id (step_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    FOREIGN KEY (execution_id) REFERENCES flow_execution(execution_id)
);
```

## API 接口

### 核心接口

#### FlowStorageService
```java
public interface FlowStorageService {
    /**
     * 保存流程定义
     * @param flowDefinition 流程定义
     * @return 保存的流程定义
     */
    FlowDefinitionEntity saveFlowDefinition(FlowDefinitionEntity flowDefinition);
    
    /**
     * 根据ID查询流程定义
     * @param flowId 流程ID
     * @return 流程定义
     */
    Optional<FlowDefinitionEntity> findFlowDefinitionById(String flowId);
    
    /**
     * 查询所有活跃的流程定义
     * @return 流程定义列表
     */
    List<FlowDefinitionEntity> findActiveFlowDefinitions();
    
    /**
     * 删除流程定义
     * @param flowId 流程ID
     */
    void deleteFlowDefinition(String flowId);
}
```

#### ExecutionStorageService
```java
public interface ExecutionStorageService {
    /**
     * 保存流程执行记录
     * @param execution 执行记录
     * @return 保存的执行记录
     */
    FlowExecutionEntity saveExecution(FlowExecutionEntity execution);
    
    /**
     * 更新执行状态
     * @param executionId 执行ID
     * @param status 新状态
     */
    void updateExecutionStatus(String executionId, ExecutionStatus status);
    
    /**
     * 查询正在运行的执行记录
     * @return 执行记录列表
     */
    List<FlowExecutionEntity> findRunningExecutions();
    
    /**
     * 根据状态统计执行数量
     * @param status 执行状态
     * @return 数量
     */
    long countExecutionsByStatus(ExecutionStatus status);
}
```

### MyBatis Mapper 示例

#### FlowExecutionMapper
```java
@Mapper
public interface FlowExecutionMapper {
    
    @Insert("INSERT INTO flow_execution (execution_id, flow_id, status, start_time, input_data) " +
            "VALUES (#{executionId}, #{flowId}, #{status}, #{startTime}, #{inputData})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FlowExecutionEntity execution);
    
    @Select("SELECT * FROM flow_execution WHERE execution_id = #{executionId}")
    FlowExecutionEntity findByExecutionId(String executionId);
    
    @Update("UPDATE flow_execution SET status = #{status}, end_time = #{endTime}, " +
            "duration = #{duration}, output_data = #{outputData}, error_message = #{errorMessage} " +
            "WHERE execution_id = #{executionId}")
    int updateExecution(FlowExecutionEntity execution);
    
    @Select("SELECT COUNT(*) FROM flow_execution WHERE status = #{status}")
    long countByStatus(String status);
    
    @Select("SELECT * FROM flow_execution WHERE status = 'RUNNING' ORDER BY start_time DESC")
    List<FlowExecutionEntity> findRunningExecutions();
}
```

## 配置说明

### 数据库配置

#### application-storage.yml
```yaml
spring:
  datasource:
    # H2 数据库配置（开发/测试环境）
    url: jdbc:h2:mem:simple_flow;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    
    # MySQL 数据库配置（生产环境）
    # url: jdbc:mysql://localhost:3306/simple_flow?useUnicode=true&characterEncoding=utf8&useSSL=false
    # driver-class-name: com.mysql.cj.jdbc.Driver
    # username: simple_flow
    # password: your_password
  
  jpa:
    hibernate:
      ddl-auto: none  # 使用 SQL 脚本初始化
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        # dialect: org.hibernate.dialect.MySQL8Dialect  # MySQL
        format_sql: true
  
  sql:
    init:
      mode: always
      schema-locations: classpath:schema-h2.sql
      data-locations: classpath:data-h2.sql

# MyBatis 配置
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: io.github.nemoob.storage.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
    lazy-loading-enabled: true
    multiple-result-sets-enabled: true
    use-column-label: true
    use-generated-keys: true
    auto-mapping-behavior: partial
    default-executor-type: simple
    default-statement-timeout: 25
    default-fetch-size: 100
    safe-row-bounds-enabled: false
    local-cache-scope: session
    jdbc-type-for-null: other
    lazy-load-trigger-methods: equals,clone,hashCode,toString

# 连接池配置
hikari:
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
  maximum-pool-size: 20
  minimum-idle: 5
  pool-name: SimpleFlowHikariCP
```

## 性能优化

### 🚀 优化策略

1. **索引优化**
```sql
-- 复合索引
CREATE INDEX idx_execution_flow_status ON flow_execution(flow_id, status);
CREATE INDEX idx_execution_time_status ON flow_execution(start_time, status);

-- 覆盖索引
CREATE INDEX idx_execution_summary ON flow_execution(flow_id, status, start_time, end_time);
```

2. **查询优化**
```java
// 分页查询
@Select("SELECT * FROM flow_execution WHERE status = #{status} " +
        "ORDER BY start_time DESC LIMIT #{offset}, #{limit}")
List<FlowExecutionEntity> findByStatusWithPaging(
    @Param("status") String status,
    @Param("offset") int offset,
    @Param("limit") int limit
);

// 批量插入
@Insert("<script>" +
        "INSERT INTO step_execution (execution_id, step_id, status) VALUES " +
        "<foreach collection='steps' item='step' separator=','>" +
        "(#{step.executionId}, #{step.stepId}, #{step.status})" +
        "</foreach>" +
        "</script>")
void batchInsertSteps(@Param("steps") List<StepExecutionEntity> steps);
```

## 测试指南

### 单元测试

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlowExecutionRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private FlowExecutionRepository repository;
    
    @Test
    void testFindByStatus() {
        // 准备测试数据
        FlowExecutionEntity execution = new FlowExecutionEntity();
        execution.setExecutionId("test-001");
        execution.setFlowId("test-flow");
        execution.setStatus(ExecutionStatus.RUNNING);
        entityManager.persistAndFlush(execution);
        
        // 执行查询
        List<FlowExecutionEntity> results = repository.findByStatus(ExecutionStatus.RUNNING);
        
        // 验证结果
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getExecutionId()).isEqualTo("test-001");
    }
}
```

## 依赖关系

### Maven 依赖

```xml
<dependencies>
    <!-- Spring Boot Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.2</version>
    </dependency>
    
    <!-- 数据库驱动 -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- 连接池 -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
    </dependency>
</dependencies>
```

## 版本历史

- **v1.0.0** - 初始版本，基础存储功能
- **v1.1.0** - 添加 MyBatis 支持
- **v1.2.0** - 增加缓存和性能优化
- **v1.3.0** - 添加监控指标和数据迁移

## 许可证

MIT License - 详见 [LICENSE](../LICENSE) 文件

## 功能特性

### 核心实体

- **FlowDefinition**: 流程定义，包含流程名称、版本、配置等信息
- **FlowExecution**: 流程执行记录，跟踪流程的执行状态和结果
- **StepExecution**: 步骤执行记录，记录流程中每个步骤的执行情况
- **FlowConfig**: 流程配置，存储系统级别的配置参数

### 服务层功能

#### FlowDefinitionService
- 流程定义的 CRUD 操作
- 版本管理（自动生成下一版本号）
- 按名称查询最新版本
- 激活/停用流程
- 逻辑删除

#### FlowExecutionService
- 流程执行记录的管理
- 状态跟踪（PENDING、RUNNING、SUCCESS、FAILED、CANCELLED、SKIPPED）
- 执行统计和监控
- 按各种条件查询和分页

#### StepExecutionService
- 步骤执行记录的管理
- 重试机制支持
- 步骤级别的状态跟踪
- 执行统计和分析

#### FlowConfigService
- 系统配置管理
- 支持多种数据类型（STRING、INTEGER、BOOLEAN）
- 配置的激活/停用
- 批量操作支持

## 数据库设计

### 表结构

```sql
-- 流程定义表
CREATE TABLE flow_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_name VARCHAR(100) NOT NULL,
    version VARCHAR(20) NOT NULL,
    description TEXT,
    flow_config JSON,
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name_version (flow_name, version)
);

-- 流程执行表
CREATE TABLE flow_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(100) UNIQUE NOT NULL,
    flow_definition_id BIGINT NOT NULL,
    flow_name VARCHAR(100) NOT NULL,
    flow_version VARCHAR(20) NOT NULL,
    status ENUM('PENDING','RUNNING','SUCCESS','FAILED','CANCELLED','SKIPPED') DEFAULT 'PENDING',
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (flow_definition_id) REFERENCES flow_definition(id)
);

-- 步骤执行表
CREATE TABLE step_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_execution_id BIGINT NOT NULL,
    execution_id VARCHAR(100) NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    status ENUM('PENDING','RUNNING','SUCCESS','FAILED','CANCELLED','SKIPPED') DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (flow_execution_id) REFERENCES flow_execution(id),
    UNIQUE KEY uk_execution_step (execution_id, step_name)
);

-- 流程配置表
CREATE TABLE flow_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(200) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    config_type ENUM('STRING','INTEGER','BOOLEAN','JSON') DEFAULT 'STRING',
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 测试数据

### 加载测试数据

项目提供了完整的测试数据，包括：

1. **3个流程定义**：
   - 用户注册流程 (user-registration-flow v1.0.0)
   - 订单处理流程 (order-processing-flow v1.0.0)
   - 数据备份流程 (data-backup-flow v2.1.0)

2. **4个流程执行记录**：
   - 1个成功的用户注册
   - 1个运行中的订单处理
   - 1个失败的用户注册
   - 1个成功的数据备份

3. **13个步骤执行记录**：
   - 涵盖各种执行状态
   - 包含重试和错误信息

4. **20个系统配置**：
   - 超时设置
   - 重试配置
   - 通知设置
   - 安全配置等

### 使用测试数据

#### 方法1：使用 TestDataInitializer

```java
@Autowired
private TestDataInitializer testDataInitializer;

@Test
void testWithData() {
    // 初始化测试数据
    testDataInitializer.initializeTestData();
    
    // 打印数据统计
    testDataInitializer.printDataStatistics();
    
    // 执行测试逻辑
    // ...
    
    // 清理数据（可选）
    testDataInitializer.cleanupTestData();
}
```

#### 方法2：直接执行 SQL 脚本

```sql
-- 执行测试数据脚本
source src/test/resources/data/test-data.sql;
```

#### 方法3：运行集成测试

```bash
# 运行完整的集成测试
mvn test -Dtest=StorageIntegrationTest
```

### 测试数据示例

#### 查询流程定义
```java
// 查询所有活跃流程
List<FlowDefinition> activeFlows = flowDefinitionService.findActiveFlows();
// 结果：3个流程定义

// 查询最新版本
FlowDefinition latest = flowDefinitionService.findLatestVersionByName("user-registration-flow");
// 结果：版本 1.0.0 的用户注册流程
```

#### 查询执行记录
```java
// 查询运行中的流程
List<FlowExecution> running = flowExecutionService.findRunningExecutions();
// 结果：1个运行中的订单处理流程

// 查询执行统计
Map<ExecutionStatus, Integer> stats = flowExecutionService.getStatusStatistics();
// 结果：SUCCESS=2, FAILED=1, RUNNING=1
```

#### 查询配置
```java
// 获取超时配置
Integer timeout = flowConfigService.getConfigValueAsInteger("flow.execution.timeout");
// 结果：3600 秒

// 检查功能开关
Boolean loggingEnabled = flowConfigService.getConfigValueAsBoolean("flow.logging.enabled");
// 结果：true
```

## 开发指南

### 添加新的流程定义

```java
FlowDefinition newFlow = new FlowDefinition();
newFlow.setFlowName("my-new-flow");
newFlow.setVersion("1.0.0");
newFlow.setDescription("我的新流程");
newFlow.setFlowConfig("{\"steps\":[...]}");
newFlow.setIsActive(true);

FlowDefinition saved = flowDefinitionService.save(newFlow);
```

### 记录流程执行

```java
// 创建流程执行记录
FlowExecution execution = new FlowExecution();
execution.setExecutionId("exec-" + System.currentTimeMillis());
execution.setFlowDefinitionId(flowDefinitionId);
execution.setFlowName("my-flow");
execution.setFlowVersion("1.0.0");
execution.setStatus(ExecutionStatus.RUNNING);

FlowExecution saved = flowExecutionService.save(execution);

// 更新执行状态
flowExecutionService.markAsStarted(saved.getId());
// ... 执行流程逻辑 ...
flowExecutionService.markAsCompleted(saved.getId(), ExecutionStatus.SUCCESS);
```

### 记录步骤执行

```java
// 创建步骤执行记录
StepExecution step = new StepExecution();
step.setFlowExecutionId(flowExecutionId);
step.setExecutionId(executionId);
step.setStepName("my-step");
step.setStatus(ExecutionStatus.RUNNING);

StepExecution saved = stepExecutionService.save(step);

// 更新步骤状态
stepExecutionService.markAsStarted(saved.getId());
// ... 执行步骤逻辑 ...
stepExecutionService.markAsCompleted(saved.getId(), ExecutionStatus.SUCCESS);
```

### 管理配置

```java
// 添加新配置
FlowConfig config = new FlowConfig();
config.setConfigKey("my.config.key");
config.setConfigValue("my-value");
config.setConfigType("STRING");
config.setDescription("我的配置");

FlowConfig saved = flowConfigService.save(config);

// 更新配置值
flowConfigService.updateConfigValue("my.config.key", "new-value");

// 获取配置值
String value = flowConfigService.getConfigValue("my.config.key");
```

## 注意事项

1. **事务管理**：所有服务方法都应该在事务中执行
2. **数据一致性**：确保流程执行和步骤执行的关联关系正确
3. **性能优化**：对于大量数据的查询，建议使用分页
4. **错误处理**：合理处理数据库异常和业务异常
5. **测试隔离**：测试之间应该保持数据隔离，避免相互影响

## 扩展建议

1. **添加索引**：根据实际查询需求添加数据库索引
2. **缓存支持**：对于频繁查询的配置数据，可以添加缓存
3. **监控指标**：添加执行时间、成功率等监控指标
4. **数据归档**：定期清理过期的执行记录
5. **权限控制**：添加基于角色的访问控制