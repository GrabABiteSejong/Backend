package project.pleasemajor.service;


import java.util.*;
import java.util.regex.Pattern;

public class Tokenizer {
  // ·, /, :, 괄호 등 대부분 구분자로 처리
  private static final Pattern NON_TEXT = Pattern.compile("[^0-9a-zA-Z가-힣\\s]");
  private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
  private static final Pattern DIGITS = Pattern.compile("\\d+");

  public Set<String> tokenize(String text) {
    if (text == null || text.isBlank()) return Set.of();

    String cleaned = NON_TEXT.matcher(text).replaceAll(" ");
    cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim().toLowerCase(Locale.ROOT);

    List<String> words = new ArrayList<>();
    for (String w : cleaned.split(" ")) {
      if (w.isBlank()) continue;
      if (w.chars().allMatch(Character::isDigit)) continue;

      String noDigits = DIGITS.matcher(w).replaceAll("");
      if (noDigits.length() >= 2) words.add(noDigits);
      else if (w.length() >= 2) words.add(w);
    }

    Set<String> out = new HashSet<>(words);

    // "복잡도 분석" 같은 2단어 조합도 매칭되게 바이그램 추가
    for (int i = 0; i + 1 < words.size(); i++) {
      String a = words.get(i), b = words.get(i + 1);
      if (a.length() >= 2 && b.length() >= 2) {
        out.add(a + b);
        out.add(a + "_" + b);
      }
    }

    // 한글 붙어쓰기 대응(과목명: 미적분학1 등) 2~4 shingle
    for (String w : words) {
      if (containsHangul(w) && w.length() >= 4) {
        shingle(out, w, 2, 4, 10);
      }
    }

    // 1글자 제거
    out.removeIf(t -> t.length() < 2);
    return out;
  }

  private void shingle(Set<String> out, String token, int minN, int maxN, int maxLen) {
    String s = token.length() > maxLen ? token.substring(0, maxLen) : token;
    for (int n = minN; n <= maxN; n++) {
      for (int i = 0; i + n <= s.length(); i++) out.add(s.substring(i, i + n));
    }
  }

  private boolean containsHangul(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c >= '가' && c <= '힣') return true;
    }
    return false;
  }
}

