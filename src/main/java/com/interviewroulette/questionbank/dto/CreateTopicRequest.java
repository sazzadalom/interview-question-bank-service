
package com.interviewroulette.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(

		@NotBlank(message = "Topic name is required") @Size(max = 150, message = "Topic name cannot exceed 150 characters") String name,

		@Size(max = 500, message = "Description cannot exceed 500 characters") String description

) {
}
