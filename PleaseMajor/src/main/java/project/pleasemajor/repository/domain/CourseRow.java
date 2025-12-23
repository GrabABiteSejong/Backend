package project.pleasemajor.repository.domain;


import java.util.ArrayList;
import java.util.List;

public record CourseRow(
    int year,
    int semester,
    String category,
    String name,
    String prerequisite,
    String note,
    String keywords,
    String url
) {
  public String yearSemesterKey() {
    return year + "-" + semester;
  }

  public List<String> prerequisiteList() {
    return splitList(prerequisite);
  }

  public List<String> keywordList() {
    return splitList(keywords);
  }

  private static List<String> splitList(String raw) {
    if (raw == null) return List.of();
    String s = raw.trim();
    if (s.isEmpty() || s.equals("-") || s.equalsIgnoreCase("없음")) return List.of();

    String[] parts = s.split("[,|/;\\n]");
    List<String> out = new ArrayList<>();
    for (String p : parts) {
      String t = p.trim();
      if (!t.isEmpty()) out.add(t);
    }
    return List.copyOf(out);
  }
}
