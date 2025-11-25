package lf.linguageflowpoc.pricing.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

public record PricingResponse(
    String orderId,
    BigDecimal totalPrice,
    String currency,
    OffsetDateTime expirationAt,
    PricingDetails details
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PricingDetails(BigDecimal baseRate, BigDecimal formalityCoef, BigDecimal urgencyCoef) {
    }
}
