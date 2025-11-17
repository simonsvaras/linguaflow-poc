package lf.linguageflowpoc.pricing.api;

import jakarta.validation.Valid;
import lf.linguageflowpoc.pricing.domain.PricingRequest;
import lf.linguageflowpoc.pricing.domain.PricingResponse;
import lf.linguageflowpoc.pricing.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/quote")
    public ResponseEntity<PricingResponse> computeQuote(@Valid @RequestBody PricingRequest request) {
        return ResponseEntity.ok(pricingService.computeQuote(request));
    }
}
