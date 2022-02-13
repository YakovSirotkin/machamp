CREATE TABLE async_task
(
    task_id      BIGSERIAL PRIMARY KEY,
    task_type    VARCHAR(255),
    description  JSON,
    attempt      SMALLINT    NOT NULL DEFAULT 0,
    process_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    taken        TIMESTAMPTZ DEFAULT NULL
);