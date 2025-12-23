package project.pleasemajor.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrackSessionState {
  public Map<String, Double> trackScores = new HashMap<>();
  public Set<String> suppressedTracks = new HashSet<>();
  public boolean optOutAll = false;          // “계열 추천 자체 X”
  public Instant lastRecommendedAt = null;   // 마지막 추천 시각

  public enum SessionType {
    TRACK, PLAN, CHAT
  }

  public final class SessionIds {
    public static String newId(SessionType type) {
      return type.name() + "-" + java.util.UUID.randomUUID();
    }
  }

}
