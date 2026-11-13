package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:copiesactives;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "ludalex.images.copies.demarrage-actif=true"
})
class DemarrageActifCopiesTest {

    @Autowired
    private EtatCopies etat;

    @Test
    void laProprietePermetDeDemarrerActif() {
        assertThat(etat.actif()).isTrue();
    }
}
