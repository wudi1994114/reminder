\set ON_ERROR_STOP on

-- 增量升级：连接现有 saas-admin 数据库后执行，可重复运行；不使用首次初始化脚本。
BEGIN;

CREATE TABLE IF NOT EXISTS reminder.daily_record (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    weight_kg NUMERIC(4, 1),
    height_cm NUMERIC(4, 1),
    diary TEXT,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_daily_record_owner_date UNIQUE (owner_user_id, record_date),
    CONSTRAINT ck_daily_record_date CHECK (record_date BETWEEN DATE '0001-01-01' AND DATE '9999-12-31'),
    CONSTRAINT ck_daily_record_weight CHECK (weight_kg IS NULL OR weight_kg BETWEEN 0.1 AND 999.9),
    CONSTRAINT ck_daily_record_height CHECK (height_cm IS NULL OR height_cm BETWEEN 0.1 AND 300.0),
    CONSTRAINT ck_daily_record_diary CHECK (diary IS NULL OR char_length(diary) BETWEEN 1 AND 10000),
    CONSTRAINT ck_daily_record_content CHECK (weight_kg IS NOT NULL OR height_cm IS NOT NULL OR diary IS NOT NULL),
    CONSTRAINT ck_daily_record_version CHECK (version >= 0)
);

CREATE INDEX IF NOT EXISTS idx_daily_record_height_history
    ON reminder.daily_record (owner_user_id, record_date DESC) WHERE height_cm IS NOT NULL;

COMMENT ON TABLE reminder.daily_record IS '个人每日体重、身高测量和纯文本备注';
COMMENT ON COLUMN reminder.daily_record.record_date IS '用户选择的记录日期，不受服务器时区换日影响';
COMMENT ON COLUMN reminder.daily_record.version IS '乐观锁版本，更新和删除必须匹配读取版本';

COMMIT;
