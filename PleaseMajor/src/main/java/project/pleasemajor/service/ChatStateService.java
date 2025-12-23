package project.pleasemajor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * 채팅 세션별 상태(SessionState)를 Redis에 저장/로드
 * “점수/옵트아웃 같은 상태” 저장.
 * */
@Service
public class ChatStateService {
  private final StringRedisTemplate redis;
  private final ObjectMapper om = new ObjectMapper();

  private static final Duration TTL = Duration.ofHours(1);

  public ChatStateService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public SessionState load(String sessionId) {
    String json = redis.opsForValue().get(key(sessionId));
    if (json == null || json.isBlank()) return new SessionState();
    try {
      return om.readValue(json, SessionState.class);
    } catch (Exception e) {
      return new SessionState();
    }
  }

  public void save(String sessionId, SessionState state) {
    try {
      String json = om.writeValueAsString(state);
      redis.opsForValue().set(key(sessionId), json, TTL);
    } catch (Exception ignored) {}
  }

  private String key(String sessionId) {
    return "chatbot:state:" + sessionId;
  }

}
