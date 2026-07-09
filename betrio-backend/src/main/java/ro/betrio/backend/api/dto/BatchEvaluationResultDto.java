package ro.betrio.backend.api.dto;

import java.util.List;

public record BatchEvaluationResultDto(
        int candidates,
        int evaluated,
        int failed,
        List<String> messages
) {
}