package com.core.reminder.repository;

import com.common.reminder.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByOwnerUserId(Long ownerUserId);

    @Query("SELECT e FROM Exercise e WHERE (e.isPublic = true OR e.ownerUserId = :userId) " +
           "AND (:keyword IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:muscleGroup IS NULL OR e.muscleGroup = :muscleGroup) " +
           "ORDER BY e.createdAt DESC")
    List<Exercise> searchAccessible(@Param("userId") Long userId,
                                    @Param("keyword") String keyword,
                                    @Param("muscleGroup") String muscleGroup);
}


