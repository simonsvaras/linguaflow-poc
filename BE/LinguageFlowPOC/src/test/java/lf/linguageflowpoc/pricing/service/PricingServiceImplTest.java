package lf.linguageflowpoc.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lf.linguageflowpoc.pricing.config.PricingProperties;
import lf.linguageflowpoc.pricing.domain.Formality;
import lf.linguageflowpoc.pricing.domain.PricingRequest;
import lf.linguageflowpoc.pricing.domain.PricingResponse;
import lf.linguageflowpoc.pricing.domain.Urgency;
import lf.linguageflowpoc.pricing.rules.BaseRateRule;
import lf.linguageflowpoc.pricing.rules.FormalityRule;
import lf.linguageflowpoc.pricing.rules.UrgencyRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingServiceImplTest {

    private PricingService service;
    private PricingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PricingProperties();
        properties.setBaseRate(new BigDecimal("0.8"));
        properties.setCurrency("CZK");
        properties.setExpirationDuration(Duration.ofHours(48));
        properties.getFormalityCoefs().put(Formality.NEUTRAL, BigDecimal.ONE);
        properties.getFormalityCoefs().put(Formality.FORMAL, new BigDecimal("1.2"));
        properties.getUrgencyCoefs().put(Urgency.NORMAL, BigDecimal.ONE);
        properties.getUrgencyCoefs().put(Urgency.RUSH, new BigDecimal("1.4"));

        List<PricingRule> rules = List.of(
            new BaseRateRule(properties),
            new FormalityRule(properties),
            new UrgencyRule(properties)
        );
        service = new PricingServiceImpl(properties, rules, Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void computesQuoteWithConfiguredCoefficients() {
        PricingRequest request = new PricingRequest("LF-1", 1500, "cs", "en", Formality.FORMAL, Urgency.RUSH);

        PricingResponse response = service.computeQuote(request);

        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("2016"));
        assertThat(response.expirationAt()).isEqualTo(OffsetDateTime.parse("2024-01-03T00:00:00Z"));
        assertThat(response.details().baseRate()).isEqualByComparingTo("0.8");
        assertThat(response.details().formalityCoef()).isEqualByComparingTo("1.2");
        assertThat(response.details().urgencyCoef()).isEqualByComparingTo("1.4");
    }

    @Test
    void throwsOnInvalidWordCount() {
        PricingRequest request = new PricingRequest("LF-1", 0, "cs", "en", Formality.FORMAL, Urgency.NORMAL);

        assertThatThrownBy(() -> service.computeQuote(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("wordCount");
    }
}
