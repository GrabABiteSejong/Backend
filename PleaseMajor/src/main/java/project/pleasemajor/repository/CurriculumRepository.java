package project.pleasemajor.repository;

import java.util.List;
import java.util.Optional;
import project.pleasemajor.repository.domain.CourseRow;
import project.pleasemajor.repository.domain.DepartmentRef;

public interface CurriculumRepository {

  List<DepartmentRef> getAllDepartments();
  Optional<List<CourseRow>> findCourses(DepartmentRef dept);

}
