package project.pleasemajor.repository.domain;

import java.util.Set;
// 필드별 토큰셋
public record DeptDoc(
    DepartmentRef dept,
    // 키워드
    Set<String> keywordTokens,
    // 과목명
    Set<String> courseNameTokens,
    // 선수과목
    Set<String> prereqTokens,
    // 이수구분
    Set<String> categoryTokens,
    String urlSample            // 학과 URL(대표 1개만)
) {}

