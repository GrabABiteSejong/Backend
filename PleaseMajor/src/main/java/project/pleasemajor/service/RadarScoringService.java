package project.pleasemajor.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RadarScoringService {

  public Radar radarFromAnswers(Map<String, List<String>> a) {
    double quant = clamp(0.7 * tri1(a, "Q13", "MATH_STRONG","MATH_AVG","MATH_WEAK")
        + 0.3 * tri1(a, "Q15", "STAT_HIGH","STAT_MID","STAT_LOW"));

    double impl = clamp(0.8 * tri1(a, "Q14", "CODE_STRONG","CODE_AVG","CODE_WEAK")
        + 0.2 * taskBonus(a));

    double data = clamp(0.5 * tri1(a, "Q15", "STAT_HIGH","STAT_MID","STAT_LOW")
        + 0.5 * tri1(a, "Q16", "AI_HIGH","AI_MID","AI_LOW")
        + interestBonus(a, "AI_DATA", 0.5));

    double domain = clamp(domainDepth(a)); // 전공코어(트랙별)
    double exec = clamp(execution(a));     // 커뮤니케이션·실행

    return new Radar(
        List.of("정량 기초", "구현·엔지니어링", "데이터 해석·모델링", "전공 도메인 깊이", "커뮤니케이션·실행"),
        List.of(r2(quant), r2(impl), r2(data), r2(domain), r2(exec))
    );
  }

  private double domainDepth(Map<String, List<String>> a) {
    // 트랙별 Q24_*가 있으면 그걸 사용해서 도메인 축을 세팅
    // 예: IT 트랙이면 "시스템/보안/로봇/반도체/콘텐츠" 중 선택
    // 단순 버전: 선택했으면 3.6~4.6, 모르겠음이면 3.0
    String track = first(a.get("Q04")); // 폴더명(예: it)
    String q24 = "Q24"; // 네가 트랙별로 Q24_it, Q24_경영...로 나눠도 됨

    String pick = first(a.get(q24));
    if (pick == null || pick.isBlank()) return 3.0;

    // 도메인 선택이 명확하면 가산
    return 4.2;
  }

  private double execution(Map<String, List<String>> a) {
    double time = switch (first(a.get("Q11"))) {
      case "T_VERY_HIGH" -> 4.7;
      case "T_HIGH" -> 4.1;
      case "T_MID" -> 3.6;
      default -> 3.0;
    };

    double collab = switch (first(a.get("Q10"))) {
      case "TEAM" -> 4.2;
      case "SOLO" -> 3.4;
      default -> 3.8;
    };

    boolean speak = a.getOrDefault("Q17", List.of()).contains("SK_SPEAK");
    double speakBonus = speak ? 0.4 : 0.0;

    return clamp(0.5 * time + 0.5 * collab + speakBonus);
  }

  private double taskBonus(Map<String, List<String>> a) {
    var task = Set.copyOf(a.getOrDefault("Q09", List.of()));
    double b = 3.0;
    if (task.contains("TASK_PROJECT")) b += 0.6; // 구현 쪽
    if (task.contains("TASK_CLOSED")) b += 0.4;  // 구현(문제풀이) 쪽
    return clamp(b);
  }

  private double interestBonus(Map<String, List<String>> a, String tag, double bonus) {
    return a.getOrDefault("Q06", List.of()).contains(tag) ? bonus : 0.0;
  }

  private double tri1(Map<String, List<String>> a, String qid, String strong, String mid, String weak) {
    String x = first(a.get(qid));
    if (strong.equals(x)) return 5.0;
    if (mid.equals(x)) return 3.5;
    return 2.0;
  }

  private String first(List<String> v) {
    return (v == null || v.isEmpty()) ? "" : v.get(0);
  }

  private double clamp(double v) { return Math.max(1.0, Math.min(5.0, v)); }
  private double r2(double v) { return Math.round(v * 100.0) / 100.0; }

  public record Radar(List<String> axes, List<Double> scores) {}
}

