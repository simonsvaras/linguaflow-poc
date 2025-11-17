package lf.linguageflowpoc.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lf.linguageflowpoc.pricing.config.PricingProperties;
import lf.linguageflowpoc.pricing.domain.PricingRequest;
import lf.linguageflowpoc.pricing.domain.PricingResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class PricingServiceImpl implements PricingService {

    private final PricingProperties properties;
    private final List<PricingRule> rules;
    private final Clock clock;

    public PricingServiceImpl(PricingProperties properties, List<PricingRule> rules, Clock clock) {
        this.properties = properties;
        this.rules = rules;
        this.clock = clock;
    }

    @Override
    public PricingResponse computeQuote(PricingRequest request) {
        validate(request);
        PricingContext context = new PricingContext(request);
        if (CollectionUtils.isEmpty(rules)) {
            throw new IllegalStateException("No pricing rules configured");
        }
        rules.forEach(rule -> rule.apply(context));
        BigDecimal roundedPrice = context.getAmount().setScale(0, RoundingMode.CEILING);
        Instant expiration = Instant.now(clock).plus(properties.getExpirationDuration());
        return context.toResponse(roundedPrice, properties.getCurrency(), expiration);
    }

    private void validate(PricingRequest request) {
        if (request.wordCount() == null || request.wordCount() <= 0) {
            throw new IllegalArgumentException("wordCount must be greater than 0");
        }
        if (request.formality() == null) {
            throw new IllegalArgumentException("formality must be provided");
        }
        if (request.urgency() == null) {
            throw new IllegalArgumentException("urgency must be provided");
        }
    }
}
