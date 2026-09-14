package com.cosre.cosre_backend.modules.classroom.repository;

import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Optional<Classroom> findByCodeIgnoreCase(String code);

    @EntityGraph(attributePaths = {"subject", "lecturers", "students"})
    @Query("select distinct c from Classroom c order by c.code")
    List<Classroom> findAllDetailed();

    @EntityGraph(attributePaths = {"subject", "lecturers", "students"})
    @Query("select c from Classroom c where c.id = :id")
    Optional<Classroom> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"subject", "lecturers", "students"})
    @Query("select distinct c from Classroom c left join c.lecturers l left join c.students s " +
            "where l.username = :username or s.username = :username order by c.code")
    List<Classroom> findAccessibleByUsername(@Param("username") String username);
}
