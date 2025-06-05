# 第一阶段：Maven构建阶段
FROM docker-0.unsee.tech/maven:3.8.1-openjdk-8-slim AS builder

# 设置工作目录
WORKDIR /app

# 复制整个项目结构
COPY . .

# 清理并构建整个项目
# 使用Maven reactor模式，自动处理模块间依赖关系
RUN mvn clean package -DskipTests -B

# 第二阶段：运行时镜像
FROM docker-0.unsee.tech/openjdk:8-jre-slim

# 设置工作目录
WORKDIR /app

# 创建非root用户运行应用（安全最佳实践）
RUN groupadd -r spring && useradd -r -g spring spring

# 从构建阶段复制编译好的jar文件
COPY --from=builder /app/reminder-core/target/*.jar app.jar

# 更改文件所有者
RUN chown spring:spring app.jar

# 切换到非root用户
USER spring

# 暴露应用端口
EXPOSE 8080

# 设置JVM参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 


