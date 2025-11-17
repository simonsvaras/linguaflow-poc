package lf.linguageflowpoc.pricing.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PricingRequest(
    String orderId,
    @NotNull @Min(1) Integer wordCount,
    String sourceLang,
    String targetLang,
    @NotNull Formality formality,
    @NotNull Urgency urgency
) {
}
