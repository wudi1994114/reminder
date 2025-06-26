package com.wwmty.stream.consumer.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StreamEvent {

    private Long id;

    private LocalDateTime createdAt;

    private String payload;

    private String streamKey;

    private String messageId;
} 