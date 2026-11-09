package com.interviewroulette.questionbank.dto;

public record InvalidExcelRow(
    int rowNumber,
    String reason
) {
}