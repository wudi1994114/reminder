-- 数据库管理命令 (通常在连接到数据库服务器后，在 psql 命令行或其他工具中单独执行):
-- 1. 创建数据库 (如果不存在):
CREATE DATABASE remind WITH OWNER your_username;
-- 替换 your_username 为实际的数据库用户
-- 2. 连接到新创建的数据库:
--    \c remind

-- 删除可能存在的旧表 (将移动到各自创建语句前)
-- DROP TABLE IF EXISTS reminder_execution_history;
-- DROP TABLE IF EXISTS complex_reminder;
-- DROP TABLE IF EXISTS simple_reminder;
-- DROP TABLE IF EXISTS app_user;

-- 创建用户表 (app_user)
DROP TABLE IF EXISTS app_user CASCADE;
-- 使用 CASCADE 以防未来添加依赖
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY, -- 用户唯一标识符，自增
    username VARCHAR(100) UNIQUE NOT NULL, -- 用户登录名，唯一且不允许为空
    password VARCHAR(255) NOT NULL, -- 加密后的用户密码，不允许为空
    nickname VARCHAR(100) NOT NULL, -- 用户昵称，不允许为空
    email VARCHAR(255) NOT NULL, -- 用户邮箱，不允许为空
    phone_number VARCHAR(20) UNIQUE, -- 用户手机号，唯一 (如果提供的话)
    avatar_url TEXT, -- 用户头像 URL (允许为空)
    gender VARCHAR(50), -- 用户性别 (例如: 'Male', 'Female', 'Other', 'Prefer not to say')
    birth_date DATE, -- 用户出生日期
    created_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 记录创建时间
        updated_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL -- 记录最后更新时间
);

-- 为用户表常用查询字段创建索引
CREATE INDEX idx_user_username ON app_user (username);

CREATE INDEX idx_user_email ON app_user (email);

CREATE INDEX idx_user_phone_number ON app_user (phone_number);

-- 用户表注释
COMMENT ON TABLE app_user IS '存储用户信息';

COMMENT ON COLUMN app_user.id IS '用户的唯一标识符 (主键)';

COMMENT ON COLUMN app_user.username IS '用户的登录名 (唯一)';

COMMENT ON COLUMN app_user.password IS '用户加密后的密码哈希';

COMMENT ON COLUMN app_user.nickname IS '用户的昵称';

COMMENT ON COLUMN app_user.email IS '用户的电子邮箱地址';

COMMENT ON COLUMN app_user.phone_number IS '用户的手机号码 (唯一，可选)';

COMMENT ON COLUMN app_user.avatar_url IS '用户头像的 URL 地址 (可选)';

COMMENT ON COLUMN app_user.gender IS '用户的性别';

COMMENT ON COLUMN app_user.birth_date IS '用户的出生日期';

COMMENT ON COLUMN app_user.created_at IS '用户记录的创建时间戳';

COMMENT ON COLUMN app_user.updated_at IS '用户记录的最后更新时间戳';

-- 创建微信用户表 (wechat_user)
DROP TABLE IF EXISTS wechat_user CASCADE;

CREATE TABLE wechat_user (
    id BIGSERIAL PRIMARY KEY, -- 微信用户唯一标识符，自增
    app_user_id BIGINT NOT NULL, -- 关联的系统用户ID
    openid VARCHAR(100) UNIQUE NOT NULL, -- 微信小程序的openid，唯一标识用户
    unionid VARCHAR(100), -- 微信unionid，用于关联同一微信开放平台下的应用
    nickname VARCHAR(100), -- 微信用户昵称
    avatar_url TEXT, -- 微信用户头像URL
    gender INTEGER, -- 微信用户性别：0-未知，1-男，2-女
    country VARCHAR(50), -- 微信用户所在国家
    province VARCHAR(50), -- 微信用户所在省份
    city VARCHAR(50), -- 微信用户所在城市
    language VARCHAR(20), -- 微信用户语言
    session_key VARCHAR(100), -- 会话密钥
    last_login_time TIMESTAMP
    WITH
        TIME ZONE, -- 最后登录时间
        created_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 记录创建时间
        updated_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL -- 记录最后更新时间
);

