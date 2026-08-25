-- 用户 Todo 列表默认按 id 分页，并且始终过滤 deleted。
CREATE INDEX idx_todos_user_deleted_id
    ON todos (user_id, deleted, id);

-- 用户按完成状态筛选 Todo 时，先匹配用户和生命周期，再匹配 completed。
CREATE INDEX idx_todos_user_deleted_completed_id
    ON todos (user_id, deleted, completed, id);

-- 过期扫描使用 completed、deleted、due_date，并按 id 分页处理。
CREATE INDEX idx_todos_deleted_completed_due_id
    ON todos (deleted, completed, due_date, id);

-- 管理员列表和软删除统计使用 deleted，并通常按 id 排序。
CREATE INDEX idx_todos_deleted_id
    ON todos (deleted, id);
