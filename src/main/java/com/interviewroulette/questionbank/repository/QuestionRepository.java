package com.interviewroulette.questionbank.repository;

import com.interviewroulette.questionbank.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

	List<Question> findAllByTopic_Id(Long topicId);

	Optional<Question> findByIdAndTopic_Id(UUID questionId, Long topicId);

	boolean existsByTopic_IdAndNormalizedHash(Long topicId, String normalizedHash);

	Optional<Question> findByTopic_IdAndNormalizedHash(Long topicId, String normalizedHash);
}