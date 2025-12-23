package project.pleasemajor.service;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import project.pleasemajor.repository.domain.TrackScore;
import project.pleasemajor.util.SystemPrompt;
import reactor.core.publisher.Flux;


/**
 * 사용자의 마음(시간에 따른 관심도 변화)
 * */
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
    Prepared p = prepare(sessionIdOrNull, userMessage);

    String answer = chatClient.prompt()
        .system(SystemPrompt.TEXT)
        .user(p.prompt)
        .call()
        .content();

    memory.append(p.sessionId, "USER", userMessage);
    memory.append(p.sessionId, "ASSISTANT", answer);

    return new ChatResult(p.sessionId, answer, p.shouldRecommend);
  }

  public Flux<String> replyStream(String sessionIdOrNull, String userMessage) {
    Prepared p = prepare(sessionIdOrNull, userMessage);

    // 스트리밍은 chunk가 여러번 오니까, 최종 저장은 마지막에 해야 함.
    // 방법 1) controller에서 Flux를 collect 해서 마지막에 저장
    // 방법 2) 여기서 StringBuilder로 누적 후 doOnComplete에 저장
    // -> 아래는 "여기서 누적"하는 방식

    StringBuilder sb = new StringBuilder();

    return chatClient.prompt()
        .system(SystemPrompt.TEXT)
        .user(p.prompt)
        .stream()
        .content()
        .doOnNext(sb::append)
        .doOnComplete(() -> {
          String answer = sb.toString();
          memory.append(p.sessionId, "USER", userMessage);
          memory.append(p.sessionId, "ASSISTANT", answer);
        });
  }

  private Prepared prepare(String sessionIdOrNull, String userMessage) {
    String sessionId = (sessionIdOrNull == null || sessionIdOrNull.isBlank())
        ? memory.newSessionId()
        : sessionIdOrNull;

    String history = memory.loadRecent(sessionId);
    TrackSessionState st = stateService.load(sessionId);

    if (userOptOutAll(userMessage)) st.optOutAll = true;

    List<TrackScore> ranked = trackService.rankTracksWithScore(userMessage, 5);

    // 누적 업데이트(EMA)
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


    boolean shouldRecommend = false;
    String context = "";

    if (!st.optOutAll) {
      TrackScore top = pickTopFromState(st, ranked);
      if (top != null
          && !st.suppressedTracks.contains(top.track())
          && st.trackScores.getOrDefault(top.track(), 0.0) >= RECOMMEND_THRESHOLD
          && cooldownPassed(st.lastRecommendedAt)) {

        shouldRecommend = true;
        List<String> topTracks = top2TracksFromState(st, ranked);

        StringBuilder csb = new StringBuilder();
        for (String t : topTracks) {
          if (st.suppressedTracks.contains(t)) continue;
          csb.append(trackContextBuilder.build(userMessage, t)).append("\n");
        }
        context = csb.toString();
        st.lastRecommendedAt = Instant.now();
      }
    }

    stateService.save(sessionId, st);

    String prompt = """
[대화기억]
%s

%s
[사용자 메시지]
%s
""".formatted(history, context, userMessage);

    return new Prepared(sessionId, prompt, shouldRecommend);
  }

  private record Prepared(String sessionId, String prompt, boolean shouldRecommend) {}

  private boolean cooldownPassed(Instant last) {
    return last == null || last.plus(COOLDOWN).isBefore(Instant.now());
  }

  private TrackScore pickTopFromState(TrackSessionState st, List<TrackScore> ranked) {
    TrackScore best = null;
    double bestScore = -1;
    for (TrackScore ts : ranked) {
      double s = st.trackScores.getOrDefault(ts.track(), 0.0);
      if (s > bestScore) { bestScore = s; best = new TrackScore(ts.track(), s); }
    }
    return best;
  }

  private List<String> top2TracksFromState(TrackSessionState st, List<TrackScore> ranked) {
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


  public record ChatResult(String sessionId, String answer, boolean recommendedNow) {}
}

