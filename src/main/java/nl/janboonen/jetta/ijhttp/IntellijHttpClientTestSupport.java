package nl.janboonen.jetta.ijhttp;

import nl.janboonen.jetta.exception.JettaException;
import nl.janboonen.jetta.testcontainers.ExitCodeWaitStrategy;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

public abstract class IntellijHttpClientTestSupport {

    private static final String IJHTTP_WORKDIR = "/workdir/";

    private static final String DOCKERHOST_HOSTNAME = "host.testcontainers.internal";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final IntellijHttpClientFile annotation;

    private final JunitReportReader junitReportReader = new JunitReportReader();

    private final IntellijHttpClientCommandBuilder commandBuilder = new IntellijHttpClientCommandBuilder();

    IntellijHttpClientTestSupport() {
        this.annotation = this.getClass().getAnnotation(IntellijHttpClientFile.class);
        if (annotation == null) {
            throw new IllegalStateException("@HttpTestFile must be present on class");
        }
    }

    public int getPort() {
        return annotation.servicePort();
    }

    @TestFactory
    List<DynamicTest> generateHttpTests() {
        var httpFile = Paths.get(annotation.value()).toAbsolutePath();
        var httpFileName = httpFile.getFileName().toString();
        var reportFileName = "report.xml";
        var reportDir = Paths.get("target/ijhttp-reports/").toAbsolutePath();
        var reportFile = reportDir.resolve(reportFileName);
        boolean isCreated = reportDir.toFile().mkdirs();
        if (!isCreated && !reportDir.toFile().exists()) {
            throw new JettaException("Could not create report directory: " + reportDir);
        }

        // expose the Spring Boot port to the Testcontainers network
        Testcontainers.exposeHostPorts(getPort());
        try (
                GenericContainer<?> ijhttpContainer = new GenericContainer<>(annotation.dockerImage())
        ) {
            logger.info("Running all tests in {} using JetBrains' IntelliJ HTTP Client against http://{}:{}", httpFile, DOCKERHOST_HOSTNAME, getPort());

            ijhttpContainer
                    .withLogConsumer(slf4jLogConsumer(LoggerFactory.getLogger("ijhttp")))
                    .withExposedPorts(getPort())
                    .withAccessToHost(true)
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(httpFile),
                            IJHTTP_WORKDIR + httpFileName
                    )
                    .waitingFor(new ExitCodeWaitStrategy()
                            .withAllowedExitCodes(0, 1)
                            .withMaxRunningTime(Duration.of(annotation.maxRunningTimeInSeconds(), ChronoUnit.SECONDS)))
                    .withCommand(commandBuilder.generate(annotation, ijhttpContainer, httpFileName, getPort()));
            ijhttpContainer.start();
            ijhttpContainer.copyFileFromContainer(IJHTTP_WORKDIR + reportFileName, reportFile.toString());
            if (!reportFile.toFile().exists()) {
                throw new JettaException("ijhttpContainer did not produce a report: " + reportFile);
            }
        }

        return junitReportReader.extractTestCases(reportFile);
    }

    private static Consumer<OutputFrame> slf4jLogConsumer(Logger logger) {
        return frame -> {
            String text = frame.getUtf8String().trim();
            switch (frame.getType()) {
                case STDERR -> logger.error(text);
                case STDOUT -> logger.info(text);
                default -> logger.debug(text);
            }
        };
    }

}
