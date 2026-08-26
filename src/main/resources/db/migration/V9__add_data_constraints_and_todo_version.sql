-- 第 25 周：把关键业务规则下沉到数据库，避免绕过 Java API 的写入破坏数据。

-- Todo 的版本号用于 JPA 乐观锁，初始值为 0。
-- Hibernate 更新 Todo 时会同时检查旧版本号，成功后自动加 1。
ALTER TABLE todos
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 标题不能只是空格；Java 层负责友好提示，数据库层负责最终兜底。
ALTER TABLE todos
    ADD CONSTRAINT ck_todos_title_not_blank
    CHECK (TRIM(title) <> '');

-- 枚举值以字符串保存，数据库约束防止出现 Java 枚举之外的值。
ALTER TABLE todos
    ADD CONSTRAINT ck_todos_priority
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE users
    ADD CONSTRAINT ck_users_role
    CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE todo_action_logs
    ADD CONSTRAINT ck_todo_action_logs_action
    CHECK (action IN ('CREATED', 'UPDATED', 'COMPLETED', 'UNCOMPLETED', 'DELETED', 'RESTORED', 'OVERDUE'));

-- 文件大小不能为负数；文件内容本身保存在文件系统，元数据仍由数据库约束。
ALTER TABLE todo_attachments
    ADD CONSTRAINT ck_todo_attachments_file_size
    CHECK (file_size >= 0);
