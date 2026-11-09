
package com.interviewroulette.questionbank.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;


/**
 * @class QuestionNormalizer
 *
 * <p>
 * 
 * This implements our agreed duplicate-detection normalization rules:
 *  Uni-code NFKC normalization.
 *  Trim leading and trailing whitespace.
 *  Collapse repeated whitespace.
 *  Case-insensitive comparison using Locale.ROOT.
 *  Preserve punctuation and technical symbols.
 *  SHA-256 hash generation.
 *  
 * </p>
 * 
 * */
public final class QuestionNormalizer {

    private QuestionNormalizer() {
    }

    public static String normalize(String questionText) {

        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException(
                "Question text cannot be empty"
            );
        }

        String normalized = Normalizer.normalize(
            questionText,
            Normalizer.Form.NFKC
        );

        normalized = normalized
            .trim()
            .replaceAll("(?U)\\s+", " ")
            .toLowerCase(Locale.ROOT);

        return normalized;
    }

    public static String generateHash(String questionText) {

        String normalized = normalize(questionText);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                normalized.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 algorithm is not available",
                exception
            );
        }
    }
}
