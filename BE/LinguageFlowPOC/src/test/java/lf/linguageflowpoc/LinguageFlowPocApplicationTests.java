package lf.linguageflowpoc;

import io.camunda.zeebe.client.ZeebeClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
    "lf.workers.pricing.enabled=false",
    "lf.deployment.process-resources=false"
})
class LinguageFlowPocApplicationTests {

    @MockBean
    private ZeebeClient zeebeClient;

    @Test
    void contextLoads() {
    }
}
