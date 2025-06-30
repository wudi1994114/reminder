#!/bin/bash

# Reminder应用停止脚本

echo "=== Reminder应用停止脚本 ==="
echo "时间: $(date)"
echo "=========================="

# 停止函数
stop_service() {
    local service_name=$1
    local pid_file=$2
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        echo "停止 $service_name (PID: $pid)..."
        
        if kill -0 "$pid" 2>/dev/null; then
            # 尝试优雅停止
            kill -TERM "$pid"
            
            # 等待进程停止
            local count=0
            while kill -0 "$pid" 2>/dev/null && [ $count -lt 30 ]; do
                echo "等待 $service_name 停止... ($count/30)"
                sleep 1
                count=$((count + 1))
            done
            
            # 如果进程仍在运行，强制停止
            if kill -0 "$pid" 2>/dev/null; then
                echo "强制停止 $service_name..."
                kill -KILL "$pid"
                sleep 2
            fi
            
            if ! kill -0 "$pid" 2>/dev/null; then
                echo "$service_name 已停止"
                rm -f "$pid_file"
            else
                echo "错误: 无法停止 $service_name"
                return 1
            fi
        else
            echo "$service_name 进程不存在，清理PID文件"
            rm -f "$pid_file"
        fi
    else
        echo "$service_name PID文件不存在: $pid_file"
    fi
}

# 创建必要的目录
mkdir -p pids

# 停止Job模块
stop_service "Job模块" "pids/reminder-job.pid"

# 停止Core模块
stop_service "Core模块" "pids/reminder-core.pid"

echo "=== 停止完成 ==="

# 检查是否还有相关进程
echo "检查残留进程..."
REMAINING_PROCESSES=$(ps aux | grep -E "(reminder-core|reminder-job)" | grep -v grep | grep java || true)

if [ -n "$REMAINING_PROCESSES" ]; then
    echo "发现残留进程:"
    echo "$REMAINING_PROCESSES"
    echo ""
    echo "如需强制清理，请手动执行:"
    echo "pkill -f 'reminder-core'"
    echo "pkill -f 'reminder-job'"
else
    echo "没有发现残留进程"
fi

echo "停止脚本执行完成"
