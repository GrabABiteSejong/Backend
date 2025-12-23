package project.pleasemajor.repository.domain;

import java.util.Map;

public record NextStepResponse(
    String sessionId,
    boolean done,
    Map<String, Object> payload // done=false -> question, done=true -> result
) {

}
