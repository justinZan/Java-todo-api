package com.zading.todoapi.controller;

import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.ErrorCode;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * 解析 Todo 列表接口的排序参数。
 *
 * <p>排序参数属于 HTTP 查询参数，不应该继续堆积在 Controller 的业务代码中。
 * 独立出来后，Controller 只负责组装请求，解析规则也可以被单独测试。</p>
 */
@Component
public class TodoSortParser {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "title",
            "completed",
            "deleted",
            "priority",
            "dueDate",
            "completedAt",
            "deletedAt",
            "createdAt",
            "updatedAt"
    );

    public Sort parse(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的排序字段: " + field);
        }

        Sort.Direction sortDirection = parseDirection(direction);
        Sort requestedSort = Sort.by(sortDirection, field);

        // 非唯一字段排序时补充 id，保证分页翻页时顺序稳定。
        if (!"id".equals(field)) {
            requestedSort = requestedSort.and(Sort.by(Sort.Direction.ASC, "id"));
        }

        return requestedSort;
    }

    private Sort.Direction parseDirection(String direction) {
        if (Arrays.stream(Sort.Direction.values())
                .map(Enum::name)
                .anyMatch(name -> name.equalsIgnoreCase(direction))) {
            return Sort.Direction.fromString(direction);
        }

        throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的排序方向: " + direction);
    }
}
