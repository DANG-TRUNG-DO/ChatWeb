package com.chatweb.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {

    private List<T> content;
    private String nextCursor;
    private boolean hasMore;
    private int size;

    public static <T> CursorPageResponse<T> of(List<T> content, String nextCursor, boolean hasMore) {
        return CursorPageResponse.<T>builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .size(content != null ? content.size() : 0)
                .build();
    }
}
