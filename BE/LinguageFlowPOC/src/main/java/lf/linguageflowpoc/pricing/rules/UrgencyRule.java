package lf.linguageflowpoc.pricing.rules;

import java.math.BigDecimal;
import java.util.Map;
import lf.linguageflowpoc.pricing.config.PricingProperties;
import lf.linguageflowpoc.pricing.domain.Urgency;
import lf.linguageflowpoc.pricing.service.PricingContext;
import lf.linguageflowpoc.pricing.service.PricingRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class UrgencyRule implements PricingRule {

    private final PricingProperties properties;

    public UrgencyRule(PricingProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal apply(PricingContext context) {
        Map<Urgency, BigDecimal> coefficients = properties.getUrgencyCoefs();
        BigDecimal coef = coefficients.getOrDefault(context.getRequest().urgency(), BigDecimal.ONE);
        context.setUrgencyCoef(coef);
        BigDecimal amount = context.getAmount().multiply(coef);
        context.setAmount(amount);
        return amount;
    }
}
