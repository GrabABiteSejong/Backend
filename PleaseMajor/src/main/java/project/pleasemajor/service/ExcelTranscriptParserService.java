package project.pleasemajor.service;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelTranscriptParserService {

  public Set<String> parseCompletedCourses(InputStream in) {
    try (Workbook wb = new XSSFWorkbook(in)) {
      Sheet sheet = wb.getSheetAt(0);
      if (sheet == null) return Set.of();

      int headerRowIdx = findHeaderRow(sheet);
      if (headerRowIdx < 0) return Set.of();

      Row header = sheet.getRow(headerRowIdx);
      int nameCol = findColumnIndex(header, "교과목명", "과목명");
      if (nameCol < 0) return Set.of();

      Set<String> completed = new HashSet<>();
      for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) continue;

        String courseName = cellString(row.getCell(nameCol));
        courseName = normalizeCourseName(courseName);
        if (!courseName.isBlank()) completed.add(courseName);
      }
      return completed;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse transcript excel", e);
    }
  }

  private int findHeaderRow(Sheet sheet) {
    // 보통 상단 0~10줄 안에 헤더가 있음
    int max = Math.min(10, sheet.getLastRowNum());
    for (int r = 0; r <= max; r++) {
      Row row = sheet.getRow(r);
      if (row == null) continue;
      for (Cell c : row) {
        String v = cellString(c);
        if (v.contains("교과목명") || v.equals("과목명")) return r;
      }
    }
    return -1;
  }

  private int findColumnIndex(Row header, String... candidates) {
    for (Cell c : header) {
      String v = cellString(c).trim();
      for (String cand : candidates) {
        if (v.equals(cand) || v.contains(cand)) return c.getColumnIndex();
      }
    }
    return -1;
  }

  private String cellString(Cell c) {
    if (c == null) return "";
    return switch (c.getCellType()) {
      case STRING -> c.getStringCellValue();
      case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
      case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
      case FORMULA -> c.getCellFormula(); // 필요하면 evaluate로 바꿔도 됨
      default -> "";
    };
  }

  private String normalizeCourseName(String s) {
    if (s == null) return "";
    // "자료구조 및 실습" vs "자료구조및실습" 같은 매칭을 위해 공백 제거
    return s.trim().replaceAll("\\s+", "");
  }}

