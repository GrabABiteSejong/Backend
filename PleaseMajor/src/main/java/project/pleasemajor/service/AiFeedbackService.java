package project.pleasemajor.service;

import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import project.pleasemajor.service.RoadmapPlannerService.ScoredCourse;

@Service
public class AiFeedbackService {

  private final ChatClient chatClient;

  public AiFeedbackService(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  public String generateFeedback(
      String college,
      String major,
      Map<String, List<String>> answers,
      RadarScoringService.Radar radar,
      Map<String, List<RoadmapPlannerService.ScoredCourse>> roadmap
  ) {
    String prompt = buildPrompt(college, major, answers, radar, roadmap);

    return chatClient.prompt()
        .user(prompt)
        .call()
        .content();
  }

  private String buildPrompt(
      String college,
      String major,
      Map<String, List<String>> answers,
      RadarScoringService.Radar radar,
      Map<String, List<RoadmapPlannerService.ScoredCourse>> roadmap
  ) {
    return """
      너는 세종대 진로/교과 추천 코치다.
      규칙:
      - 단정/비하 금지. 실천 가능한 조언.
      - 점수는 이미 계산됨. 새 점수 생성 금지.
      - 7~10문장, (강점2) (보완2) (다음학기 행동3) 구성.

      입력:
      - 단과대 코드: %s
      - 학과: %s
      - 관심분야(Q06): %s
      - 목표(Q07): %s
      - 레이더: %s / %s
      - 로드맵(요약): %s
      """
        .formatted(
            college,
            major,
            answers.getOrDefault("Q06", List.of()),
            answers.getOrDefault("Q07", List.of()),
            radar.axes(),
            radar.scores(),
            summarizeRoadmap(roadmap)
        );
  }

  private String summarizeRoadmap(Map<String, List<ScoredCourse>> roadmap) {
    StringBuilder sb = new StringBuilder();
    roadmap.forEach((sem, list) -> {
      sb.append(sem).append(": ");
      for (int i = 0; i < Math.min(2, list.size()); i++) {
        sb.append(list.get(i).course().name());
        if (i < Math.min(2, list.size()) - 1) sb.append(", ");
      }
      sb.append(" | ");
    });
    return sb.toString();
  }
}