CREATE TABLE async_task
(
    task_id      BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_type    VARCHAR(255),
    description  NVARCHAR(max),
    attempt      SMALLINT    NOT NULL DEFAULT 0,
    priority     INTEGER     NOT NULL DEFAULT 100,
    process_time DATETIMEOFFSET NOT NULL DEFAULT GETDATE(),
    created      DATETIMEOFFSET NOT NULL DEFAULT GETDATE(),
    taken        DATETIMEOFFSET          DEFAULT NULL
);
