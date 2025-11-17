package lf.linguageflowpoc.pricing.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import lf.linguageflowpoc.pricing.domain.Formality;
import lf.linguageflowpoc.pricing.domain.Urgency;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pricing")
public class PricingProperties {

    private BigDecimal baseRate = BigDecimal.ZERO;
    private String currency = "CZK";
    private Duration expirationDuration = Duration.ofHours(48);
    private Map<Formality, BigDecimal> formalityCoefs = new EnumMap<>(Formality.class);
    private Map<Urgency, BigDecimal> urgencyCoefs = new EnumMap<>(Urgency.class);

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public void setBaseRate(BigDecimal baseRate) {
        this.baseRate = baseRate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Duration getExpirationDuration() {
        return expirationDuration;
    }

    public void setExpirationDuration(Duration expirationDuration) {
        this.expirationDuration = expirationDuration;
    }

    public Map<Formality, BigDecimal> getFormalityCoefs() {
        return formalityCoefs;
    }

    public void setFormalityCoefs(Map<Formality, BigDecimal> formalityCoefs) {
        this.formalityCoefs = formalityCoefs;
    }

    public Map<Urgency, BigDecimal> getUrgencyCoefs() {
        return urgencyCoefs;
    }

    public void setUrgencyCoefs(Map<Urgency, BigDecimal> urgencyCoefs) {
        this.urgencyCoefs = urgencyCoefs;
    }
}
