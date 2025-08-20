package nl.janboonen.jetta.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootApplication
public class TestApplication {

    public static void main(String[] args) {
        // Spring Boot application entry point
    }

    @Controller
    static class RestController {

        @PostMapping
        ResponseEntity<TextResponse> generateAsciiArt(@RequestParam("text") String text) {
            return ResponseEntity.ok()
                    .headers(headers -> headers.set(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .body(new TextResponse(HtmlUtils.htmlEscape(text)));
        }

    }

    record TextResponse(String text) {
    }

}
