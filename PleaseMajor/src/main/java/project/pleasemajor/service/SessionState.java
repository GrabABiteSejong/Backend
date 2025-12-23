package project.pleasemajor.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SessionState {
  public Map<String, Double> trackScores = new HashMap<>();
  public Set<String> suppressedTracks = new HashSet<>();
  public boolean optOutAll = false;          // “계열 추천 자체 X”
  public Instant lastRecommendedAt = null;   // 마지막 추천 시각

}
