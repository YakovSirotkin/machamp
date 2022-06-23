CREATE TABLE async_task
(
    task_id      BIGSERIAL PRIMARY KEY,
    task_type    TEXT,
    description  JSON,
    attempt      SMALLINT    NOT NULL DEFAULT 0,
    priority     INTEGER     NOT NULL DEFAULT 100,
    process_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    taken        TIMESTAMPTZ          DEFAULT NULL
);