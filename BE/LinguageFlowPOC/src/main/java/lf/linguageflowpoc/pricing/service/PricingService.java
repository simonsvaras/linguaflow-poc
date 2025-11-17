package lf.linguageflowpoc.pricing.service;

import lf.linguageflowpoc.pricing.domain.PricingRequest;
import lf.linguageflowpoc.pricing.domain.PricingResponse;

public interface PricingService {
    PricingResponse computeQuote(PricingRequest request);
}
