package com.alexgit95.ludalex.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:erreurs;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class PagesErreurTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void affiche404SansDetailTechnique() throws Exception {
        HttpResponse<String> reponse = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/inexistant"))
                        .header("Accept", "text/html").build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(reponse.statusCode()).isEqualTo(404);
        assertThat(reponse.body()).contains("Page introuvable")
                .doesNotContain("Exception")
                .doesNotContain("org.springframework")
                .doesNotContain("trace");
    }
}
