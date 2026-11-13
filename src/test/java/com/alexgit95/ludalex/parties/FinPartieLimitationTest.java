package com.alexgit95.ludalex.parties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finlimite;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "ludalex.fin.limite-par-minute=5",
        "ludalex.fin.en-tete-ip=X-Test-Ip"
})
@AutoConfigureMockMvc
class FinPartieLimitationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void repond429AuDelaDuSeuilParAdresseSansConsulterLaBase() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/fin/" + "L".repeat(43)).header("X-Test-Ip", "198.51.100.1"))
                    .andExpect(status().isNotFound());
        }

        mvc.perform(get("/fin/" + "L".repeat(43)).header("X-Test-Ip", "198.51.100.1"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
        mvc.perform(get("/fin/" + "L".repeat(43)).header("X-Test-Ip", "198.51.100.2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nApplliquePasLaLimiteAuResteDeLApplication() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(get("/").header("X-Test-Ip", "198.51.100.3"))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/jeux"));
        }
    }
}
