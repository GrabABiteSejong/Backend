package project.pleasemajor.service;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import project.pleasemajor.repository.CurriculumRepository;
import project.pleasemajor.repository.domain.CourseRow;
import project.pleasemajor.repository.domain.DepartmentRef;

@Component
public class CurriculumContextBuilder {
  private final CurriculumRepository repo;
  private final Tokenizer tokenizer = new Tokenizer();

  public CurriculumContextBuilder(CurriculumRepository repo) {
    this.repo = repo;
  }

  public String build(String userMessage, List<DepartmentRef> depts) {
    Set<String> msgTokens = tokenizer.tokenize(userMessage);

    StringBuilder sb = new StringBuilder();
    sb.append("[커리큘럼 컨텍스트]\n");
    sb.append("- 규칙: 아래에 나온 학과/과목만 추천 가능\n\n");

    for (DepartmentRef d : depts) {
      List<CourseRow> rows = repo.findCourses(d).orElse(List.of());

      sb.append("학과: ").append(d.dept()).append(" (계열: ").append(d.track()).append(")\n");

      // 과목(행) 점수화: note/name/prereq 매칭
      List<ScoredRow> scored = new ArrayList<>();
      for (CourseRow r : rows) {
        double s = 0;
        String name = safe(r.name());
        String note = safe(r.note());
        String pre = safe(r.prerequisite());

        for (String t : msgTokens) {
          if (t.length() < 2) continue;
          if (name.contains(t)) s += 3;
          if (note.contains(t)) s += 5;
          if (pre.contains(t)) s += 2;
        }

        // 전공계열 우선 가산(선택)
        if (safe(r.category()).contains("전공")) s += 0.5;

        scored.add(new ScoredRow(r, s));
      }

      scored.sort((a, b) -> Double.compare(b.score, a.score));

      // 컨텍스트는 과목 너무 많이 넣지 말기 (학과당 Top 20)
      int limit = 20;
      for (int i = 0; i < Math.min(limit, scored.size()); i++) {
        CourseRow r = scored.get(i).row;
        sb.append("- (").append(r.year()).append("-").append(r.semester()).append(") ")
            .append(r.category()).append(": ").append(r.name());
        if (!safe(r.prerequisite()).isBlank()) sb.append(" | 선수: ").append(r.prerequisite());
        if (!safe(r.note()).isBlank()) sb.append(" | 비고: ").append(r.note());
        sb.append("\n");
      }
      sb.append("\n");
    }

    sb.append("[/커리큘럼 컨텍스트]\n");
    return sb.toString();
  }

  private String safe(String s) { return s == null ? "" : s; }

  private static class ScoredRow {
    final CourseRow row;
    final double score;
    ScoredRow(CourseRow row, double score) { this.row = row; this.score = score; }
  }

}
