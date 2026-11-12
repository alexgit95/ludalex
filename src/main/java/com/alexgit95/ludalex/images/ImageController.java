package com.alexgit95.ludalex.images;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ImageController {

    private final StockageImages stockage;

    public ImageController(StockageImages stockage) {
        this.stockage = stockage;
    }

    @GetMapping("/images/{nom}")
    public ResponseEntity<Resource> image(@PathVariable String nom) {
        Optional<Resource> ressource = stockage.charger(nom);
        if (ressource.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = FormatImage.depuisNom(nom)
                .map(format -> MediaType.parseMediaType(format.typeMime()))
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(ressource.get());
    }
}
