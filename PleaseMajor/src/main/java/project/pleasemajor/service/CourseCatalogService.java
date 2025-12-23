package project.pleasemajor.service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import project.pleasemajor.repository.domain.CourseRow;

@Service
public class CourseCatalogService {

  private final Map<String, List<CourseRow>> cache = new ConcurrentHashMap<>();

  public List<CourseRow> loadMajorCsv(String collegeFolder, String majorName) {
    String key = collegeFolder + "::" + majorName;
    return cache.computeIfAbsent(key, k -> readCsv(collegeFolder, majorName));
  }

  private List<CourseRow> readCsv(String collegeFolder, String majorName) {
    String path = "data/" + collegeFolder + "/" + majorName + ".csv";

    try (var reader = new InputStreamReader(
        new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {

      CSVParser parser = CSVFormat.DEFAULT
          .builder()
          .setHeader("학년","학기","이수구분","과목명","선수과목","비고","키워드","URL")
          .setSkipHeaderRecord(true)
          .setTrim(true)
          .build()
          .parse(reader);

      List<CourseRow> rows = new ArrayList<>();
      for (CSVRecord r : parser) {
        int year = parseIntSafe(r.get("학년"), 1);
        int semester = parseIntSafe(r.get("학기"), 1);

        String category = getSafe(r, "이수구분");
        String name = getSafe(r, "과목명");
        String prerequisite = getSafe(r, "선수과목");
        String note = getSafe(r, "비고");
        String keywords = getSafe(r, "키워드");
        String url = getSafe(r, "URL");

        rows.add(new CourseRow(year, semester, category, name, prerequisite, note, keywords, url));
      }
      return List.copyOf(rows);

    } catch (Exception e) {
      throw new IllegalStateException("CSV load failed: " + path, e);
    }
  }

  private int parseIntSafe(String raw, int def) {
    if (raw == null) return def;
    String s = raw.trim();
    if (s.isEmpty()) return def;
    try { return Integer.parseInt(s); } catch (Exception e) { return def; }
  }

  private String getSafe(CSVRecord r, String header) {
    try {
      String v = r.get(header);
      return v == null ? "" : v.trim();
    } catch (Exception e) {
      return "";
    }
  }
}
