package project.pleasemajor.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.pleasemajor.repository.CurriculumRepository;
import project.pleasemajor.repository.domain.CourseRow;
import project.pleasemajor.repository.domain.DepartmentRef;

@Component
@RequiredArgsConstructor
public class TrackContextBuilder {
  private final CurriculumRepository repo;
  private final TrackCandidateService trackCandidateService;

  public String build(String userMessage, String track) {
    // 계열 안에서 사용자와 가장 맞는 학과 2개만 “근거”로 사용
    List<DepartmentRef> topDepts = trackCandidateService.topDepartmentsInTrack(userMessage, track, 2);

    StringBuilder sb = new StringBuilder();
    sb.append("[계열 컨텍스트]\n");
    sb.append("- 계열: ").append(track).append("\n");

    for (DepartmentRef d : topDepts) {
      sb.append("  - 대표 학과: ").append(d.dept()).append("\n");
      trackCandidateService.urlOf(d).ifPresent(url ->
          sb.append("    - 참고 URL: ").append(url).append("\n")
      );

      // 키워드 예시(빈 줄만 아니면 몇 개만 보여주기)
      List<CourseRow> rows = repo.findCourses(d).orElse(List.of());
      List<String> keywordSamples = new ArrayList<>();
      for (CourseRow r : rows) {
        if (r.keywords() != null && !r.keywords().isBlank()) {
          keywordSamples.add(r.keywords().trim());
        }
        if (keywordSamples.size() >= 3) break;
      }
      if (!keywordSamples.isEmpty()) {
        sb.append("    - 키워드 예시: ").append(String.join(" | ", keywordSamples)).append("\n");
      }
    }

    sb.append("[/계열 컨텍스트]\n");
    return sb.toString();
  }

}
