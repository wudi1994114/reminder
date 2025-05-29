-- 数据库迁移脚本：更新用户活动日志表
-- 将 details 字段从 JSONB 改为 TEXT

-- 检查表是否存在
DO $$
BEGIN
    -- 如果表存在且 details 字段是 JSONB 类型，则进行转换
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'user_activity_logs' 
        AND column_name = 'details' 
        AND data_type = 'jsonb'
    ) THEN
        -- 备份现有数据（将 JSONB 转换为 TEXT）
        ALTER TABLE user_activity_logs 
        ALTER COLUMN details TYPE TEXT USING details::TEXT;
        
        RAISE NOTICE '已将 user_activity_logs.details 字段从 JSONB 转换为 TEXT';
    ELSE
        RAISE NOTICE 'user_activity_logs 表不存在或 details 字段已经是 TEXT 类型';
    END IF;
END $$; 