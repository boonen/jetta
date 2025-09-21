package nl.janboonen.jetta.ijhttp;

import nl.janboonen.jetta.exception.JettaException;
import org.junit.jupiter.api.DynamicTest;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

final class JunitReportReader {

    private final DocumentBuilderFactory documentFactory;

    JunitReportReader() {
        try {
            documentFactory = DocumentBuilderFactory.newInstance();
            documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            throw new JettaException("Failed to configure XML parser", e);
        }
    }

    public List<DynamicTest> extractTestCases(Path reportFile) {
        try (InputStream is = java.nio.file.Files.newInputStream(reportFile)) {
            return extractTestCases(is);
        } catch (IOException e) {
            throw new JettaException("Failed to read ijhttp report: " + reportFile, e);
        }
    }

    @SuppressWarnings("java:S5960")
    List<DynamicTest> extractTestCases(InputStream reportFile) {
        List<DynamicTest> tests = new ArrayList<>();
        try {
            var doc = documentFactory.newDocumentBuilder().parse(reportFile);
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

}
