#!/bin/bash

# Reminder应用启动脚本 - 使用Nacos配置中心
# 使用方法: ./start-with-nacos.sh [dev|prod|test]

# 设置脚本错误时退出
set -e

# 获取环境参数，默认为dev
ENVIRONMENT=${1:-dev}

echo "=== Reminder应用启动脚本 ==="
echo "环境: $ENVIRONMENT"
echo "时间: $(date)"
echo "=========================="

# 根据环境设置不同的配置
case $ENVIRONMENT in
    "dev")
        echo "配置开发环境..."
        export SPRING_PROFILES_ACTIVE=dev
        export NACOS_SERVER_ADDR=localhost:8848
        export NACOS_NAMESPACE=dev
        export NACOS_GROUP=DEFAULT_GROUP
        export NACOS_USERNAME=nacos
        export NACOS_PASSWORD=nacos
        
        # 开发环境数据库配置
        export DATABASE_URL=jdbc:postgresql://localhost:5432/remind_dev
        export DATABASE_USERNAME=dev_user
        export DATABASE_PASSWORD=dev_password
        
        # 开发环境Redis配置
        export REDIS_HOST=localhost
        export REDIS_PORT=6379
        export REDIS_PASSWORD=""
        export REDIS_DATABASE=1
        ;;
        
    "prod")
        echo "配置生产环境..."
        export SPRING_PROFILES_ACTIVE=prod
        export NACOS_SERVER_ADDR=${PROD_NACOS_SERVER_ADDR:-prod-nacos-server:8848}
        export NACOS_NAMESPACE=${PROD_NACOS_NAMESPACE:-prod}
        export NACOS_GROUP=${PROD_NACOS_GROUP:-DEFAULT_GROUP}
        export NACOS_USERNAME=${PROD_NACOS_USERNAME}
        export NACOS_PASSWORD=${PROD_NACOS_PASSWORD}
        
        # 生产环境配置从环境变量获取
        export DATABASE_URL=${PROD_DATABASE_URL}
        export DATABASE_USERNAME=${PROD_DATABASE_USERNAME}
        export DATABASE_PASSWORD=${PROD_DATABASE_PASSWORD}
        
        export REDIS_HOST=${PROD_REDIS_HOST}
        export REDIS_PORT=${PROD_REDIS_PORT:-6379}
        export REDIS_PASSWORD=${PROD_REDIS_PASSWORD}
        export REDIS_DATABASE=${PROD_REDIS_DATABASE:-0}
        ;;
        
    "test")
        echo "配置测试环境..."
        export SPRING_PROFILES_ACTIVE=test
        export NACOS_SERVER_ADDR=test-nacos-server:8848
        export NACOS_NAMESPACE=test
        export NACOS_GROUP=DEFAULT_GROUP
        export NACOS_USERNAME=nacos
        export NACOS_PASSWORD=nacos
        ;;
        
    *)
        echo "错误: 不支持的环境 '$ENVIRONMENT'"
        echo "支持的环境: dev, prod, test"
        exit 1
        ;;
esac

# 检查必要的环境变量
check_required_env() {
    local var_name=$1
    local var_value=${!var_name}
    
    if [ -z "$var_value" ]; then
        echo "错误: 环境变量 $var_name 未设置"
        return 1
    fi
}

echo "检查必要的环境变量..."
check_required_env "NACOS_SERVER_ADDR"
check_required_env "SPRING_PROFILES_ACTIVE"

# 打印配置信息（隐藏敏感信息）
echo "=== 配置信息 ==="
echo "Spring Profile: $SPRING_PROFILES_ACTIVE"
echo "Nacos服务器: $NACOS_SERVER_ADDR"
echo "Nacos命名空间: ${NACOS_NAMESPACE:-默认}"
echo "Nacos分组: $NACOS_GROUP"
echo "==============="

# 设置JVM参数
JVM_OPTS="-Xms512m -Xmx1024m"
JVM_OPTS="$JVM_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE"
JVM_OPTS="$JVM_OPTS -Dspring.cloud.bootstrap.enabled=true"
JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8"
JVM_OPTS="$JVM_OPTS -Duser.timezone=Asia/Shanghai"

# 根据环境调整JVM参数
if [ "$ENVIRONMENT" = "prod" ]; then
    JVM_OPTS="-Xms1024m -Xmx2048m $JVM_OPTS"
    JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
    JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"
fi

echo "JVM参数: $JVM_OPTS"

# 检查JAR文件是否存在
CORE_JAR="reminder-core/target/reminder-core-0.0.1-SNAPSHOT.jar"
JOB_JAR="reminder-job/target/reminder-job-0.0.1-SNAPSHOT.jar"

if [ ! -f "$CORE_JAR" ]; then
    echo "错误: Core模块JAR文件不存在: $CORE_JAR"
    echo "请先执行: mvn clean package"
    exit 1
fi

if [ ! -f "$JOB_JAR" ]; then
    echo "错误: Job模块JAR文件不存在: $JOB_JAR"
    echo "请先执行: mvn clean package"
    exit 1
fi

# 启动应用
echo "=== 启动应用 ==="

# 启动Core模块
echo "启动Core模块..."
nohup java $JVM_OPTS -jar $CORE_JAR > logs/reminder-core-$ENVIRONMENT.log 2>&1 &
CORE_PID=$!
echo "Core模块PID: $CORE_PID"

# 等待Core模块启动
sleep 10

# 启动Job模块
echo "启动Job模块..."
nohup java $JVM_OPTS -jar $JOB_JAR > logs/reminder-job-$ENVIRONMENT.log 2>&1 &
JOB_PID=$!
echo "Job模块PID: $JOB_PID"

# 保存PID到文件
mkdir -p pids
echo $CORE_PID > pids/reminder-core.pid
echo $JOB_PID > pids/reminder-job.pid

echo "=== 启动完成 ==="
echo "Core模块PID: $CORE_PID (保存到 pids/reminder-core.pid)"
echo "Job模块PID: $JOB_PID (保存到 pids/reminder-job.pid)"
echo "日志文件:"
echo "  Core: logs/reminder-core-$ENVIRONMENT.log"
echo "  Job: logs/reminder-job-$ENVIRONMENT.log"
echo ""
echo "使用以下命令查看日志:"
echo "  tail -f logs/reminder-core-$ENVIRONMENT.log"
echo "  tail -f logs/reminder-job-$ENVIRONMENT.log"
echo ""
echo "使用以下命令停止应用:"
echo "  ./stop-reminder.sh"