-- 为微信用户表常用查询字段创建索引
CREATE INDEX idx_wechat_user_openid ON wechat_user (openid);

CREATE INDEX idx_wechat_user_app_user_id ON wechat_user (app_user_id);

CREATE INDEX idx_wechat_user_unionid ON wechat_user (unionid);

CREATE INDEX idx_wechat_user_last_login ON wechat_user (last_login_time);

-- 微信用户表注释
COMMENT ON TABLE wechat_user IS '存储微信小程序用户信息';

COMMENT ON COLUMN wechat_user.id IS '微信用户的唯一标识符 (主键)';

COMMENT ON COLUMN wechat_user.app_user_id IS '关联的系统用户ID';

COMMENT ON COLUMN wechat_user.openid IS '微信小程序的openid，唯一标识用户';

COMMENT ON COLUMN wechat_user.unionid IS '微信unionid，用于关联同一微信开放平台下的应用';

COMMENT ON COLUMN wechat_user.nickname IS '微信用户昵称';

COMMENT ON COLUMN wechat_user.avatar_url IS '微信用户头像URL';

COMMENT ON COLUMN wechat_user.gender IS '微信用户性别：0-未知，1-男，2-女';

COMMENT ON COLUMN wechat_user.country IS '微信用户所在国家';

COMMENT ON COLUMN wechat_user.province IS '微信用户所在省份';

COMMENT ON COLUMN wechat_user.city IS '微信用户所在城市';

COMMENT ON COLUMN wechat_user.language IS '微信用户语言';

COMMENT ON COLUMN wechat_user.session_key IS '会话密钥';

COMMENT ON COLUMN wechat_user.last_login_time IS '最后登录时间';

COMMENT ON COLUMN wechat_user.created_at IS '微信用户记录的创建时间戳';

COMMENT ON COLUMN wechat_user.updated_at IS '微信用户记录的最后更新时间戳';

-- 创建复杂提醒模板表 (complex_reminder)
DROP TABLE IF EXISTS complex_reminder CASCADE;
-- 注意：移除了 related_simple_reminder_id 和用户外键
CREATE TABLE complex_reminder (
    id BIGSERIAL PRIMARY KEY, -- 复杂提醒模板唯一标识符
    from_user_id BIGINT, -- 创建提醒模板的用户 ID
    to_user_id BIGINT NOT NULL, -- 接收提醒的目标用户 ID
    title TEXT NOT NULL, -- 提醒标题模板
    description TEXT, -- 提醒描述模板
    cron_expression VARCHAR(255) NOT NULL, -- 定义重复规则的 CRON 表达式
    reminder_type VARCHAR(50) NOT NULL, -- 提醒方式 (例如: 'EMAIL', 'SMS')
    valid_from DATE, -- 提醒生效开始日期
    valid_until DATE, -- 提醒失效日期
    max_executions INTEGER, -- 最大执行次数限制
    last_generated_ym INTEGER, -- 最后生成简单任务的年月(格式YYYYMM，如202405表示2024年5月)
    idempotency_key VARCHAR(255), -- 幂等键，用于防止重复创建
    created_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 记录创建时间
        updated_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL -- 记录最后更新时间
);

-- 为复杂提醒表常用查询字段创建索引
CREATE INDEX idx_complex_reminder_from_user ON complex_reminder (from_user_id);

CREATE INDEX idx_complex_reminder_to_user ON complex_reminder (to_user_id);

