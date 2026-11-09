
package com.interviewroulette.questionbank.dto;

import java.time.Instant;

public record TopicResponse(Long id, String name, String slug, String description, Boolean active, Instant createdAt,
		Instant updatedAt) {
}
