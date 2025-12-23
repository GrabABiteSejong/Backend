package project.pleasemajor.repository.domain;

import java.util.Set;
// 필드별 토큰셋
public record DeptDoc(
    DepartmentRef dept,
    Set<String> keywordTokens,   // ★ 키워드 칼럼
    Set<String> courseNameTokens,
    Set<String> prereqTokens,
    Set<String> categoryTokens,
    String urlSample            // 학과 URL(대표 1개만)
) {}

