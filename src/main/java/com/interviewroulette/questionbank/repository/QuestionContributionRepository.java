package com.interviewroulette.questionbank.repository;

import com.interviewroulette.questionbank.entity.Question;
import com.interviewroulette.questionbank.entity.QuestionContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionContributionRepository extends JpaRepository<QuestionContribution, Long> {

	@Query("""
			SELECT DISTINCT qc.question
			FROM QuestionContribution qc
			WHERE qc.candidateId = :candidateId
			  AND qc.question.topic.id = :topicId
			""")
	List<Question> findDistinctQuestionsByCandidateAndTopic(@Param("candidateId") UUID candidateId,
			@Param("topicId") Long topicId);
}