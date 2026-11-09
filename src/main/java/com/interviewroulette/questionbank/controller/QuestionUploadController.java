package com.interviewroulette.questionbank.controller;

import com.interviewroulette.questionbank.dto.ExcelUploadResponse;
import com.interviewroulette.questionbank.service.upload.ExcelQuestionImportService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/question-uploads")
public class QuestionUploadController {

	private final ExcelQuestionImportService importService;

	public QuestionUploadController(ExcelQuestionImportService importService) {
		this.importService = importService;
	}


	@PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ExcelUploadResponse uploadExcel(@RequestParam("file") MultipartFile file,
			@RequestParam("candidateId") UUID candidateId,
			@RequestParam(value = "topicId", required = false) Long topicId) {
		return importService.importQuestions(file, candidateId, topicId);
	}
}