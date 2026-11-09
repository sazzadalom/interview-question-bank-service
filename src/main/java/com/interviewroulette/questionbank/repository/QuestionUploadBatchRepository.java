package com.interviewroulette.questionbank.repository;

import com.interviewroulette.questionbank.entity.QuestionUploadBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuestionUploadBatchRepository
    extends JpaRepository<QuestionUploadBatch, UUID> {
}