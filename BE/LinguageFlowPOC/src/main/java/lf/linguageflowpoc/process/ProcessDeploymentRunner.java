package lf.linguageflowpoc.process;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.DeployResourceCommandStep1;
import io.camunda.zeebe.client.api.command.DeployResourceCommandStep1.DeployResourceCommandStep2;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
@ConditionalOnProperty(value = "lf.deployment.process-resources", havingValue = "true", matchIfMissing = true)
public class ProcessDeploymentRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDeploymentRunner.class);
    private static final String[] RESOURCE_PATTERNS = {
        "classpath*:/process/*.bpmn",
        "classpath*:/process/forms/*.form",
        "classpath*:/process/dmn/*.dmn"
    };

    private final ZeebeClient zeebeClient;
    private final ResourcePatternResolver resourceResolver;

    public ProcessDeploymentRunner(ZeebeClient zeebeClient, ResourcePatternResolver resourceResolver) {
        this.zeebeClient = zeebeClient;
        this.resourceResolver = resourceResolver;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ResourceHolder> resources = loadResources();
        if (resources.isEmpty()) {
            LOGGER.warn("No Camunda resources found for deployment using patterns {}", Arrays.toString(RESOURCE_PATTERNS));
            return;
        }

        DeployResourceCommandStep1 deployCommand = zeebeClient.newDeployResourceCommand();
        DeployResourceCommandStep2 commandStep = null;
        for (ResourceHolder resource : resources) {
            commandStep = commandStep == null
                ? deployCommand.addResourceBytes(resource.bytes(), resource.name())
                : commandStep.addResourceBytes(resource.bytes(), resource.name());
        }

        var response = commandStep.send().join();
        resources.forEach(res -> LOGGER.info("Deployed resource {}", res.name()));
        LOGGER.info("Deployed Camunda artifacts, deployment key={}", response.getKey());
    }

    private List<ResourceHolder> loadResources() {
        List<ResourceHolder> resources = new ArrayList<>();
        for (String pattern : RESOURCE_PATTERNS) {
            try {
                Resource[] found = resourceResolver.getResources(pattern);
                for (Resource resource : found) {
                    if (!resource.exists() || !resource.isReadable()) {
                        continue;
                    }
                    String filename = resource.getFilename();
                    if (filename == null) {
                        continue;
                    }
                    try (InputStream inputStream = resource.getInputStream()) {
                        byte[] bytes = StreamUtils.copyToByteArray(inputStream);
                        resources.add(new ResourceHolder(filename, bytes));
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load resources for pattern " + pattern, ex);
            }
        }
        resources.sort(Comparator.comparing(ResourceHolder::name));
        return resources;
    }

    private record ResourceHolder(String name, byte[] bytes) {}
}
