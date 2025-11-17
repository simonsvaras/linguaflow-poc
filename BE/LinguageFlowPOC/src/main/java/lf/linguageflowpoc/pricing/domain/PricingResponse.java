package lf.linguageflowpoc.pricing.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;

public record PricingResponse(
    String orderId,
    BigDecimal totalPrice,
    String currency,
    Instant expirationAt,
    PricingDetails details
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PricingDetails(BigDecimal baseRate, BigDecimal formalityCoef, BigDecimal urgencyCoef) {
    }
}
