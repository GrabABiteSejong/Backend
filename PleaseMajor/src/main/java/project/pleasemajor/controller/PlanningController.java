package project.pleasemajor.controller;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.pleasemajor.repository.domain.AnswerRequest;
import project.pleasemajor.repository.domain.FinalResultResponse;
import project.pleasemajor.repository.domain.NextStepResponse;
import project.pleasemajor.service.PlanSessionState;
import project.pleasemajor.service.PlanningService;
import project.pleasemajor.service.QuestionFlowService;

@RestController
@RequestMapping("/api/planning")
public class PlanningController {
  private final QuestionFlowService flowService;
  private final PlanningService planningService;

  public PlanningController(QuestionFlowService flowService, PlanningService planningService) {
    this.flowService = flowService;
    this.planningService = planningService;
  }

  @PostMapping("/session/start")
  public NextStepResponse start() {
    PlanSessionState s = flowService.start();
    String nextQid = flowService.nextQuestionId(s);
    return new NextStepResponse(s.sessionId, false, Map.of("nextQuestionId", nextQid));
  }

  @PostMapping("/session/answer")
  public NextStepResponse answer(@RequestBody @Valid AnswerRequest req) {
    PlanSessionState s = flowService.get(req.sessionId()).orElseThrow();
    flowService.saveAnswer(s, req.questionId(), req.answers());

    String nextQid = flowService.nextQuestionId(s);
    if (nextQid != null) {
      return new NextStepResponse(s.sessionId, false, Map.of("nextQuestionId", nextQid));
    }

    // 엑셀 이수 과목 파싱을 붙이면 completedCourses에 넣으면 됨
    FinalResultResponse result = planningService.generateFinal(s, Set.of());
    return new NextStepResponse(s.sessionId, true, Map.of("result", result));
  }

}
