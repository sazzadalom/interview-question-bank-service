package com.interviewroulette.questionbank.service.upload;

import com.interviewroulette.questionbank.dto.ExcelUploadResponse;
import com.interviewroulette.questionbank.dto.InvalidExcelRow;
import com.interviewroulette.questionbank.entity.*;
import com.interviewroulette.questionbank.repository.*;
import com.interviewroulette.questionbank.util.QuestionNormalizer;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ExcelQuestionImportService {

	private final TopicRepository topicRepository;
	private final QuestionRepository questionRepository;
	private final QuestionUploadBatchRepository batchRepository;
	private final QuestionContributionRepository contributionRepository;

	public ExcelQuestionImportService(TopicRepository topicRepository, QuestionRepository questionRepository,
			QuestionUploadBatchRepository batchRepository, QuestionContributionRepository contributionRepository) {
		this.topicRepository = topicRepository;
		this.questionRepository = questionRepository;
		this.batchRepository = batchRepository;
		this.contributionRepository = contributionRepository;
	}

	@Transactional
	public ExcelUploadResponse importQuestions(MultipartFile file, UUID candidateId, Long selectedTopicId) {

		validateFile(file, candidateId);

		QuestionUploadBatch batch = batchRepository
				.saveAndFlush(new QuestionUploadBatch(candidateId, file.getOriginalFilename()));

		List<InvalidExcelRow> invalidDetails = new ArrayList<>();

		int totalRows = 0;
		int validRows = 0;
		int newQuestions = 0;
		int duplicateQuestions = 0;

		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

			if (workbook.getNumberOfSheets() == 0) {
				throw new IllegalArgumentException("Excel file does not contain a worksheet");
			}

			Sheet sheet = workbook.getSheetAt(0);

			if (sheet.getPhysicalNumberOfRows() == 0) {
				throw new IllegalArgumentException("Excel worksheet is empty");
			}

			DataFormatter formatter = new DataFormatter();

			Row headerRow = sheet.getRow(sheet.getFirstRowNum());

			Map<String, Integer> headers = readHeaders(headerRow, formatter);

			Integer questionColumn = headers.get("question");
			Integer topicColumn = headers.get("topic");
			Integer difficultyColumn = headers.get("difficulty");
			Integer expectedAnswerColumn = headers.get("expected answer");

			if (questionColumn == null) {
				throw new IllegalArgumentException("Required Excel column 'Question' is missing");
			}

			if (topicColumn == null && selectedTopicId == null) {
				throw new IllegalArgumentException("Provide a Topic column or select a topic for the upload");
			}

			Topic selectedTopic = null;

			if (topicColumn == null) {
				selectedTopic = topicRepository.findById(selectedTopicId)
						.orElseThrow(() -> new IllegalArgumentException("Selected topic not found"));

				if (!selectedTopic.getActive()) {
					throw new IllegalArgumentException("Selected topic is inactive");
				}
			}

			int firstDataRow = sheet.getFirstRowNum() + 1;

			for (int rowIndex = firstDataRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

				totalRows++;

				Row row = sheet.getRow(rowIndex);

				try {
					String questionText = getCellValue(row, questionColumn, formatter);

					if (questionText == null || questionText.isBlank()) {
						throw new IllegalArgumentException("Question is empty");
					}

					Topic topic;

					if (topicColumn != null) {

						String topicName = getCellValue(row, topicColumn, formatter);

						if (topicName == null || topicName.isBlank()) {
							throw new IllegalArgumentException("Topic is empty");
						}

						topic = topicRepository.findByNameIgnoreCase(topicName.trim())
								.orElseThrow(() -> new IllegalArgumentException("Topic does not exist: " + topicName));

						if (!topic.getActive()) {
							throw new IllegalArgumentException("Topic is inactive: " + topicName);
						}

					} else {
						topic = selectedTopic;
					}

					String difficulty = getCellValue(row, difficultyColumn, formatter);

					String expectedAnswer = getCellValue(row, expectedAnswerColumn, formatter);

					String normalizedHash = QuestionNormalizer.generateHash(questionText);

					Optional<Question> existingQuestion = questionRepository
							.findByTopic_IdAndNormalizedHash(topic.getId(), normalizedHash);

					Question question;

					if (existingQuestion.isPresent()) {

						question = existingQuestion.get();
						duplicateQuestions++;

					} else {

						question = new Question();
						question.setTopic(topic);
						question.setQuestionText(questionText.trim());
						question.setNormalizedHash(normalizedHash);
						question.setDifficulty(normalizeOptional(difficulty));
						question.setExpectedAnswer(normalizeOptional(expectedAnswer));

						question = questionRepository.saveAndFlush(question);
						newQuestions++;
					}

					contributionRepository.save(new QuestionContribution(question, candidateId, batch));

					validRows++;

				} catch (IllegalArgumentException exception) {

					invalidDetails.add(new InvalidExcelRow(rowIndex + 1, exception.getMessage()));
				}
			}

		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to read the Excel file", exception);
		}

		int invalidRows = invalidDetails.size();

		batch.complete(totalRows, validRows, newQuestions, duplicateQuestions, invalidRows);

		batchRepository.save(batch);

		return new ExcelUploadResponse(batch.getId(), file.getOriginalFilename(), "COMPLETED", totalRows, validRows,
				newQuestions, duplicateQuestions, invalidRows, List.copyOf(invalidDetails));
	}

	private Map<String, Integer> readHeaders(Row headerRow, DataFormatter formatter) {

		if (headerRow == null) {
			throw new IllegalArgumentException("Excel header row is missing");
		}

		Map<String, Integer> headers = new HashMap<>();

		for (Cell cell : headerRow) {

			String header = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);

			if (!header.isBlank()) {
				headers.put(header, cell.getColumnIndex());
			}
		}

		return headers;
	}

	private String getCellValue(Row row, Integer columnIndex, DataFormatter formatter) {

		if (row == null || columnIndex == null) {
			return null;
		}

		Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		if (cell == null) {
			return null;
		}

		return formatter.formatCellValue(cell).trim();
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private void validateFile(MultipartFile file, UUID candidateId) {

		if (candidateId == null) {
			throw new IllegalArgumentException("Candidate ID is required");
		}

		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Please upload an Excel file");
		}

		String filename = file.getOriginalFilename();

		if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {

			throw new IllegalArgumentException("Only .xlsx Excel files are supported");
		}
	}
}