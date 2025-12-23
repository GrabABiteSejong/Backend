package project.pleasemajor.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import project.pleasemajor.repository.domain.CourseRow;

@Service
public class RoadmapPlannerService {

  private static final Map<String, Set<String>> INTEREST_KEYWORDS = Map.ofEntries(
      Map.entry("AI_DATA", Set.of("데이터", "통계", "EDA", "기계학습", "딥러닝", "시각화", "분석", "머신러닝", "강화학습")),
      Map.entry("SW_DEV", Set.of("웹", "프로그래밍", "HTML", "CSS", "JS", "백엔드", "프런트엔드", "데이터베이스", "SQL", "Git")),
      Map.entry("SECURITY", Set.of("보안", "암호", "네트워크", "시스템", "취약점")),
      Map.entry("HW_SEMI", Set.of("회로", "반도체", "전자", "신호", "임베디드")),
      Map.entry("ROBOT_EMB", Set.of("로봇", "제어", "임베디드", "센서", "신호")),
      Map.entry("BIZ_STRAT", Set.of("전략", "기획", "경영", "경제", "회계", "재무")),
      Map.entry("MKT_BRAND", Set.of("마케팅", "브랜딩", "소비자", "광고")),
      Map.entry("PUBLIC_LAW", Set.of("법", "정책", "행정", "규제")),
      Map.entry("TOUR_SERVICE", Set.of("호텔", "관광", "서비스", "외식", "조리")),
      Map.entry("BIO_LIFE", Set.of("생명", "유전", "미생물", "바이오")),
      Map.entry("FOOD_NUT", Set.of("식품", "영양", "공정", "품질")),
      Map.entry("BASIC_SCI", Set.of("물리", "화학", "실험", "분석")),
      Map.entry("MATH_STAT", Set.of("선형대수", "확률", "통계", "최적화", "이산수학", "수치해석"))
  );

  public Map<String, List<ScoredCourse>> buildRoadmap(
      List<CourseRow> catalog,
      Set<String> interestTags,
      Set<String> completedCourses,
      int currentYear, int currentSemester,
      int maxPerSemester
  ) {
    Map<String, List<CourseRow>> bySem = catalog.stream()
        .collect(Collectors.groupingBy(CourseRow::yearSemesterKey));

    List<String> order = List.of("1-1","1-2","2-1","2-2","3-1","3-2","4-1","4-2");
    String startKey = nextSemesterKey(currentYear, currentSemester);

    Map<String, List<ScoredCourse>> result = new LinkedHashMap<>();
    Set<String> planned = new HashSet<>(completedCourses == null ? Set.of() : completedCourses);

    boolean started = false;

    for (String semKey : order) {
      if (!started) {
        if (semKey.equals(startKey)) started = true;
        else continue;
      }

      List<CourseRow> candidates = bySem.getOrDefault(semKey, List.of());

      // 이미 이수한 과목 제외
      candidates = candidates.stream()
          .filter(c -> !planned.contains(c.name()))
          .toList();

      List<ScoredCourse> scored = candidates.stream()
          .map(c -> new ScoredCourse(c, score(c, interestTags)))
          .sorted(Comparator.comparingDouble(ScoredCourse::score).reversed())
          .toList();

      List<ScoredCourse> picked = new ArrayList<>();
      for (ScoredCourse sc : scored) {
        if (picked.size() >= maxPerSemester) break;
        if (prereqSatisfied(sc.course(), planned)) picked.add(sc);
      }

      for (ScoredCourse sc : picked) planned.add(sc.course().name());
      result.put(semKey, picked);
    }

    return result;
  }

  private String nextSemesterKey(int year, int sem) {
    return (sem == 1) ? (year + "-2") : ((year + 1) + "-1");
  }

  private boolean prereqSatisfied(CourseRow c, Set<String> plannedOrDone) {
    List<String> prereqs = c.prerequisiteList();
    if (prereqs.isEmpty()) return true;

    for (String p : prereqs) {
      if (!plannedOrDone.contains(p)) return false;
    }
    return true;
  }

  private double score(CourseRow c, Set<String> interestTags) {
    double base = 0.0;

    String cat = c.category() == null ? "" : c.category();
    if (cat.contains("전필")) base += 20;
    else if (cat.contains("전선")) base += 10;

    // 관심 키워드 매칭
    Set<String> target = new HashSet<>();
    for (String it : interestTags) target.addAll(INTEREST_KEYWORDS.getOrDefault(it, Set.of()));

    int match = 0;
    // keywordList()는 split된 토큰(키워드) 기반
    for (String kw : c.keywordList()) {
      for (String t : target) {
        if (kw.contains(t) || t.contains(kw)) { match++; break; }
      }
    }

    // 키워드가 빈 경우를 위해 raw string에도 약하게 보정
    if (match == 0 && c.keywords() != null) {
      String raw = c.keywords();
      for (String t : target) {
        if (!t.isBlank() && raw.contains(t)) { match++; break; }
      }
    }

    base += match * 6.0;

    // 진로/콜로키움류 가산
    String n = c.name();
    if (n.contains("진로") || n.contains("취창업") || n.contains("콜로키움")) base += 5;

    return base;
  }

  public record ScoredCourse(CourseRow course, double score) {}
}