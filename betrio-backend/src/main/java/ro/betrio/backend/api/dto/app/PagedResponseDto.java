package ro.betrio.backend.api.dto.app;

import java.util.List;

public record PagedResponseDto<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        String sortBy,
        String sortDir
) {
}