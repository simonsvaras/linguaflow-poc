package lf.linguageflowpoc.pricing.rules;

import java.math.BigDecimal;
import java.util.Map;
import lf.linguageflowpoc.pricing.config.PricingProperties;
import lf.linguageflowpoc.pricing.domain.Formality;
import lf.linguageflowpoc.pricing.service.PricingContext;
import lf.linguageflowpoc.pricing.service.PricingRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class FormalityRule implements PricingRule {

    private final PricingProperties properties;

    public FormalityRule(PricingProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal apply(PricingContext context) {
        Map<Formality, BigDecimal> coefficients = properties.getFormalityCoefs();
        BigDecimal coef = coefficients.getOrDefault(context.getRequest().formality(), BigDecimal.ONE);
        context.setFormalityCoef(coef);
        BigDecimal amount = context.getAmount().multiply(coef);
        context.setAmount(amount);
        return amount;
    }
}
