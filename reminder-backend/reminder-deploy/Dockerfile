# =============================================================================
# 腾讯云开发专用多阶段构建 Dockerfile
# 适用于上传整个reminder-backend项目，只构建reminder-core的情况
# 腾讯云构建命令：docker build -t [镜像名] .
# =============================================================================

# ===== 第一阶段：构建阶段 =====
FROM maven:3.8.4-openjdk-8 AS builder

# 设置工作目录
WORKDIR /build

# 设置Maven镜像源（使用阿里云镜像加速）
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' >> /root/.m2/settings.xml && \
    echo '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' >> /root/.m2/settings.xml && \
    echo '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0' >> /root/.m2/settings.xml && \
    echo '          http://maven.apache.org/xsd/settings-1.0.0.xsd">' >> /root/.m2/settings.xml && \
    echo '  <mirrors>' >> /root/.m2/settings.xml && \
    echo '    <mirror>' >> /root/.m2/settings.xml && \
    echo '      <id>aliyunmaven</id>' >> /root/.m2/settings.xml && \
    echo '      <mirrorOf>*</mirrorOf>' >> /root/.m2/settings.xml && \
    echo '      <name>阿里云公共仓库</name>' >> /root/.m2/settings.xml && \
    echo '      <url>https://maven.aliyun.com/repository/public</url>' >> /root/.m2/settings.xml && \
    echo '    </mirror>' >> /root/.m2/settings.xml && \
    echo '  </mirrors>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

# 复制父项目的pom.xml
COPY pom.xml /build/

# 复制common模块
COPY reminder-common /build/reminder-common

# 复制reminder-core模块
COPY reminder-core /build/reminder-core

# 首先构建common模块（reminder-core依赖此模块）
WORKDIR /build/reminder-common
RUN mvn clean install -DskipTests -B

# 然后构建reminder-core模块
WORKDIR /build/reminder-core
RUN mvn clean package -DskipTests -B

# 验证JAR文件是否生成
RUN ls -la target/ && \
    if [ ! -f target/*.jar ]; then \
        echo "❌ 错误：JAR文件未生成"; \
        exit 1; \
    fi

# ===== 第二阶段：运行时环境 =====
FROM openjdk:8-jdk-slim AS runtime

# 设置时区为中国标准时间
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 安装curl用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 创建应用用户（安全最佳实践）
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 创建日志目录
RUN mkdir -p /var/log/reminder/core && \
    chown -R appuser:appuser /var/log/reminder && \
    chmod -R 755 /var/log/reminder

# 从构建阶段复制JAR文件
COPY --from=builder /build/reminder-core/target/*.jar app.jar

# 设置文件权限
RUN chown appuser:appuser app.jar

# 切换到应用用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -Djava.security.egd=file:/dev/./urandom"

# Spring Boot配置
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# 元数据标签
LABEL maintainer="reminder-team" \
      version="1.0" \
      description="Reminder Core Service for Tencent Cloud" \
      build-date="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" 