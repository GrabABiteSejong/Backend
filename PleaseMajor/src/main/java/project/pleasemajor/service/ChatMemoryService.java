package project.pleasemajor.service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

/**
 *
 * 채팅 세션별 대화 기록을 Redis 리스트에 저장/로드
 * */
@Service
public class ChatMemoryService {
  private final StringRedisTemplate redis;
  private static final Duration TTL = Duration.ofHours(1);

  public ChatMemoryService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public String newSessionId() {
    return UUID.randomUUID().toString();
  }

  public void append(String sessionId, String role, String content) {
    String key = key(sessionId);
    redis.opsForList().rightPush(key, role + ": " + content);
    redis.expire(key, TTL);
    redis.opsForList().trim(key, -40, -1);
  }

  public String loadRecent(String sessionId) {
    List<String> lines = redis.opsForList().range(key(sessionId), -20, -1);
    if (lines == null || lines.isEmpty()) return "";
    return String.join("\n", lines);
  }

  private String key(String sessionId) {
    return "chatbot:session:" + sessionId;
  }

}
