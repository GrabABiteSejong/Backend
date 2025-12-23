package project.pleasemajor.repository;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import project.pleasemajor.repository.domain.CourseRow;
import project.pleasemajor.repository.domain.DepartmentRef;

@Component
public class CsvCurriculumRepository implements CurriculumRepository {
  private final Map<DepartmentRef, List<CourseRow>> store = new ConcurrentHashMap<>();

  @PostConstruct
  public void loadAll() throws IOException {
    var resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:curriculum/*/*.csv");

    for (Resource r : resources) {
      String filename = r.getFilename();
      if (filename == null) continue;

      String track = extractTrack(r);
      String dept = stripExt(filename);

      List<CourseRow> rows = parseOne(r);
      store.put(new DepartmentRef(track, dept), rows);
    }
  }

  @Override
  public List<DepartmentRef> getAllDepartments() {
    return new ArrayList<>(store.keySet());
  }

  @Override
  public Optional<List<CourseRow>> findCourses(DepartmentRef dept) {
    return Optional.ofNullable(store.get(dept));
  }

  private List<CourseRow> parseOne(Resource resource) throws IOException {
    DelimAndReader dr = openWithDelimiter(resource);

    CSVFormat format = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setDelimiter(dr.delimiter)
        .setTrim(true)
        .build();

    try (Reader reader = dr.reader; CSVParser parser = format.parse(reader)) {
      List<CourseRow> list = new ArrayList<>();
      for (CSVRecord rec : parser) {
        int year = parseIntSafe(getByAny(rec, "학년"));
        int sem  = parseIntSafe(getByAny(rec, "학기"));
        String cat = getByAny(rec, "이수구분");
        String name = getByAny(rec, "과목명");
        String pre  = getByAny(rec, "선수과목");
        String note = getByAny(rec, "비고");
        String keywords = getByAny(rec, "키워드", "keywords", "keyword");
        String url = getByAny(rec, "URL", "url", "Url");

        list.add(new CourseRow(year, sem, cat, name, pre, note, keywords, url));
      }
      return list;
    }
  }

  private record DelimAndReader(char delimiter, Reader reader) {}

  private DelimAndReader openWithDelimiter(Resource resource) throws IOException {
    String firstLine;
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      firstLine = br.readLine();
    }
    char delimiter = (firstLine != null && firstLine.contains("\t")) ? '\t' : ',';
    Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
    return new DelimAndReader(delimiter, reader);
  }

  private String getByAny(CSVRecord rec, String... headers) {
    for (String h : headers) {
      try {
        String v = rec.get(h);
        if (v != null) return v.trim();
      } catch (Exception ignored) {}
    }
    return "";
  }

  private int parseIntSafe(String s) {
    try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
  }

  private String extractTrack(Resource r) {
    try {
      String url = r.getURL().toString(); // .../curriculum/engineering/xxx.csv
      int idx = url.indexOf("/curriculum/");
      if (idx < 0) return "unknown";
      String rest = url.substring(idx + "/curriculum/".length());
      int slash = rest.indexOf('/');
      return slash > 0 ? rest.substring(0, slash) : "unknown";
    } catch (IOException e) {
      return "unknown";
    }
  }

  private String stripExt(String filename) {
    int dot = filename.lastIndexOf('.');
    return dot > 0 ? filename.substring(0, dot) : filename;
  }

}
