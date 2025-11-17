package lf.linguageflowpoc.workers;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobWorker;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Locale;
import java.util.Map;
import lf.linguageflowpoc.pricing.domain.Formality;
import lf.linguageflowpoc.pricing.domain.PricingRequest;
import lf.linguageflowpoc.pricing.domain.PricingResponse;
import lf.linguageflowpoc.pricing.domain.Urgency;
import lf.linguageflowpoc.pricing.service.PricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "lf.workers.pricing.enabled", havingValue = "true", matchIfMissing = true)
public class PricingWorker {

    static final String JOB_TYPE = "pricing.compute";
    static final String WORKER_NAME = "pricingComputeWorker";
    static final String ERROR_CODE = "PRICING_FAILED";
    private static final int DEFAULT_WORD_COUNT = 1000;
    private static final String DEFAULT_FORMALITY = "neutral";
    private static final String DEFAULT_URGENCY = "normal";

    private static final Logger LOGGER = LoggerFactory.getLogger(PricingWorker.class);

    private final ZeebeClient zeebeClient;
    private final PricingService pricingService;
    private JobWorker worker;

    public PricingWorker(ZeebeClient zeebeClient, PricingService pricingService) {
        this.zeebeClient = zeebeClient;
        this.pricingService = pricingService;
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
            String orderId = resolveOrderId(variables.get("orderId"), job.getBpmnProcessId());

            LOGGER.debug("pricing.compute job {} input wordCount={}, formality={}, urgency={}",
                job.getKey(), wordCount, formality, urgency);

            PricingRequest request = new PricingRequest(
                orderId,
                wordCount,
                null,
                null,
                Formality.valueOf(formality.toUpperCase(Locale.ROOT)),
                Urgency.valueOf(urgency.toUpperCase(Locale.ROOT))
            );

            PricingResponse response = pricingService.computeQuote(request);
            Map<String, Object> payload = Map.of("quote", response);

            jobClient.newCompleteCommand(job)
                .variables(payload)
                .send()
                .join();

            LOGGER.info("Completed pricing job {} with totalPrice {} {}",
                job.getKey(), response.totalPrice(), response.currency());
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

    private String resolveOrderId(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }
}
