package lf.linguageflowpoc.pricing.service;

import java.math.BigDecimal;
import lf.linguageflowpoc.pricing.domain.PricingRequest;
import lf.linguageflowpoc.pricing.domain.PricingResponse;

public class PricingContext {

    private final PricingRequest request;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal baseRate = BigDecimal.ZERO;
    private BigDecimal formalityCoef = BigDecimal.ONE;
    private BigDecimal urgencyCoef = BigDecimal.ONE;

    public PricingContext(PricingRequest request) {
        this.request = request;
    }

    public PricingRequest getRequest() {
        return request;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public void setBaseRate(BigDecimal baseRate) {
        this.baseRate = baseRate;
    }

    public BigDecimal getFormalityCoef() {
        return formalityCoef;
    }

    public void setFormalityCoef(BigDecimal formalityCoef) {
        this.formalityCoef = formalityCoef;
    }

    public BigDecimal getUrgencyCoef() {
        return urgencyCoef;
    }

    public void setUrgencyCoef(BigDecimal urgencyCoef) {
        this.urgencyCoef = urgencyCoef;
    }

    public PricingResponse toResponse(BigDecimal totalPrice, String currency, java.time.Instant expirationAt) {
        PricingResponse.PricingDetails details = new PricingResponse.PricingDetails(baseRate, formalityCoef, urgencyCoef);
        return new PricingResponse(request.orderId(), totalPrice, currency, expirationAt, details);
    }
}
