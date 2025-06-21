-- 为复杂提醒表添加幂等性支持
-- 执行时间：2024年6月21日
-- 说明：添加幂等键字段和相关约束，防止重复创建复杂任务

-- 步骤1：添加幂等键字段
ALTER TABLE complex_reminder ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

-- 步骤2：添加字段注释
COMMENT ON COLUMN complex_reminder.idempotency_key IS '幂等键，用于防止重复创建相同的复杂提醒';

-- 步骤3：添加幂等键的唯一索引（只对非空值生效）
CREATE UNIQUE INDEX IF NOT EXISTS idx_complex_reminder_idempotency_key 
ON complex_reminder (idempotency_key) 
WHERE idempotency_key IS NOT NULL;

-- 步骤4：添加业务唯一约束，防止相同用户创建完全相同的复杂提醒
-- 使用COALESCE处理NULL值，确保约束的一致性
CREATE UNIQUE INDEX IF NOT EXISTS idx_complex_reminder_business_unique 
ON complex_reminder (
    from_user_id, 
    to_user_id, 
    title, 
    cron_expression, 
    reminder_type, 
    COALESCE(valid_from, '1900-01-01'::date), 
    COALESCE(valid_until, '9999-12-31'::date),
    COALESCE(max_executions, -1)
);

-- 步骤5：为现有数据生成幂等键（可选，如果需要的话）
-- 注意：这个步骤在生产环境中需要谨慎执行，可能需要分批处理大量数据
-- UPDATE complex_reminder 
-- SET idempotency_key = 'legacy_' || id::text 
-- WHERE idempotency_key IS NULL;

-- 验证脚本执行结果
-- 检查字段是否添加成功
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_name = 'complex_reminder' 
AND column_name = 'idempotency_key';

-- 检查索引是否创建成功
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'complex_reminder' 
AND (indexname = 'idx_complex_reminder_idempotency_key' 
     OR indexname = 'idx_complex_reminder_business_unique');

-- 检查表结构
\d complex_reminder;
