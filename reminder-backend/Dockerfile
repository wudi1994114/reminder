# 第一阶段：Maven构建阶段
FROM docker-0.unsee.tech/maven:3.8.1-openjdk-8-slim AS builder

# 设置工作目录
WORKDIR /app

# 复制父项目pom.xml
COPY pom.xml .

# 复制各个子模块的pom.xml文件（利用Docker层缓存优化）
COPY reminder-common/pom.xml ./reminder-common/
COPY reminder-core/pom.xml ./reminder-core/
COPY reminder-job/pom.xml ./reminder-job/

# 先安装父pom到本地仓库（解决多模块依赖问题）
RUN mvn install -N -B

# 预下载Maven依赖（这一层会被缓存，除非pom.xml发生变化）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY reminder-common/src ./reminder-common/src
COPY reminder-core/src ./reminder-core/src
COPY reminder-job/src ./reminder-job/src

# 执行Maven构建，跳过测试以加快构建速度
# 首先构建并安装common模块，然后构建core模块
RUN mvn clean install -pl reminder-common -DskipTests -B && \
    mvn clean package -pl reminder-core -DskipTests -B

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


