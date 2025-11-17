package com.lf.workers;

import com.lf.service.PricingCalculator;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobWorker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PricingWorker {

    static final String JOB_TYPE = "pricing.compute";
    static final String WORKER_NAME = "pricingComputeWorker";
    static final String ERROR_CODE = "PRICING_FAILED";
    private static final int DEFAULT_WORD_COUNT = 1000;
    private static final String DEFAULT_FORMALITY = "neutral";
    private static final String DEFAULT_URGENCY = "normal";
    private static final Duration QUOTE_TTL = Duration.ofHours(48);

    private static final Logger LOGGER = LoggerFactory.getLogger(PricingWorker.class);

    private final ZeebeClient zeebeClient;
    private final PricingCalculator pricingCalculator;
    private final Clock clock;
    private JobWorker worker;

    public PricingWorker(ZeebeClient zeebeClient, PricingCalculator pricingCalculator, Clock clock) {
        this.zeebeClient = zeebeClient;
        this.pricingCalculator = pricingCalculator;
        this.clock = clock;
    }

    @PostConstruct
    public void register() {
        this.worker = zeebeClient.newWorker()
            .jobType(JOB_TYPE)
            .handler(this::handleJob)
            .name(WORKER_NAME)
            .fetchVariables("wordCount", "formality", "urgency")
            .open();

        LOGGER.info("Registered Zeebe worker '{}' for jobType {}", WORKER_NAME, JOB_TYPE);
    }

    @PreDestroy
    public void shutdown() {
        if (worker != null) {
            worker.close();
        }
    }

    private void handleJob(JobClient jobClient, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            int wordCount = extractNumber(variables.get("wordCount"), DEFAULT_WORD_COUNT);
            String formality = extractText(variables.get("formality"), DEFAULT_FORMALITY);
            String urgency = extractText(variables.get("urgency"), DEFAULT_URGENCY);

            LOGGER.debug("pricing.compute job {} input wordCount={}, formality={}, urgency={}",
                job.getKey(), wordCount, formality, urgency);

            long totalPrice = pricingCalculator.calculateTotalPrice(wordCount, formality, urgency);
            Instant expirationAt = Instant.now(clock).plus(QUOTE_TTL);
            Map<String, Object> payload = Map.of(
                "quote", Map.of(
                    "totalPrice", totalPrice,
                    "currency", "CZK",
                    "expirationAt", expirationAt.toString()
                )
            );

            jobClient.newCompleteCommand(job)
                .variables(payload)
                .send()
                .join();

            LOGGER.info("Completed pricing job {} with totalPrice {} CZK", job.getKey(), totalPrice);
        } catch (Exception ex) {
            LOGGER.error("Pricing job {} failed", job.getKey(), ex);
            jobClient.newThrowErrorCommand(job)
                .errorCode(ERROR_CODE)
                .errorMessage(ex.getMessage())
                .send()
                .join();
        }
    }

    private int extractNumber(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                LOGGER.debug("Unable to parse '{}' as number", text);
            }
        }
        return fallback;
    }

    private String extractText(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return fallback;
        }
        return text.toLowerCase(Locale.ROOT);
    }
}
