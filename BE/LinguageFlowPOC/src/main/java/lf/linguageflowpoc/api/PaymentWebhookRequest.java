package lf.linguageflowpoc.api;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record PaymentWebhookRequest(
    @NotBlank String orderId,
    Map<String, Object> variables
) {}
