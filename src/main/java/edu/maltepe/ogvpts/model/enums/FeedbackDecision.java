package edu.maltepe.ogvpts.model.enums;

/**
 * Danışman kararı (feedback.decision).
 * Veritabanında küçük harfle saklanır (ör. onay); dönüşümü FeedbackDecisionConverter yapar.
 */
public enum FeedbackDecision {
    ONAY, DUZELTME
}
