package com.alexgit95.ludalex.images;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Point d'entrée commun des jeux et des joueurs pour leurs images. */
@Service
public class ServiceImages {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceImages.class);

    private final StockageImages stockage;
    private final UrlSortanteValidateur validateur;
    private final TelechargeurImage telechargeur;
    private final CalculateurCouleurFond calculateur;
    private final long tailleMax;

    public ServiceImages(StockageImages stockage, UrlSortanteValidateur validateur, TelechargeurImage telechargeur,
            CalculateurCouleurFond calculateur, ImagesProperties proprietes) {
        this.stockage = stockage;
        this.validateur = validateur;
        this.telechargeur = telechargeur;
        this.calculateur = calculateur;
        this.tailleMax = proprietes.tailleMax().toBytes();
    }

    public ImagePreparee preparerUpload(MultipartFile fichier) {
        Upload upload = stocker(fichier);
        return new ImagePreparee(upload.source(), calculateur.calculer(upload.octets()));
    }

    /** Pour les images sans couleur de fond (joueurs) : aucun calcul. */
    public ImageSource enregistrerUpload(MultipartFile fichier) {
        return stocker(fichier).source();
    }

    /** Pour les images sans couleur de fond (joueurs) : aucun téléchargement, seule l'URL est conservée. */
    public ImageSource enregistrerUrl(String url) {
        return ImageSource.url(validateur.validerSyntaxe(url).toString());
    }

    private record Upload(ImageSource source, byte[] octets) {
    }

    private Upload stocker(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ImageInvalideException("Aucun fichier envoyé");
        }
        if (fichier.getSize() > tailleMax) {
            throw new ImageInvalideException("L'image dépasse la taille maximale de 2 Mo");
        }
        byte[] octets;
        try (InputStream flux = fichier.getInputStream()) {
            octets = lireBorne(flux);
        } catch (IOException e) {
            throw new ImageInvalideException("Fichier illisible", e);
        }
        ImageSource source = stockage.enregistrer(octets);
        stockage.supprimerSiAnnulation(source);
        return new Upload(source, octets);
    }

    /** Seule l'URL est conservée ; l'image est téléchargée une fois pour la couleur, avec repli neutre. */
    public ImagePreparee preparerUrl(String url) {
        String adresse = validateur.validerSyntaxe(url).toString();
        String couleur;
        try {
            couleur = calculateur.calculer(telechargeur.telecharger(adresse));
        } catch (ImageInvalideException e) {
            LOG.info("Couleur de fond neutre pour une image distante : {}", e.getMessage());
            couleur = CalculateurCouleurFond.NEUTRE;
        }
        return new ImagePreparee(ImageSource.url(adresse), couleur);
    }

    /**
     * Couleur à enregistrer : choix manuel (différent de la valeur actuelle), sinon couleur calculée
     * pour une nouvelle image, sinon couleur existante.
     */
    public String resoudreCouleur(String couleurSaisie, String couleurExistante, ImagePreparee nouvelle) {
        boolean saisieManuelle = couleurSaisie != null && !couleurSaisie.isBlank()
                && !couleurSaisie.equalsIgnoreCase(couleurExistante);
        if (saisieManuelle) {
            if (!CalculateurCouleurFond.estCouleurValide(couleurSaisie)) {
                throw new ImageInvalideException("Couleur invalide (format attendu : #RRGGBB)");
            }
            return couleurSaisie.toLowerCase();
        }
        if (nouvelle != null) {
            return nouvelle.couleurFond();
        }
        return CalculateurCouleurFond.estCouleurValide(couleurExistante)
                ? couleurExistante : CalculateurCouleurFond.NEUTRE;
    }

    /** Supprime l'ancien fichier uploadé une fois la transaction validée. */
    public void remplacer(ImageSource ancienne, ImageSource nouvelle) {
        if (ancienne != null && !ancienne.equals(nouvelle)) {
            stockage.supprimerApresCommit(ancienne);
        }
    }

    private byte[] lireBorne(InputStream flux) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        byte[] tampon = new byte[8192];
        int lu;
        while ((lu = flux.read(tampon)) != -1) {
            if (sortie.size() + lu > tailleMax) {
                throw new ImageInvalideException("L'image dépasse la taille maximale de 2 Mo");
            }
            sortie.write(tampon, 0, lu);
        }
        return sortie.toByteArray();
    }
}
