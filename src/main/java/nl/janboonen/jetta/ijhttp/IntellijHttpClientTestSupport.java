package nl.janboonen.jetta.ijhttp;

import nl.janboonen.jetta.exception.JettaException;
import nl.janboonen.jetta.testcontainers.ExitCodeWaitStrategy;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.MountableFile;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class IntellijHttpClientTestSupport {

    private static final String IJHTTP_WORKDIR = "/workdir/";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final IntellijHttpClientFile annotation;

    private final DocumentBuilderFactory documentFactory;

    IntellijHttpClientTestSupport() {
        this.annotation = this.getClass().getAnnotation(IntellijHttpClientFile.class);
        if (annotation == null) {
            throw new IllegalStateException("@HttpTestFile must be present on class");
        }
        try {
            documentFactory = DocumentBuilderFactory.newInstance();
            documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            throw new JettaException("Failed to configure XML parser", e);
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

        try (
                GenericContainer<?> ijhttpContainer = new GenericContainer<>(annotation.dockerImage())
        ) {
            logger.info("Running all tests in {} using JetBrains' IntelliJ HTTP Client against http://{}:{}", httpFile, ijhttpContainer.getHost(), getPort());

            ijhttpContainer
                    .withLogConsumer(slf4jLogConsumer(LoggerFactory.getLogger("ijhttp")))
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(httpFile),
                            IJHTTP_WORKDIR + httpFileName
                    )
                    .waitingFor(new ExitCodeWaitStrategy()
                            .withAllowedExitCodes(0, 1)
                            .withMaxRunningTime(Duration.of(annotation.maxRunningTimeInSeconds(), ChronoUnit.SECONDS))).withCommand(generateIjHttpClientCommand(annotation, ijhttpContainer, httpFileName));
            ijhttpContainer.start();
            ijhttpContainer.copyFileFromContainer(IJHTTP_WORKDIR + reportFileName, reportFile.toString());
            if (!reportFile.toFile().exists()) {
                throw new JettaException("ijhttpContainer did not produce a report: " + reportFile);
            }
        }

        return extractTestCases(reportFile);
    }

    @SuppressWarnings("java:S5960")
    private String[] generateIjHttpClientCommand(IntellijHttpClientFile annotation, GenericContainer<?> ijhttp, String httpFileName) {
        List<String> command = new ArrayList<>();

        if (!annotation.environmentFile().isBlank()) {
            var envFile = Paths.get(annotation.environmentFile()).toAbsolutePath();
            var envFileName = envFile.getFileName().toString();
            ijhttp.withCopyFileToContainer(
                    MountableFile.forHostPath(envFile),
                    IJHTTP_WORKDIR + envFileName
            );
            command.add("--env-file");
            command.add(IJHTTP_WORKDIR + envFileName);
        }

        if (!annotation.privateEnvironmentFile().isBlank()) {
            var privateEnvFile = Paths.get(annotation.privateEnvironmentFile()).toAbsolutePath();
            var privateEnvFileName = privateEnvFile.getFileName().toString();
            ijhttp.withCopyFileToContainer(
                    MountableFile.forHostPath(privateEnvFile),
                    IJHTTP_WORKDIR + privateEnvFileName
            );
            command.add("--private-env-file");
            command.add(IJHTTP_WORKDIR + privateEnvFileName);
        }

        if (!annotation.environment().isBlank()) {
            command.add("--env");
            command.add(annotation.environment());
        }

        command.add("--env-variables");
        command.add("baseUrl=http://host.testcontainers.internal:" + getPort());
        command.add("--report");
        command.add(IJHTTP_WORKDIR);
        command.add("-D");
        command.add(IJHTTP_WORKDIR + httpFileName);

        return command.toArray(new String[0]);
    }

    @SuppressWarnings("java:S5960")
    private List<DynamicTest> extractTestCases(Path reportFile) {
        List<DynamicTest> tests = new ArrayList<>();
        try {
            var doc = documentFactory.newDocumentBuilder().parse(reportFile.toFile());
            var testcases = doc.getElementsByTagName("testcase");

            for (int i = 0; i < testcases.getLength(); i++) {
                var node = testcases.item(i);
                var name = node.getAttributes().getNamedItem("name").getTextContent();
                var failed = node.getChildNodes().getLength() > 0;
                var failureMessage = failed ? node.getChildNodes().item(0).getTextContent().trim() : null;

                tests.add(DynamicTest.dynamicTest(name, () -> {
                    if (failed) {
                        fail("ijhttp test failed: " + failureMessage);
                    }
                }));
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new JettaException("Failed to parse ijhttp report: " + reportFile, e);
        }
        return tests;
    }

    static Consumer<OutputFrame> slf4jLogConsumer(Logger logger) {
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
