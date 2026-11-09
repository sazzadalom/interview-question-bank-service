package com.interviewroulette.questionbank.dto;

import java.util.List;
import java.util.UUID;

public record CandidateQuestionCreationResponse(

        UUID batchId,

        int totalQuestions,

        int validQuestions,

        int newQuestions,

        int duplicateQuestions,

        List<QuestionResponse> questions

) {
}