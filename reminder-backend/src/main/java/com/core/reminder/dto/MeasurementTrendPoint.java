package com.core.reminder.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
public class MeasurementTrendPoint {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    LocalDate recordDate;
    BigDecimal value;
}
