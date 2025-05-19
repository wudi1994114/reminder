package com.core.reminder.repository;

import com.core.reminder.model.LegalHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LegalHolidayRepository extends JpaRepository<LegalHoliday, Long> {
    
    @Query("SELECT lh FROM LegalHoliday lh WHERE lh.year BETWEEN :startYear AND :endYear ORDER BY lh.year, lh.month, lh.day")
    List<LegalHoliday> findByYearRange(@Param("startYear") Integer startYear, @Param("endYear") Integer endYear);
    
    List<LegalHoliday> findByYear(Integer year);
    
    boolean existsByYear(Integer year);
} 