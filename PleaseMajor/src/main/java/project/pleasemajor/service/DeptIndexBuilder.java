package project.pleasemajor.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import project.pleasemajor.repository.CurriculumRepository;
import project.pleasemajor.repository.domain.CourseRow;
import project.pleasemajor.repository.domain.DepartmentRef;
import project.pleasemajor.repository.domain.DeptDoc;
import project.pleasemajor.repository.domain.DeptIndex;

/**
 * 학과 색인 구축기
 * */
@Component
public class DeptIndexBuilder {
  private final CurriculumRepository repo;
  private final Tokenizer tokenizer = new Tokenizer();

  private static final double STOPWORD_DF_RATIO = 0.60;
  private static final double IDF_CAP = 4.0;

  public DeptIndexBuilder(CurriculumRepository repo) {
    this.repo = repo;
  }

  public DeptIndex build() {
    List<DepartmentRef> depts = repo.getAllDepartments();
    int N = Math.max(1, depts.size());

    Map<DepartmentRef, DeptDoc> docs = new HashMap<>();
    Map<String, Integer> df = new HashMap<>();

    for (DepartmentRef d : depts) {
      List<CourseRow> rows = repo.findCourses(d).orElse(List.of());

      Set<String> kw = new HashSet<>();
      Set<String> nm = new HashSet<>();
      Set<String> pr = new HashSet<>();
      Set<String> ct = new HashSet<>();

      String urlSample = "";
      for (CourseRow r : rows) {
        kw.addAll(tokenizer.tokenize(r.keywords()));
        nm.addAll(tokenizer.tokenize(r.name()));
        pr.addAll(tokenizer.tokenize(r.prerequisite()));
        ct.addAll(tokenizer.tokenize(r.category()));

        if (urlSample.isBlank() && r.url() != null && !r.url().isBlank()) {
          urlSample = r.url().trim();
        }
      }

      DeptDoc doc = new DeptDoc(d, kw, nm, pr, ct, urlSample);
      docs.put(d, doc);

      // df는 학과 단위 union으로 계산
      Set<String> union = new HashSet<>();
      union.addAll(kw); union.addAll(nm); union.addAll(pr); union.addAll(ct);
      for (String t : union) df.merge(t, 1, Integer::sum);
    }

    Set<String> stopwords = new HashSet<>();
    Map<String, Double> idfMap = new HashMap<>();

    for (var e : df.entrySet()) {
      String token = e.getKey();
      int docFreq = e.getValue();
      double ratio = (double) docFreq / (double) N;

      if (ratio >= STOPWORD_DF_RATIO) stopwords.add(token);

      double idf = Math.log((N + 1.0) / (docFreq + 1.0)) + 1.0;
      if (idf > IDF_CAP) idf = IDF_CAP;
      idfMap.put(token, idf);
    }

    // 운영성 토큰은 수동 stopword (추천: 너무 넓게 잡지 말기)
    stopwords.addAll(Set.of("필수", "선택", "권장", "인증", "공학", "비공학", "수강", "제한", "학년", "학기"));

    return new DeptIndex(idfMap, stopwords, docs, N);
  }

}
