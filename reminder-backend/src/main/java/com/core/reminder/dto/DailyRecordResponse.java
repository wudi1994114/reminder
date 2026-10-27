package com.core.reminder.dto;

import com.common.reminder.model.DailyRecord;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Value
public class DailyRecordResponse {
    Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    LocalDate recordDate;
    BigDecimal weightKg;
    BigDecimal heightCm;
    String diary;
    Long version;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime updatedAt;

    public static DailyRecordResponse from(DailyRecord record) {
        return new DailyRecordResponse(record.getId(), record.getRecordDate(), record.getWeightKg(),
                record.getHeightCm(), record.getDiary(), record.getVersion(), record.getCreatedAt(), record.getUpdatedAt());
    }
}
