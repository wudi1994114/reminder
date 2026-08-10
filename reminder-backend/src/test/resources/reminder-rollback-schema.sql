DROP TABLE IF EXISTS simple_reminder;
DROP TABLE IF EXISTS complex_reminder;

CREATE TABLE complex_reminder (
    id BIGSERIAL PRIMARY KEY,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    cron_expression VARCHAR(255) NOT NULL,
    reminder_type VARCHAR(50) NOT NULL,
    valid_from DATE,
    valid_until DATE,
    max_executions INTEGER,
    last_generated_ym INTEGER,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE simple_reminder (
    id BIGSERIAL PRIMARY KEY,
    from_user_id BIGINT,
    to_user_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    reminder_type VARCHAR(50) NOT NULL,
    originating_complex_reminder_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
