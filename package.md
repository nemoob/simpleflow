

### 包搜索

https://mvnrepository.com/search?q=io.github.nemoob

---

### 打包上传
```bash

# 按依赖顺序构建所有模块
mvn clean compile package -DskipTests -pl simple-flow-api,simple-flow-core,simple-flow-expression,simple-flow-spring-boot-starter,simple-flow-storage,simple-flow-monitor,simple-flow-integration-test -am

```

