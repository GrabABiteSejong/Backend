package project.pleasemajor.controller;


import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import project.pleasemajor.repository.domain.FinalResultResponse;
import project.pleasemajor.service.ExcelTranscriptParserService;
import project.pleasemajor.service.PlanSessionState;
import project.pleasemajor.service.PlanningService;
import project.pleasemajor.service.TrackSessionState.SessionIds;
import project.pleasemajor.service.TrackSessionState.SessionType;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plan")
public class PlanSubmitController {

  private final ObjectMapper objectMapper;
  private final ExcelTranscriptParserService excelParser;
  private final PlanningService planningService;

  @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public FinalResultResponse submit(
      @RequestPart("payload") String payloadJson,
      @RequestPart(value = "transcript", required = false) MultipartFile transcript
  ) throws Exception {

    SubmitPlanRequest req = objectMapper.readValue(payloadJson, SubmitPlanRequest.class);

    // answers만 있으면 PlanSessionState로 감싸서 기존 generateFinal 그대로 재사용
    PlanSessionState s = new PlanSessionState(SessionIds.newId(SessionType.PLAN));
    s.answers.putAll(req.answers());

    Set<String> completed = Set.of();
    if (transcript != null && !transcript.isEmpty()) {
      completed = excelParser.parseCompletedCourses(transcript.getInputStream());
    }

    // 빠른 모드: includeFeedback=false면 GPT 호출 안 하게 PlanningService에서 분기 추천
    return planningService.generateFinal(s, completed);
  }

  public record SubmitPlanRequest(
      Map<String, List<String>> answers,
      boolean includeFeedback
  ) {}
}
