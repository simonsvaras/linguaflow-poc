package lf.linguageflowpoc.pricing.rules;

import java.math.BigDecimal;
import lf.linguageflowpoc.pricing.config.PricingProperties;
import lf.linguageflowpoc.pricing.service.PricingContext;
import lf.linguageflowpoc.pricing.service.PricingRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class BaseRateRule implements PricingRule {

    private final PricingProperties properties;

    public BaseRateRule(PricingProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal apply(PricingContext context) {
        BigDecimal baseRate = properties.getBaseRate();
        context.setBaseRate(baseRate);
        BigDecimal amount = baseRate.multiply(BigDecimal.valueOf(context.getRequest().wordCount()));
        context.setAmount(amount);
        return amount;
    }
}