CREATE INDEX idx_complex_reminder_valid_range ON complex_reminder (valid_from, valid_until);
-- 添加有效期范围索引
CREATE INDEX idx_complex_reminder_last_generated ON complex_reminder (last_generated_ym);
-- 添加幂等键的唯一索引，防止重复创建
CREATE UNIQUE INDEX idx_complex_reminder_idempotency_key ON complex_reminder (idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 复杂提醒表注释
COMMENT ON TABLE complex_reminder IS '存储基于 CRON 表达式的重复提醒模板';

COMMENT ON COLUMN complex_reminder.id IS '复杂提醒模板的唯一标识符 (主键)';

COMMENT ON COLUMN complex_reminder.from_user_id IS '创建此提醒模板的用户 ID';

COMMENT ON COLUMN complex_reminder.to_user_id IS '接收此提醒的目标用户 ID';

COMMENT ON COLUMN complex_reminder.title IS '提醒的标题模板';

COMMENT ON COLUMN complex_reminder.description IS '提醒的详细描述模板';

COMMENT ON COLUMN complex_reminder.cron_expression IS '定义重复周期的 CRON 表达式';

COMMENT ON COLUMN complex_reminder.reminder_type IS '提醒的方式 (如 EMAIL, SMS)';

COMMENT ON COLUMN complex_reminder.valid_from IS '提醒开始生效的日期，为空则立即生效';

COMMENT ON COLUMN complex_reminder.valid_until IS '提醒失效的日期，为空则永不失效';

COMMENT ON COLUMN complex_reminder.max_executions IS '提醒最大执行次数限制，为空则无限制';

COMMENT ON COLUMN complex_reminder.last_generated_ym IS '最后生成简单任务的年月(格式YYYYMM，如202405表示2024年5月)';

COMMENT ON COLUMN complex_reminder.idempotency_key IS '幂等键，用于防止重复创建相同的复杂提醒';

COMMENT ON COLUMN complex_reminder.created_at IS '提醒记录的创建时间戳';

COMMENT ON COLUMN complex_reminder.updated_at IS '提醒记录的最后更新时间戳';

-- 创建简单提醒实例表 (simple_reminder)
DROP TABLE IF EXISTS simple_reminder CASCADE;
-- 注意：添加了 originating_complex_reminder_id，移除了用户外键
CREATE TABLE simple_reminder (
    id BIGSERIAL PRIMARY KEY, -- 简单提醒实例唯一标识符
    from_user_id BIGINT, -- 创建提醒的用户 ID
    to_user_id BIGINT NOT NULL, -- 接收提醒的用户 ID
    title TEXT NOT NULL, -- 提醒标题
    description TEXT, -- 提醒描述
    event_time TIMESTAMP
    WITH
        TIME ZONE NOT NULL, -- 提醒触发的精确时间点
        reminder_type VARCHAR(50) NOT NULL, -- 提醒方式 (例如: 'EMAIL', 'SMS')
        originating_complex_reminder_id BIGINT, -- 生成此实例的复杂提醒模板 ID (可选，无外键约束)
        created_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 记录创建时间
        updated_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL -- 记录最后更新时间
);

-- 为简单提醒表常用查询字段创建索引
-- CREATE INDEX idx_simple_reminder_event_time ON simple_reminder (event_time);
CREATE INDEX idx_simple_reminder_event_time_user ON simple_reminder (event_time, to_user_id);

CREATE INDEX idx_simple_reminder_from_user ON simple_reminder (from_user_id);

CREATE INDEX idx_simple_reminder_to_user ON simple_reminder (to_user_id);

CREATE INDEX idx_simple_reminder_complex ON simple_reminder (
    originating_complex_reminder_id
);
-- 新增索引
COMMENT ON TABLE simple_reminder IS '存储一次性的提醒实例 (可能由复杂模板生成)';

COMMENT ON COLUMN simple_reminder.id IS '简单提醒实例的唯一标识符 (主键)';

COMMENT ON COLUMN simple_reminder.from_user_id IS '创建此提醒实例的用户 ID';

COMMENT ON COLUMN simple_reminder.to_user_id IS '接收此提醒实例的用户 ID';

COMMENT ON COLUMN simple_reminder.title IS '提醒的标题';

COMMENT ON COLUMN simple_reminder.description IS '提醒的详细描述';

COMMENT ON COLUMN simple_reminder.event_time IS '提醒计划发生的精确时间戳';

COMMENT ON COLUMN simple_reminder.reminder_type IS '提醒的方式 (如 EMAIL, SMS)';

COMMENT ON COLUMN simple_reminder.originating_complex_reminder_id IS '生成此简单提醒实例的复杂提醒模板 ID (可选，无外键约束)';
-- 新增注释
COMMENT ON COLUMN simple_reminder.created_at IS '提醒记录的创建时间戳';

COMMENT ON COLUMN simple_reminder.updated_at IS '提醒记录的最后更新时间戳';

-- 创建提醒执行历史记录表 (包含冗余信息以方便查询)
DROP TABLE IF EXISTS reminder_execution_history CASCADE;

CREATE TABLE reminder_execution_history (
    id BIGSERIAL PRIMARY KEY, -- 历史记录唯一标识符
    executed_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 任务实际执行时间戳
        triggering_reminder_type VARCHAR(50) NOT NULL, -- 触发源类型 ('SIMPLE', 'COMPLEX')
        triggering_reminder_id BIGINT NOT NULL, -- 触发源提醒的 ID (simple_reminder.id 或 complex_reminder.id)
        from_user_id BIGINT NOT NULL, -- 创建原始提醒/模板的用户 ID (冗余)
        to_user_id BIGINT NOT NULL, -- 接收提醒的目标用户 ID (冗余)
        title TEXT, -- 执行时的提醒标题 (冗余)
        description TEXT, -- 执行时的提醒描述 (冗余)
        actual_reminder_method VARCHAR(50) NOT NULL, -- 实际使用的提醒方式 (例如: 'EMAIL', 'SMS') (冗余)
        scheduled_event_time TIMESTAMP
    WITH
        TIME ZONE, -- 对于 SIMPLE 触发源，记录原定计划的事件时间 (可选, 冗余)
        status VARCHAR(50) NOT NULL, -- 执行结果状态 ('SUCCESS', 'FAILURE')
        details TEXT -- 执行详情或错误信息
);

-- 为历史记录表创建索引
CREATE INDEX idx_history_executed_at ON reminder_execution_history (executed_at);

CREATE INDEX idx_history_triggering_reminder ON reminder_execution_history (
    triggering_reminder_type,
    triggering_reminder_id
);

CREATE INDEX idx_history_from_user ON reminder_execution_history (from_user_id);

CREATE INDEX idx_history_to_user ON reminder_execution_history (to_user_id);

CREATE INDEX idx_history_status ON reminder_execution_history (status);

-- 历史记录表注释
COMMENT ON
TABLE reminder_execution_history IS '记录提醒任务的执行历史 (包含冗余信息以便单表查询)';

COMMENT ON COLUMN reminder_execution_history.id IS '历史记录的唯一标识符 (主键)';

COMMENT ON COLUMN reminder_execution_history.executed_at IS '任务实际执行的时间戳';

COMMENT ON COLUMN reminder_execution_history.triggering_reminder_type IS '触发执行的提醒源类型 (SIMPLE 或 COMPLEX)';

COMMENT ON COLUMN reminder_execution_history.triggering_reminder_id IS '触发执行的提醒源的 ID (simple_reminder.id 或 complex_reminder.id)';

COMMENT ON COLUMN reminder_execution_history.from_user_id IS '创建原始提醒/模板的用户 ID (冗余存储)';

COMMENT ON COLUMN reminder_execution_history.to_user_id IS '接收提醒的目标用户 ID (冗余存储)';

COMMENT ON COLUMN reminder_execution_history.title IS '执行时的提醒标题 (冗余存储)';

COMMENT ON COLUMN reminder_execution_history.description IS '执行时的提醒描述 (冗余存储)';

COMMENT ON COLUMN reminder_execution_history.actual_reminder_method IS '实际使用的提醒方式 (如 EMAIL, SMS) (冗余存储)';

COMMENT ON COLUMN reminder_execution_history.scheduled_event_time IS '对于 SIMPLE 触发源，记录原定计划的事件时间 (冗余存储)';

COMMENT ON COLUMN reminder_execution_history.status IS '执行结果的状态 (SUCCESS, FAILURE)';

COMMENT ON COLUMN reminder_execution_history.details IS '执行的详细信息或错误日志';

-- 同样，为所有表的 updated_at 字段考虑触发器或应用层逻辑
-- CREATE OR REPLACE FUNCTION update_updated_at_column() ...
-- CREATE TRIGGER update_user_updated_at BEFORE UPDATE ON app_user ...
-- CREATE TRIGGER update_simple_reminder_updated_at BEFORE UPDATE ON simple_reminder ...
-- CREATE TRIGGER update_complex_reminder_updated_at BEFORE UPDATE ON complex_reminder ...

-- 创建法定节假日表
DROP TABLE IF EXISTS legal_holiday CASCADE;

CREATE TABLE legal_holiday (
    id BIGSERIAL PRIMARY KEY, -- 主键
    year INTEGER NOT NULL, -- 年份
    month INTEGER NOT NULL, -- 月份（1-12）
    day INTEGER NOT NULL, -- 日期（1-31）
    holiday BOOLEAN NOT NULL, -- 是否是节假日（true为节假日，false为调休）
    name VARCHAR(100) NOT NULL, -- 节假日名称
    created_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 记录创建时间
        updated_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL -- 记录最后更新时间
);

-- 为法定节假日表创建索引
CREATE INDEX idx_legal_holiday_date ON legal_holiday (year, month, day);

-- 添加唯一约束，确保同一天不会重复
CREATE UNIQUE INDEX idx_legal_holiday_unique_date ON legal_holiday (year, month, day);

-- 法定节假日表注释
COMMENT ON TABLE legal_holiday IS '存储法定节假日信息';

COMMENT ON COLUMN legal_holiday.id IS '节假日记录的唯一标识符 (主键)';

COMMENT ON COLUMN legal_holiday.year IS '节假日所在年份';

COMMENT ON COLUMN legal_holiday.month IS '节假日所在月份（1-12）';

COMMENT ON COLUMN legal_holiday.day IS '节假日所在日期（1-31）';

COMMENT ON COLUMN legal_holiday.holiday IS '是否是节假日（true为节假日，false为调休）';

COMMENT ON COLUMN legal_holiday.name IS '节假日名称';

COMMENT ON COLUMN legal_holiday.created_at IS '记录的创建时间戳';

COMMENT ON COLUMN legal_holiday.updated_at IS '记录的最后更新时间戳';

-- 创建二十四节气表 (solar_term)
DROP TABLE IF EXISTS solar_term CASCADE;

CREATE TABLE solar_term (
    id BIGSERIAL PRIMARY KEY, -- 节气唯一标识符，自增
    name VARCHAR(50) NOT NULL, -- 节气名称 (例如: '立春', '雨水')
    year INTEGER NOT NULL, -- 年份
    month INTEGER NOT NULL, -- 月份（1-12）
    day INTEGER NOT NULL, -- 日期（1-31）
    suitable TEXT, -- 宜：适宜做的事情，较长文本
    taboo TEXT, -- 忌：不宜做的事情，较长文本
    created_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 记录创建时间
        updated_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL -- 记录最后更新时间
);

-- 为节气表常用查询字段创建索引
CREATE INDEX idx_solar_term_date ON solar_term (year, month, day);

CREATE INDEX idx_solar_term_name_year ON solar_term (name, year);

-- 添加唯一约束，确保同一天不会重复记录同一个节气 (理论上一年只有一个同名节气，但为保险起见加上日期)
CREATE UNIQUE INDEX idx_solar_term_unique_date_name ON solar_term (year, month, day, name);

-- 节气表注释
COMMENT ON TABLE solar_term IS '存储二十四节气信息，包含宜忌';

COMMENT ON COLUMN solar_term.id IS '节气的唯一标识符 (主键)';

COMMENT ON COLUMN solar_term.name IS '节气的名称';

COMMENT ON COLUMN solar_term.year IS '节气所在年份';

COMMENT ON COLUMN solar_term.month IS '节气所在月份（1-12）';

COMMENT ON COLUMN solar_term.day IS '节气所在日期（1-31）';

COMMENT ON COLUMN solar_term.suitable IS '该节气适宜做的事情';

COMMENT ON COLUMN solar_term.taboo IS '该节气不宜做的事情';

COMMENT ON COLUMN solar_term.created_at IS '记录的创建时间戳';

COMMENT ON COLUMN solar_term.updated_at IS '记录的最后更新时间戳';

-- 用户活动日志表
DROP TABLE IF EXISTS user_activity_logs CASCADE;

CREATE TABLE user_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
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
    details TEXT, -- 详细信息（JSON格式）
    created_at TIMESTAMP
    WITH
        TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 索引优化
CREATE INDEX idx_activity_logs_user_id ON user_activity_logs (user_id);

CREATE INDEX idx_activity_logs_created_at ON user_activity_logs (created_at);

CREATE INDEX idx_activity_logs_user_time ON user_activity_logs (user_id, created_at);

CREATE INDEX idx_activity_logs_action ON user_activity_logs (action);

CREATE INDEX idx_activity_logs_resource ON user_activity_logs (resource_type, resource_id);

CREATE INDEX idx_activity_logs_status ON user_activity_logs (status);

CREATE INDEX idx_activity_logs_session ON user_activity_logs (session_id);

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

-- 创建用户偏好设置表 (user_preference) - 键值对存储模式
DROP TABLE IF EXISTS user_preference CASCADE;

CREATE TABLE user_preference (
    id BIGSERIAL PRIMARY KEY, -- 偏好设置唯一标识符，自增
    user_id BIGINT NOT NULL, -- 用户ID，关联app_user表
    key VARCHAR(100) NOT NULL, -- 偏好设置键名
    value TEXT, -- 偏好设置值，存储为字符串
    property VARCHAR(500), -- 偏好设置属性/描述（可选）
    create_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
    modify_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL -- 修改时间
);

-- 创建索引
CREATE INDEX idx_user_preference_user_id ON user_preference (user_id);

-- 添加唯一约束，确保同一用户的同一键名只有一条记录
CREATE UNIQUE INDEX idx_user_preference_unique_user_key ON user_preference (user_id, key);

-- 表和字段注释
COMMENT ON TABLE user_preference IS '用户偏好设置表，采用键值对存储模式';
COMMENT ON COLUMN user_preference.id IS '偏好设置的唯一标识符 (主键)';
COMMENT ON COLUMN user_preference.user_id IS '关联的用户ID';
COMMENT ON COLUMN user_preference.key IS '偏好设置键名，如defaultReminderType、theme等';
COMMENT ON COLUMN user_preference.value IS '偏好设置值，以字符串形式存储';
COMMENT ON COLUMN user_preference.property IS '偏好设置的描述或额外属性信息';
COMMENT ON COLUMN user_preference.create_at IS '记录创建时间';
COMMENT ON COLUMN user_preference.modify_at IS '记录最后修改时间';


-- 创建用户反馈表 (user_feedback)
DROP TABLE IF EXISTS user_feedback CASCADE;
CREATE TABLE user_feedback
(
    id          BIGSERIAL PRIMARY KEY,                                       -- 反馈唯一标识符，自增
    user_id     BIGINT,                                                      -- 用户ID，可为空（支持匿名反馈）
    email       VARCHAR(255),                                                -- 用户邮箱，可为空
    message     TEXT                                               NOT NULL, -- 用户反馈内容
    create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL  -- 反馈创建时间
);

-- 创建索引
CREATE INDEX idx_user_feedback_create_time ON user_feedback (create_time);
CREATE INDEX idx_user_feedback_user_id ON user_feedback (user_id);

-- 表和字段注释
COMMENT ON TABLE user_feedback IS '用户反馈表，存储用户提交的反馈信息';
COMMENT ON COLUMN user_feedback.id IS '反馈记录的唯一标识符，自增主键';
COMMENT ON COLUMN user_feedback.user_id IS '提交反馈的用户ID，可为空支持匿名反馈';
COMMENT ON COLUMN user_feedback.email IS '用户邮箱地址，可为空';
COMMENT ON COLUMN user_feedback.message IS '用户反馈的具体内容，支持长文本';
COMMENT ON COLUMN user_feedback.create_time IS '反馈记录的创建时间，自动设置为当前时间';
