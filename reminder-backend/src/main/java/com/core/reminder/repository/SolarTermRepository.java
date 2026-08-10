package com.core.reminder.repository;

import com.core.reminder.model.SolarTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SolarTermRepository extends JpaRepository<SolarTerm, Long> {
    
    /**
     * 根据年份范围查找节气
     */
    @Query("SELECT st FROM SolarTerm st WHERE st.year BETWEEN :startYear AND :endYear ORDER BY st.year, st.month, st.day")
    List<SolarTerm> findByYearRange(@Param("startYear") Integer startYear, @Param("endYear") Integer endYear);
    
    /**
     * 根据年份查找节气
     */
    List<SolarTerm> findByYear(Integer year);
    
    /**
     * 根据年月日查找节气
     */
    SolarTerm findByYearAndMonthAndDay(Integer year, Integer month, Integer day);
    
    /**
     * 检查指定年份的节气数据是否存在
     */
    boolean existsByYear(Integer year);
    
    /**
     * 根据节气名称和年份查找节气
     */
    SolarTerm findByNameAndYear(String name, Integer year);
} 