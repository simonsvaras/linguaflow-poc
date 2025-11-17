package com.lf.api;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.PublishMessageCommandStep1;
import java.util.Map;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class MessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);
    private static final String MESSAGE_NAME = "PaymentReceived";

    private final ZeebeClient zeebeClient;

    public MessageController(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @PostMapping("/payment")
    public ResponseEntity<Void> correlatePayment(@Valid @RequestBody PaymentWebhookRequest request) {
        Map<String, Object> variables = request.variables() == null ? Map.of() : request.variables();

        LOGGER.debug("Correlating {} message for order {} with variables {}", MESSAGE_NAME, request.orderId(), variables);

        PublishMessageCommandStep1 step1 = zeebeClient.newPublishMessageCommand();
        PublishMessageCommandStep1.PublishMessageCommandStep2 step2 = step1.messageName(MESSAGE_NAME);
        PublishMessageCommandStep1.PublishMessageCommandStep3 step3 = step2.correlationKey(request.orderId());
        step3.variables(variables).send().join();

        LOGGER.info("PaymentReceived message correlated for order {}", request.orderId());
        return ResponseEntity.accepted().build();
    }
}
