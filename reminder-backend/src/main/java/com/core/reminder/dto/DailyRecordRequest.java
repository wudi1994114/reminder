package com.core.reminder.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import javax.validation.constraints.*;
import java.io.IOException;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyRecordRequest {
    @DecimalMin(value = "0.1", message = "体重须在 0.1–999.9 kg 之间")
    @DecimalMax(value = "999.9", message = "体重须在 0.1–999.9 kg 之间")
    @Digits(integer = 3, fraction = 1, message = "体重最多保留一位小数")
    private BigDecimal weightKg;

    @DecimalMin(value = "0.1", message = "身高须在 0.1–300.0 cm 之间")
    @DecimalMax(value = "300.0", message = "身高须在 0.1–300.0 cm 之间")
    @Digits(integer = 3, fraction = 1, message = "身高最多保留一位小数")
    private BigDecimal heightCm;

    @Size(max = 10000, message = "备注最多 10000 字")
    private String diary;

    @Positive(message = "记录 ID 必须为正整数")
    @JsonDeserialize(using = VersionDeserializer.class)
    private Long recordId;

    @PositiveOrZero(message = "版本号必须为非负整数")
    @JsonDeserialize(using = VersionDeserializer.class)
    private Long version;

    @JsonIgnore
    @AssertTrue(message = "请至少填写体重、身高或备注中的一项")
    public boolean isContentPresent() {
        return weightKg != null || heightCm != null || normalizedDiary() != null;
    }

    public String normalizedDiary() {
        return diary == null || diary.codePoints().allMatch(
                c -> Character.isWhitespace(c) || Character.isSpaceChar(c)) ? null : diary;
    }

    /** Avoid Jackson's default truncation of a fractional optimistic-lock version. */
    public static class VersionDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                throw JsonMappingException.from(parser, "版本号必须为非负整数");
            }
            return parser.getLongValue();
        }
    }
}
