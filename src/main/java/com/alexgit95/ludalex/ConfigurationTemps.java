package com.alexgit95.ludalex;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurationTemps {

    @Bean
    Clock horloge() {
        return Clock.systemDefaultZone();
    }
}
