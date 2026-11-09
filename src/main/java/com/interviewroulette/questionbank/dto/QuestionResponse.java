package com.interviewroulette.questionbank.dto;

import java.time.Instant;
import java.util.UUID;

public record QuestionResponse(
    UUID id,
    Long topicId,
    String topicName,
    String questionText,
    String difficulty,
    String expectedAnswer,
    Instant createdAt
) {
}