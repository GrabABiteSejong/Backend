package project.pleasemajor.repository.domain;

import java.util.List;
import java.util.Map;

public record FinalResultResponse(
    RoadmapResponse roadmap,
    RadarResponse radar,
    String feedback
) {
  public record RoadmapResponse(Map<String, List<CourseCard>> semesters) {}
  public record CourseCard(
      String yearSemester, // "2-1"
      String completionType, // 전필/전선/교양/BSM...
      String courseName,
      List<String> keywords,
      String url,
      String note,
      List<String> prerequisites,
      double score
  ) {}

  public record RadarResponse(List<String> axes, List<Double> scores) {}
}
