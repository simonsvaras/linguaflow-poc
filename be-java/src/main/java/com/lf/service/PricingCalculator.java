package com.lf.service;

import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PricingCalculator {

    private static final double BASE_RATE = 0.8d;
    private static final Map<String, Double> FORMALITY_COEFFICIENTS = Map.of(
        "neutral", 1.0d,
        "formal", 1.2d,
        "creative", 1.3d
    );
    private static final Map<String, Double> URGENCY_COEFFICIENTS = Map.of(
        "normal", 1.0d,
        "rush", 1.4d,
        "overnight", 1.6d
    );

    public long calculateTotalPrice(int wordCount, String formality, String urgency) {
        int sanitizedWordCount = Math.max(wordCount, 0);
        double formalityCoef = lookupCoefficient(FORMALITY_COEFFICIENTS, formality, 1.0d);
        double urgencyCoef = lookupCoefficient(URGENCY_COEFFICIENTS, urgency, 1.0d);
        double rawPrice = BASE_RATE * sanitizedWordCount * formalityCoef * urgencyCoef;
        return Math.round(rawPrice);
    }

    private double lookupCoefficient(Map<String, Double> source, String key, double fallback) {
        if (key == null) {
            return fallback;
        }
        return source.getOrDefault(key.toLowerCase(Locale.ROOT), fallback);
    }
}
