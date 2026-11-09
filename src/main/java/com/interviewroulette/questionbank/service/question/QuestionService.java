package com.interviewroulette.questionbank.service.question;

import com.interviewroulette.questionbank.dto.CandidateQuestionCreationResponse;
import com.interviewroulette.questionbank.dto.CandidateQuestionRequest;
import com.interviewroulette.questionbank.dto.CreateCandidateQuestionsRequest;
import com.interviewroulette.questionbank.dto.QuestionResponse;
import com.interviewroulette.questionbank.entity.Question;
import com.interviewroulette.questionbank.entity.QuestionContribution;
import com.interviewroulette.questionbank.entity.QuestionUploadBatch;
import com.interviewroulette.questionbank.entity.Topic;
import com.interviewroulette.questionbank.repository.QuestionContributionRepository;
import com.interviewroulette.questionbank.repository.QuestionRepository;
import com.interviewroulette.questionbank.repository.QuestionUploadBatchRepository;
import com.interviewroulette.questionbank.repository.TopicRepository;
import com.interviewroulette.questionbank.util.QuestionNormalizer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final QuestionContributionRepository questionContributionRepository;
    private final QuestionUploadBatchRepository batchRepository;

    public QuestionService(
            QuestionRepository questionRepository,
            TopicRepository topicRepository,
            QuestionContributionRepository questionContributionRepository,
            QuestionUploadBatchRepository batchRepository) {

        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.questionContributionRepository = questionContributionRepository;
        this.batchRepository = batchRepository;
    }

    public List<QuestionResponse> getQuestionsByTopic(Long topicId) {

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found"));

        return questionRepository.findAllByTopic_Id(topicId)
                .stream()
                .map(question -> toResponse(question, topic))
                .toList();
    }

    public QuestionResponse getQuestion(UUID questionId) {

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        return toResponse(question, question.getTopic());
    }

    public List<QuestionResponse> getCandidateQuestionsByTopic(
            UUID candidateId,
            Long topicId) {

        return questionContributionRepository
                .findDistinctQuestionsByCandidateAndTopic(candidateId, topicId)
                .stream()
                .map(question -> toResponse(question, question.getTopic()))
                .toList();
    }

    @Transactional
    public CandidateQuestionCreationResponse createCandidateQuestions(
            CreateCandidateQuestionsRequest request) {

        UUID candidateId = request.candidateId();

        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new IllegalArgumentException("Topic not found"));

        if (!Boolean.TRUE.equals(topic.getActive())) {
            throw new IllegalArgumentException("Topic is inactive");
        }

        QuestionUploadBatch batch = batchRepository.saveAndFlush(
                new QuestionUploadBatch(candidateId, "MANUAL_ENTRY"));

        int newQuestions = 0;
        int duplicateQuestions = 0;

        List<QuestionResponse> savedQuestions = new ArrayList<>();

        for (CandidateQuestionRequest item : request.questions()) {

            String questionText = item.question().trim();

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
                question.setQuestionText(questionText);
                question.setNormalizedHash(normalizedHash);
                question.setDifficulty(normalizeDifficulty(item.difficulty()));
                question.setExpectedAnswer(normalizeOptional(item.expectedAnswer()));

                question = questionRepository.saveAndFlush(question);

                newQuestions++;
            }

            questionContributionRepository.save(
                    new QuestionContribution(question, candidateId, batch));

            savedQuestions.add(toResponse(question, topic));
        }

        batch.complete(
                request.questions().size(),
                request.questions().size(),
                newQuestions,
                duplicateQuestions,
                0);

        batchRepository.save(batch);

        return new CandidateQuestionCreationResponse(
                batch.getId(),
                request.questions().size(),
                request.questions().size(),
                newQuestions,
                duplicateQuestions,
                List.copyOf(savedQuestions));
    }

    private QuestionResponse toResponse(Question question, Topic topic) {

        return new QuestionResponse(
                question.getId(),
                topic.getId(),
                topic.getName(),
                question.getQuestionText(),
                question.getDifficulty(),
                question.getExpectedAnswer(),
                question.getCreatedAt());
    }

    private String normalizeOptional(String value) {

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private String normalizeDifficulty(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String difficulty = value.trim().toUpperCase(Locale.ROOT);

        if (!List.of("BEGINNER", "INTERMEDIATE", "ADVANCED").contains(difficulty)) {
            throw new IllegalArgumentException(
                    "Invalid difficulty: " + value
                            + ". Allowed values: BEGINNER, INTERMEDIATE, ADVANCED");
        }

        return difficulty;
    }
}