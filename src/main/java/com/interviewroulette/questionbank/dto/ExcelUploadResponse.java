package com.interviewroulette.questionbank.dto;

import java.util.List;
import java.util.UUID;

public record ExcelUploadResponse(
    UUID uploadBatchId,
    String filename,
    String status,
    int totalRows,
    int validRows,
    int newQuestions,
    int duplicateQuestions,
    int invalidRows,
    List<InvalidExcelRow> invalidRowDetails
) {
}