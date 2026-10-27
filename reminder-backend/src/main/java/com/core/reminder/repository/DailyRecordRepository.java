package com.core.reminder.repository;

import com.common.reminder.model.DailyRecord;
import com.core.reminder.dto.MeasurementTrendPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {
    Optional<DailyRecord> findByOwnerUserIdAndRecordDate(Long ownerUserId, LocalDate recordDate);

    List<DailyRecord> findByOwnerUserIdAndRecordDateGreaterThanEqualAndRecordDateLessThanOrderByRecordDateAsc(
            Long ownerUserId, LocalDate startDate, LocalDate endDate);

    Optional<DailyRecord> findFirstByOwnerUserIdAndRecordDateLessThanEqualAndHeightCmIsNotNullOrderByRecordDateDesc(
            Long ownerUserId, LocalDate recordDate);

    @Query("select new com.core.reminder.dto.MeasurementTrendPoint(r.recordDate, r.weightKg) "
            + "from DailyRecord r where r.ownerUserId = :owner and r.recordDate between :start and :end "
            + "and r.weightKg is not null order by r.recordDate asc")
    List<MeasurementTrendPoint> findWeightTrend(@Param("owner") Long ownerUserId,
                                              @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select new com.core.reminder.dto.MeasurementTrendPoint(r.recordDate, r.heightCm) "
            + "from DailyRecord r where r.ownerUserId = :owner and r.recordDate between :start and :end "
            + "and r.heightCm is not null order by r.recordDate asc")
    List<MeasurementTrendPoint> findHeightTrend(@Param("owner") Long ownerUserId,
                                              @Param("start") LocalDate start, @Param("end") LocalDate end);
}
