package lf.linguageflowpoc.api;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.PublishMessageCommandStep1;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZeebeClient zeebeClient;

    @Test
    void correlatesPaymentReceivedMessage() throws Exception {
        PublishMessageCommandStep1 step1 = mock(PublishMessageCommandStep1.class);
        PublishMessageCommandStep1.PublishMessageCommandStep2 step2 = mock(PublishMessageCommandStep1.PublishMessageCommandStep2.class);
        PublishMessageCommandStep1.PublishMessageCommandStep3 step3 = mock(PublishMessageCommandStep1.PublishMessageCommandStep3.class);
        ZeebeFuture<PublishMessageResponse> future = mock(ZeebeFuture.class);

        when(zeebeClient.newPublishMessageCommand()).thenReturn(step1);
        when(step1.messageName("PaymentReceived")).thenReturn(step2);
        when(step2.correlationKey("LF-2025-00123")).thenReturn(step3);
        when(step3.variables(anyMap())).thenReturn(step3);
        when(step3.send()).thenReturn(future);
        when(future.join()).thenReturn(mock(PublishMessageResponse.class));

        String payload = "{\"orderId\":\"LF-2025-00123\",\"variables\":{\"paidAt\":\"2025-11-12T09:00:00Z\"}}";

        mockMvc.perform(post("/webhooks/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isAccepted());

        verify(zeebeClient).newPublishMessageCommand();
        verify(step1).messageName("PaymentReceived");
        verify(step2).correlationKey("LF-2025-00123");
        verify(step3).variables(anyMap());
        verify(step3).send();
    }
}
