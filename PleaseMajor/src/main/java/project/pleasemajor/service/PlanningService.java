package project.pleasemajor.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.pleasemajor.repository.domain.CourseRow;
import project.pleasemajor.repository.domain.FinalResultResponse;
import project.pleasemajor.repository.domain.FinalResultResponse.CourseCard;

@Service
@RequiredArgsConstructor
public class PlanningService {
  private final CourseCatalogService catalogService;
  private final RoadmapPlannerService roadmapPlanner;
  private final RadarScoringService radarScoring;
  private final AiFeedbackService feedbackService;

  public FinalResultResponse generateFinal(PlanSessionState s, Set<String> completedCourses) {

    String college = first(s.answers.get("Q04")); // ex) COL_AIC
    String major   = first(s.answers.get("Q05")); // ex) 컴퓨터공학과
    Set<String> interestTags = new HashSet<>(s.answers.getOrDefault("Q06", List.of()));

    int curYear = parseYear(first(s.answers.get("Q02")));      // "Y2" -> 2
    int curSem  = parseSemester(first(s.answers.get("Q03")));  // "S1" -> 1

    List<CourseRow> catalog = catalogService.loadMajorCsv(college, major);

    var roadmap = roadmapPlanner.buildRoadmap(
        catalog,
        interestTags,
        completedCourses == null ? Set.of() : completedCourses,
        curYear, curSem,
        4
    );

    // 프론트용 변환
    Map<String, List<FinalResultResponse.CourseCard>> semesters = new LinkedHashMap<>();
    for (var e : roadmap.entrySet()) {
      String semKey = e.getKey();

      List<FinalResultResponse.CourseCard> cards = e.getValue().stream()
          .map(sc -> {
            CourseRow c = sc.course();
            return new FinalResultResponse.CourseCard(
                semKey,
                c.category(),          // ✅ completionType 대체
                c.name(),              // ✅ courseName 대체
                c.keywordList(),       // ✅ List<String>
                c.url(),
                c.note(),
                c.prerequisiteList(),  // ✅ List<String>
                sc.score()
            );
          })
          .collect(Collectors.toList());

      semesters.put(semKey, cards);
    }

    var radar = radarScoring.radarFromAnswers(s.answers);

    // ✅ AiFeedbackService 시그니처 맞춰서 호출 (아래 3번 참고)
    String feedback = feedbackService.generateFeedback(college, major, s.answers, radar, roadmap);

    return new FinalResultResponse(
        new FinalResultResponse.RoadmapResponse(semesters),
        new FinalResultResponse.RadarResponse(radar.axes(), radar.scores()),
        feedback
    );
  }

  private String first(List<String> v) {
    return (v == null || v.isEmpty()) ? "" : v.get(0);
  }

  private int parseYear(String y) {
    if (y == null || y.isBlank()) return 1;
    // "Y2" 형태가 아니라 "2"가 올 수도 있으니 방어
    return Integer.parseInt(y.replace("Y", "").trim());
  }

  private int parseSemester(String s) {
    if (s == null || s.isBlank()) return 1;
    return Integer.parseInt(s.replace("S", "").trim());
  }

}
