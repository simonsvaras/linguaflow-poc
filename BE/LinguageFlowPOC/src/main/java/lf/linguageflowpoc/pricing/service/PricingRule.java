package lf.linguageflowpoc.pricing.service;

import java.math.BigDecimal;

public interface PricingRule {
    BigDecimal apply(PricingContext context);
}
