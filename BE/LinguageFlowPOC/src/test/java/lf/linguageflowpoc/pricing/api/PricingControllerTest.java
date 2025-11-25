package lf.linguageflowpoc.pricing.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import lf.linguageflowpoc.pricing.domain.Formality;
import lf.linguageflowpoc.pricing.domain.PricingRequest;
import lf.linguageflowpoc.pricing.domain.Urgency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "lf.workers.pricing.enabled=false",
    "lf.deployment.process-resources=false"
})
@AutoConfigureMockMvc
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ZeebeClient zeebeClient;

    @Test
    void returnsPricingQuote() throws Exception {
        PricingRequest request = new PricingRequest("LF-2025-0001", 2500, "cs", "en", Formality.FORMAL, Urgency.RUSH);

        mockMvc.perform(post("/api/pricing/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("LF-2025-0001"))
            .andExpect(jsonPath("$.totalPrice").exists())
            .andExpect(jsonPath("$.details.baseRate").value(0.8));
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        PricingRequest request = new PricingRequest(null, 0, null, null, Formality.NEUTRAL, Urgency.NORMAL);

        mockMvc.perform(post("/api/pricing/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
