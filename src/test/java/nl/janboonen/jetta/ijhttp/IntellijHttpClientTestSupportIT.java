package nl.janboonen.jetta.ijhttp;

import nl.janboonen.jetta.test.TestApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@IntellijHttpClientFile(value = "src/test/resources/ijhttp/test.http", environmentFile = "src/test/resources/http-client.env.json")
public class IntellijHttpClientTestSupportIT extends IntellijHttpClientTestSupport {

    @LocalServerPort
    private int port;

    @Override
    public int getPort() {
        return port;
    }

}
