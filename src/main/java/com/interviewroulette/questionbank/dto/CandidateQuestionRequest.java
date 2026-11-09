package com.interviewroulette.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CandidateQuestionRequest(

        @NotBlank(message = "Question text is required")
        @Size(max = 10000, message = "Question text is too long")
        String question,

        String difficulty,

        @Size(max = 20000, message = "Expected answer is too long")
        String expectedAnswer

) {
}