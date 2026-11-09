package com.interviewroulette.questionbank.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateCandidateQuestionsRequest(

        @NotNull(message = "Candidate ID is required")
        UUID candidateId,

        @NotNull(message = "Topic ID is required")
        Long topicId,

        @NotEmpty(message = "At least one question is required")
        @Size(max = 500, message = "Maximum 500 questions per submission")
        List<@Valid CandidateQuestionRequest> questions

) {
}