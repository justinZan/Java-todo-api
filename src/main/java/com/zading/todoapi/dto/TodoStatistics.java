package com.zading.todoapi.dto;

/**
 * Todo 表统计查询的聚合结果。
 *
 * @param totalCount      Todo 总数
 * @param activeCount     未软删除 Todo 数量
 * @param completedCount  未软删除且已完成 Todo 数量
 * @param incompleteCount 未软删除且未完成 Todo 数量
 * @param deletedCount    已软删除 Todo 数量
 */
public record TodoStatistics(
        Long totalCount,
        Long activeCount,
        Long completedCount,
        Long incompleteCount,
        Long deletedCount
) {
    public long totalCountOrZero() {
        return valueOrZero(totalCount);
    }

    public long activeCountOrZero() {
        return valueOrZero(activeCount);
    }

    public long completedCountOrZero() {
        return valueOrZero(completedCount);
    }

    public long incompleteCountOrZero() {
        return valueOrZero(incompleteCount);
    }

    public long deletedCountOrZero() {
        return valueOrZero(deletedCount);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
