package project.pleasemajor.service;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import project.pleasemajor.repository.domain.DepartmentRef;
import project.pleasemajor.repository.domain.TrackScore;
import project.pleasemajor.util.SystemPrompt;

@Service
@RequiredArgsConstructor
public class CareerChatService {

  private final ChatClient chatClient;
  private final ChatMemoryService memory;
  private final ChatStateService stateService;
  private final TrackCandidateService trackService;
  private final TrackContextBuilder trackContextBuilder;

  private static final double ALPHA = 0.35;
  private static final double RECOMMEND_THRESHOLD = 6.0;
  private static final Duration COOLDOWN = Duration.ofMinutes(10);


  public ChatResult reply(String sessionIdOrNull, String userMessage) {
    String sessionId = (sessionIdOrNull == null || sessionIdOrNull.isBlank())
        ? memory.newSessionId()
        : sessionIdOrNull;

    String history = memory.loadRecent(sessionId);
    SessionState st = stateService.load(sessionId);

    // 0) “계열 추천 자체” 옵트아웃 감지
    if (userOptOutAll(userMessage)) {
      st.optOutAll = true;
    }

    // 1) 이번 메시지 기반 트랙 점수(instant) 계산
    List<TrackScore> ranked = trackService.rankTracksWithScore(userMessage, 5);

    // 2) 누적 점수 업데이트(EMA)
    for (TrackScore ts : ranked) {
      String track = ts.track();
      if (st.suppressedTracks.contains(track)) {
        st.trackScores.put(track, 0.0);
        continue;
      }
      double prev = st.trackScores.getOrDefault(track, 0.0);
      double next = (1 - ALPHA) * prev + ALPHA * ts.score();
      st.trackScores.put(track, next);
    }

    // 3) “특정 계열 빼줘” 같은 억제 처리(원하면 추가)
    suppressIfUserSaysSo(st, userMessage);

    // 4) 추천 트리거 판정
    boolean shouldRecommend = false;
    String context = "";

    if (!st.optOutAll) {
      TrackScore top = pickTopFromState(st, ranked);
      if (top != null
          && !st.suppressedTracks.contains(top.track())
          && st.trackScores.getOrDefault(top.track(), 0.0) >= RECOMMEND_THRESHOLD
          && cooldownPassed(st.lastRecommendedAt)) {

        // 추천 트리거 ON: 상위 1~2개 계열 컨텍스트 붙이기
        shouldRecommend = true;
        List<String> topTracks = top2TracksFromState(st, ranked);
        StringBuilder sb = new StringBuilder();
        for (String t : topTracks) {
          if (st.suppressedTracks.contains(t)) continue;
          sb.append(trackContextBuilder.build(userMessage, t)).append("\n");
        }
        context = sb.toString();
        st.lastRecommendedAt = Instant.now();
      }
    }

    stateService.save(sessionId, st);

    // 5) GPT 프롬프트 구성
    // shouldRecommend=false면 컨텍스트 없이 “상담+질문” 위주로 가게 됨(프롬프트 규칙)
    String prompt = """
[대화기억]
%s

%s

[사용자 메시지]
%s
""".formatted(history, context, userMessage);

    String answer = chatClient.prompt()
        .system(SystemPrompt.TEXT)
        .user(prompt)
        .call()
        .content();

    memory.append(sessionId, "USER", userMessage);
    memory.append(sessionId, "ASSISTANT", answer);

    return new ChatResult(sessionId, answer, shouldRecommend);
  }

  private boolean cooldownPassed(Instant last) {
    return last == null || last.plus(COOLDOWN).isBefore(Instant.now());
  }

  private TrackScore pickTopFromState(SessionState st, List<TrackScore> ranked) {
    // ranked 기반으로도 되고, st.trackScores 전체에서 max를 찾아도 됨
    TrackScore best = null;
    double bestScore = -1;
    for (TrackScore ts : ranked) {
      double s = st.trackScores.getOrDefault(ts.track(), 0.0);
      if (s > bestScore) { bestScore = s; best = new TrackScore(ts.track(), s); }
    }
    return best;
  }

  private List<String> top2TracksFromState(SessionState st, List<TrackScore> ranked) {
    return ranked.stream()
        .map(ts -> new TrackScore(ts.track(), st.trackScores.getOrDefault(ts.track(), 0.0)))
        .sorted((a,b) -> Double.compare(b.score(), a.score()))
        .limit(2)
        .map(TrackScore::track)
        .toList();
  }

  private boolean userOptOutAll(String msg) {
    if (msg == null) return false;
    return msg.contains("계열") && (msg.contains("관심없") || msg.contains("말고") || msg.contains("그만") || msg.contains("하지마"));
  }

  private void suppressIfUserSaysSo(SessionState st, String msg) {
    // 최소 구현: “IT 빼줘/공과 빼줘” 같은 걸 track 라벨 매핑으로 잡아서 suppressedTracks에 넣기
    // 예: if (msg.contains("it") && msg.contains("빼")) { st.suppressedTracks.add("it"); st.trackScores.put("it",0.0); }
  }



  public record ChatResult(String sessionId, String answer, boolean recommendedNow) {}


}
