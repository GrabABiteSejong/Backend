package project.pleasemajor.repository.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AnswerRequest(
    @NotBlank String sessionId,
    @NotBlank String questionId,
    @NotNull List<String> answers // single이면 size=1, multi면 여러개

) {

}
