package project.pleasemajor.repository.domain;


public record CourseRow(
    int year,             // 학년
    int semester,         // 학기
    String category,      // 이수구분
    String name,          // 과목명
    String prerequisite,  // 선수과목
    String note,          // 비고 (지금은 거의 안 씀)
    String keywords,      // 키워드 (진로/역량 태그)
    String url            // 학과/전공 소개 URL
) {}
