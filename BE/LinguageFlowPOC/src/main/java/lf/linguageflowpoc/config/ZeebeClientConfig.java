package lf.linguageflowpoc.config;

import io.camunda.zeebe.client.CredentialsProvider;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CamundaSaasProperties.class, ZeebeGatewayProperties.class})
public class ZeebeClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeebeClientConfig.class);

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(ZeebeClient.class)
    public ZeebeClient zeebeClient(CamundaSaasProperties camundaProps,
                                   ZeebeGatewayProperties gatewayProps) {
        CredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder()
            .authorizationServerUrl(camundaProps.getAuthUrl())
            .audience(String.format("https://%s.%s.zeebe.camunda.io",
                camundaProps.getClusterId(), camundaProps.getRegion()))
            .clientId(camundaProps.getClientId())
            .clientSecret(camundaProps.getClientSecret())
            .build();

        String gatewayAddress = gatewayProps.getGateway();
        LOGGER.info("Connecting to Camunda gateway {}", gatewayAddress);

        return ZeebeClient.newClientBuilder()
            .gatewayAddress(gatewayAddress)
            .credentialsProvider(credentialsProvider)
            .build();
    }
}
