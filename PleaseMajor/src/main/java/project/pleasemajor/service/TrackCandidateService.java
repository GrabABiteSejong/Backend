package project.pleasemajor.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import project.pleasemajor.repository.domain.DepartmentRef;
import project.pleasemajor.repository.domain.DeptDoc;
import project.pleasemajor.repository.domain.DeptIndex;
import project.pleasemajor.repository.domain.TrackScore;

/**
 * 계열 후보군 선정기
 * */
@Service
public class TrackCandidateService {
  private final DeptIndex index;
  private final Tokenizer tokenizer = new Tokenizer();

  // 가중치(키워드 최우선)
  private static final double W_KEYWORD = 5.0;
  // 과목명
  private static final double W_NAME = 3.0;
  // 선수과목
  private static final double W_PREREQ = 2.0;
  // 이수구분
  private static final double W_CATEGORY = 1.0;
  // 계열 내 “상위 학과 몇 개”로 평균낼지
  private static final int TOP_DEPTS_PER_TRACK = 5;

  public TrackCandidateService(DeptIndexBuilder builder) {
    this.index = builder.build();
  }

  public List<TrackScore> rankTracksWithScore(String userMessage, int k) {
    Set<String> tokens = tokenizer.tokenize(userMessage);
    tokens.removeAll(index.stopwords());

    // 1) 학과별 점수
    Map<DepartmentRef, Double> deptScore = new HashMap<>();
    for (var e : index.docs().entrySet()) {
      DepartmentRef dept = e.getKey();
      DeptDoc doc = e.getValue();

      double s = 0.0;
      for (String t : tokens) {
        double idf = index.idfMap().getOrDefault(t, 1.0);

        if (doc.keywordTokens().contains(t)) s += W_KEYWORD * idf;
        if (doc.courseNameTokens().contains(t)) s += W_NAME * idf;
        if (doc.prereqTokens().contains(t)) s += W_PREREQ * idf;
        if (doc.categoryTokens().contains(t)) s += W_CATEGORY * idf;
      }
      deptScore.put(dept, s);
    }

    // 2) track별 topM 평균
    Map<String, List<Double>> byTrack = new HashMap<>();
    for (var e : deptScore.entrySet()) {
      byTrack.computeIfAbsent(e.getKey().track(), x -> new ArrayList<>()).add(e.getValue());
    }

    Map<String, Double> trackScore = new HashMap<>();
    for (var e : byTrack.entrySet()) {
      List<Double> scores = e.getValue();
      scores.sort(Comparator.reverseOrder());
      int m = Math.min(TOP_DEPTS_PER_TRACK, scores.size());
      double avg = 0.0;
      for (int i = 0; i < m; i++) avg += scores.get(i);
      avg = (m == 0) ? 0.0 : avg / m;
      trackScore.put(e.getKey(), avg);
    }

    // 3) 점수 포함 정렬 반환
    return trackScore.entrySet().stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .limit(k)
        .map(e -> new TrackScore(e.getKey(), e.getValue()))
        .toList();
  }


  public List<String> pickTopTracks(String userMessage, int k) {
    return rankTracksWithScore(userMessage, k).stream()
        .map(TrackScore::track)
        .toList();
  }

  public int usefulTokenCount(String userMessage) {
    Set<String> t = tokenizer.tokenize(userMessage);
    t.removeAll(index.stopwords());
    return t.size();
  }

  // 컨텍스트 만들 때 “계열 내에서 유저와 가장 잘 맞는 학과들”도 뽑고 싶으면 사용
  public List<DepartmentRef> topDepartmentsInTrack(String userMessage, String track, int limit) {
    Set<String> tokens = tokenizer.tokenize(userMessage);
    tokens.removeAll(index.stopwords());

    Map<DepartmentRef, Double> score = new HashMap<>();
    for (var e : index.docs().entrySet()) {
      DepartmentRef dept = e.getKey();
      if (!dept.track().equalsIgnoreCase(track)) continue;

      DeptDoc doc = e.getValue();
      double s = 0.0;
      for (String t : tokens) {
        double idf = index.idfMap().getOrDefault(t, 1.0);
        if (doc.keywordTokens().contains(t)) s += W_KEYWORD * idf;
        if (doc.courseNameTokens().contains(t)) s += W_NAME * idf;
      }
      score.put(dept, s);
    }

    return score.entrySet().stream()
        .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
        .limit(limit)
        .map(Map.Entry::getKey)
        .toList();
  }

  public Optional<String> urlOf(DepartmentRef dept) {
    DeptDoc doc = index.docs().get(dept);
    if (doc == null) return Optional.empty();
    return Optional.ofNullable(doc.urlSample()).filter(s -> !s.isBlank());
  }
}
