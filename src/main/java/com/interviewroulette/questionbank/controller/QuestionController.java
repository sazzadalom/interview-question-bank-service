
package com.interviewroulette.questionbank.controller;

import com.interviewroulette.questionbank.dto.CandidateQuestionCreationResponse;
import com.interviewroulette.questionbank.dto.CreateCandidateQuestionsRequest;
import com.interviewroulette.questionbank.dto.QuestionResponse;
import com.interviewroulette.questionbank.service.question.QuestionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class QuestionController {

	private final QuestionService questionService;

	public QuestionController(QuestionService questionService) {
		this.questionService = questionService;
	}

	@GetMapping("/topics/{topicId}/questions")
	public List<QuestionResponse> getQuestionsByTopic(
	    @PathVariable Long topicId
	) {
	    return questionService.getQuestionsByTopic(topicId);
	}

	@GetMapping("/questions/{questionId}")
	public QuestionResponse getQuestion(@PathVariable UUID questionId) {
		return questionService.getQuestion(questionId);
	}
	
	@GetMapping("/candidates/{candidateId}/topics/{topicId}/questions")
	public ResponseEntity<List<QuestionResponse>> getCandidateQuestionsByTopic(@PathVariable UUID candidateId,
			@PathVariable Long topicId) {
		return ResponseEntity.ok(questionService.getCandidateQuestionsByTopic(candidateId, topicId));
	}
	
	
	@PostMapping("/questions/bulk")
	public ResponseEntity<CandidateQuestionCreationResponse> createCandidateQuestions(
	        @Valid @RequestBody CreateCandidateQuestionsRequest request) {

	    return ResponseEntity.ok(
	            questionService.createCandidateQuestions(request)
	    );
	}
}
