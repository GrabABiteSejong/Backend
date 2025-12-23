package project.pleasemajor.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlanSessionState {
  public final String sessionId;
  public Map<String, List<String>> answers = new LinkedHashMap<>();
  public int cursor = 0;

  public PlanSessionState(String sessionId) {
    this.sessionId = sessionId;
  }

}
