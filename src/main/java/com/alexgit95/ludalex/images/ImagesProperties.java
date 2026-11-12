package com.alexgit95.ludalex.images;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "ludalex.images")
public record ImagesProperties(
        @DefaultValue("./data/images") String repertoire,
        @DefaultValue("2MB") DataSize tailleMax,
        @DefaultValue("50000000") long pixelsMax,
        @DefaultValue("5s") Duration delaiTelechargement,
        @DefaultValue("5") int redirectionsMax) {
}
