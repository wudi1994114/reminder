-- 用户活动日志表
DROP TABLE IF EXISTS user_activity_logs CASCADE;
CREATE TABLE user_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    session_id VARCHAR(100), -- 会话ID，用于关联同一次登录的操作
    action VARCHAR(100) NOT NULL, -- 操作类型
    resource_type VARCHAR(50), -- 资源类型
    resource_id BIGINT, -- 资源ID
    resource_name VARCHAR(200), -- 资源名称（冗余存储，便于查询）
    ip_address INET, -- 客户端IP地址
    user_agent TEXT, -- 用户代理字符串
    request_method VARCHAR(10), -- HTTP方法
    request_url VARCHAR(500), -- 请求URL
    status VARCHAR(20) DEFAULT 'SUCCESS', -- 操作状态：SUCCESS, FAILED, PARTIAL
    error_message TEXT, -- 错误信息（如果操作失败）
    execution_time_ms INTEGER, -- 执行时间（毫秒）
    details TEXT, -- 详细信息（JSON格式字符串）
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 索引优化
CREATE INDEX idx_activity_logs_user_id ON user_activity_logs(user_id);
CREATE INDEX idx_activity_logs_created_at ON user_activity_logs(created_at);
CREATE INDEX idx_activity_logs_user_time ON user_activity_logs(user_id, created_at);
CREATE INDEX idx_activity_logs_action ON user_activity_logs(action);
CREATE INDEX idx_activity_logs_resource ON user_activity_logs(resource_type, resource_id);
CREATE INDEX idx_activity_logs_status ON user_activity_logs(status);
CREATE INDEX idx_activity_logs_session ON user_activity_logs(session_id);

-- 分区表建议（按月分区，提高查询性能）
-- 注意：PostgreSQL 10+ 支持声明式分区
-- CREATE TABLE user_activity_logs_y2024m01 PARTITION OF user_activity_logs
-- FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- 表注释
COMMENT ON TABLE user_activity_logs IS '用户活动日志表，记录用户的各种操作行为';
COMMENT ON COLUMN user_activity_logs.id IS '日志记录的唯一标识符';
COMMENT ON COLUMN user_activity_logs.user_id IS '操作用户的ID，用户删除后设为NULL';
COMMENT ON COLUMN user_activity_logs.session_id IS '用户会话ID，用于关联同一次登录的操作';
COMMENT ON COLUMN user_activity_logs.action IS '操作类型，如LOGIN、CREATE_REMINDER等';
COMMENT ON COLUMN user_activity_logs.resource_type IS '操作的资源类型，如USER、REMINDER等';
COMMENT ON COLUMN user_activity_logs.resource_id IS '操作的资源ID';
COMMENT ON COLUMN user_activity_logs.resource_name IS '资源名称，冗余存储便于查询';
COMMENT ON COLUMN user_activity_logs.ip_address IS '客户端IP地址';
COMMENT ON COLUMN user_activity_logs.user_agent IS '客户端用户代理字符串';
COMMENT ON COLUMN user_activity_logs.request_method IS 'HTTP请求方法';
COMMENT ON COLUMN user_activity_logs.request_url IS '请求的URL路径';
COMMENT ON COLUMN user_activity_logs.status IS '操作状态：SUCCESS成功、FAILED失败、PARTIAL部分成功';
COMMENT ON COLUMN user_activity_logs.error_message IS '操作失败时的错误信息';
COMMENT ON COLUMN user_activity_logs.execution_time_ms IS '操作执行时间（毫秒）';
COMMENT ON COLUMN user_activity_logs.details IS '操作的详细信息，JSON格式存储';
COMMENT ON COLUMN user_activity_logs.created_at IS '日志记录创建时间'; 