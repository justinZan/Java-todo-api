package com.zading.todoapi.controller;

import com.zading.todoapi.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TodoSortParserTest {
    private final TodoSortParser parser = new TodoSortParser();

    @Test
    void shouldAppendIdForStablePaginationWhenSortingByNonUniqueField() {
        Sort sort = parser.parse("title,desc");

        assertEquals(Sort.Direction.DESC, sort.getOrderFor("title").getDirection());
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("id").getDirection());
    }

    @Test
    void shouldKeepIdAsTheOnlySortFieldWhenSortingById() {
        Sort sort = parser.parse("id,asc");

        assertEquals(1, sort.toList().size());
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("id").getDirection());
    }

    @Test
    void shouldRejectUnsupportedSortField() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parse("password,asc")
        );

        assertEquals("不支持的排序字段: password", exception.getMessage());
    }

    @Test
    void shouldRejectUnsupportedSortDirection() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parse("createdAt,sideways")
        );

        assertEquals("不支持的排序方向: sideways", exception.getMessage());
    }
}
