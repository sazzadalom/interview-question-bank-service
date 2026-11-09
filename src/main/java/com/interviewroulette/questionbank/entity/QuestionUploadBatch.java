package com.interviewroulette.questionbank.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "question_upload_batches")
public class QuestionUploadBatch {

//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", nullable = false, length = 36)
	private UUID id;

//    @Column(name = "candidate_id", nullable = false)
//    private UUID candidateId;
    
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "candidate_id", nullable = false, length = 36)
	private UUID candidateId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "total_rows")
    private int totalRows;

    @Column(name = "valid_rows")
    private int validRows;

    @Column(name = "new_questions")
    private int newQuestions;

    @Column(name = "duplicate_questions")
    private int duplicateQuestions;

    @Column(name = "invalid_rows")
    private int invalidRows;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected QuestionUploadBatch() {
    }

    public QuestionUploadBatch(UUID candidateId, String originalFilename) {
        this.candidateId = candidateId;
        this.originalFilename = originalFilename;
        this.status = "PROCESSING";
        this.startedAt = Instant.now();
    }

    public void complete(
        int totalRows,
        int validRows,
        int newQuestions,
        int duplicateQuestions,
        int invalidRows
    ) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.newQuestions = newQuestions;
        this.duplicateQuestions = duplicateQuestions;
        this.invalidRows = invalidRows;
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }
}