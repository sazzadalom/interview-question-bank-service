package com.interviewroulette.questionbank.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "question_contributions")
public class QuestionContribution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

//    @Column(name = "candidate_id", nullable = false)
//    private UUID candidateId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "candidate_id", nullable = false, length = 36)
    private UUID candidateId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_batch_id", nullable = false)
    private QuestionUploadBatch uploadBatch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected QuestionContribution() {
    }

    public QuestionContribution(
        Question question,
        UUID candidateId,
        QuestionUploadBatch uploadBatch
    ) {
        this.question = question;
        this.candidateId = candidateId;
        this.uploadBatch = uploadBatch;
        this.createdAt = Instant.now();
    }
}