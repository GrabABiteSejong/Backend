package project.pleasemajor.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class QuestionFlowService {
  private final Map<String, PlanSessionState> sessions = new ConcurrentHashMap<>();

  // 24개 고정: Q01~Q23 + Q24_(college)
  private static final List<String> BASE_ORDER = List.of(
      "Q01","Q02","Q03","Q04","Q05",
      "Q06","Q07","Q08","Q09","Q10","Q11","Q12",
      "Q13","Q14","Q15","Q16","Q17","Q18","Q19","Q20","Q21","Q22","Q23"
  );

  public PlanSessionState start() {
    String id = UUID.randomUUID().toString();
    PlanSessionState s = new PlanSessionState(id);
    sessions.put(id, s);
    return s;
  }

  public Optional<PlanSessionState> get(String sessionId) {
    return Optional.ofNullable(sessions.get(sessionId));
  }

  public void saveAnswer(PlanSessionState s, String qid, List<String> ans) {
    s.answers.put(qid, ans);
  }

  public String resolveQ24(PlanSessionState s) {
    String college = first(s.answers.get("Q04"));
    return switch (college) {
      case "COL_AIC" -> "Q24_AIC";
      case "COL_BIZ" -> "Q24_BIZ";
      case "COL_HTM" -> "Q24_HTM";
      case "COL_SOC" -> "Q24_SOC";
      case "COL_HUM" -> "Q24_HUM";
      case "COL_NATSCI" -> "Q24_NATSCI";
      case "COL_ENG" -> "Q24_ENG";
      case "COL_LIFE" -> "Q24_LIFE";
      default -> null; // unknown이면 Q24 없이 종료해도 됨
    };
  }

  public String nextQuestionId(PlanSessionState s) {
    List<String> order = new ArrayList<>(BASE_ORDER);

    String q24 = resolveQ24(s);
    if (q24 != null) order.add(q24);

    // cursor를 다음 "아직 답 안한 질문"으로 이동
    while (s.cursor < order.size() && s.answers.containsKey(order.get(s.cursor))) {
      s.cursor++;
    }
    if (s.cursor >= order.size()) return null;
    return order.get(s.cursor);
  }

  private String first(List<String> v) {
    return (v == null || v.isEmpty()) ? "" : v.get(0);
  }

}
