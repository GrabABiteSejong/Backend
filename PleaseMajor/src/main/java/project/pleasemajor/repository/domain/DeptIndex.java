package project.pleasemajor.repository.domain;


import java.util.Map;
import java.util.Set;


public record DeptIndex(
    Map<String, Double> idfMap,
    Set<String> stopwords,
    Map<DepartmentRef, DeptDoc> docs,
    int deptCount
) {}

