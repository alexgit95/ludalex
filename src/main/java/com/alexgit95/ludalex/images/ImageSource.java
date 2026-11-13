package com.alexgit95.ludalex.images;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Image d'une entite : upload ou URL prioritaire, avec copie de secours geree separement. */
@Embeddable
public class ImageSource {

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", length = 10)
    private TypeImage type;

    @Column(name = "image_reference", length = 2048)
    private String reference;

    protected ImageSource() {
    }

    private ImageSource(TypeImage type, String reference) {
        this.type = type;
        this.reference = reference;
    }

    public static ImageSource upload(String nomFichier) {
        return new ImageSource(TypeImage.UPLOAD, nomFichier);
    }

    public static ImageSource url(String url) {
        return new ImageSource(TypeImage.URL, url);
    }

    public TypeImage getType() {
        return type;
    }

    public String getReference() {
        return reference;
    }

    /** Adresse utilisable dans une balise img. */
    public String getUrl() {
        return type == TypeImage.UPLOAD ? "/images/" + reference : reference;
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof ImageSource image
                && type == image.type && Objects.equals(reference, image.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, reference);
    }
}
