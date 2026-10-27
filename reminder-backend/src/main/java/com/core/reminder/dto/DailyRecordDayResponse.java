package com.core.reminder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DailyRecordDayResponse {
    DailyRecordResponse record;
    LatestHeight latestHeight;

    @Value
    public static class LatestHeight {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDate recordDate;
        BigDecimal heightCm;
    }
}
