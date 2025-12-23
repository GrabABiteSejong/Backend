package project.pleasemajor.repository.domain;


public record DepartmentRef(String track, String dept) {
  public String key() { return track + "::" + dept; }
}

