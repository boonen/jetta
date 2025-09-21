package nl.janboonen.jetta.ijhttp;

import nl.janboonen.jetta.exception.JettaException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JunitReportReaderTest {

    private final JunitReportReader reportReader = new JunitReportReader();

    @Test
    void parsesReportFromPath_andDynamicTestsExecuteProperly() {
        Path report = Path.of("src/test/resources/junit/junit_report.xml");
        List<DynamicTest> tests = reportReader.extractTestCases(report);

        assertThat(tests).hasSize(4);
        assertThatCode(tests.getFirst().getExecutable()::execute).doesNotThrowAnyException();
        assertThatThrownBy(tests.getLast().getExecutable()::execute)
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void throwsJettaException_whenReportPathIsMissing() {
        Path missing = Path.of("src/test/resources/junit/nonexistent-report.xml");
        assertThatThrownBy(() -> reportReader.extractTestCases(missing))
                .isInstanceOf(JettaException.class);
    }

    @Test
    void parsesReportFromResources_andDynamicTestsExecuteProperly() throws Exception {
        Path report = Path.of("src/test/resources/junit/junit_report.xml");
        List<DynamicTest> tests = reportReader.extractTestCases(Files.newInputStream(report));

        assertThat(tests).hasSize(4);
        assertThatCode(tests.getFirst().getExecutable()::execute).doesNotThrowAnyException();
        assertThatThrownBy(tests.getLast().getExecutable()::execute)
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void returnsEmptyList_whenNoTestcasesPresent() {
        String xml = "<?xml version=\"1.0\"?>\n<testsuite></testsuite>";
        List<DynamicTest> tests = reportReader.extractTestCases(new ByteArrayInputStream(xml.getBytes()));

        assertThat(tests).isEmpty();
    }

    @Test
    void throwsJettaException_whenReportIsMalformed() {
        String xml = "<testsuite><testcase name=\"bad\"></testsuite";
        ByteArrayInputStream reportFile = new ByteArrayInputStream(xml.getBytes());
        assertThatThrownBy(() -> reportReader.extractTestCases(reportFile))
                .isInstanceOf(JettaException.class);
    }

    @Test
    void failingTestProducesTrimmedFailureMessageInAssertion() {
        String xml = """
                <?xml version="1.0"?>
                <testsuite>
                  <testcase name="failure"><failure>
                    message with spaces    \s
                  </failure></testcase>
                </testsuite>
                """;

        List<DynamicTest> tests = reportReader.extractTestCases(new ByteArrayInputStream(xml.getBytes()));

        assertThatThrownBy(tests.getFirst().getExecutable()::execute)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("message with spaces");
    }

}
