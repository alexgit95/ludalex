package com.alexgit95.ludalex.jeux;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.images.ImageInvalideException;
import com.alexgit95.ludalex.images.ImagePreparee;
import com.alexgit95.ludalex.images.ImageSource;
import com.alexgit95.ludalex.images.ServiceImages;

@Service
@Transactional
public class JeuService {

    public static final int TAILLE_PAGE = 24;

    private final JeuRepository jeux;
    private final PretRepository prets;
    private final ServiceImages images;
    private final PartiesJeu parties;
    private final Clock horloge;

    public JeuService(JeuRepository jeux, PretRepository prets, ServiceImages images, PartiesJeu parties,
            Clock horloge) {
        this.jeux = jeux;
        this.prets = prets;
        this.images = images;
        this.parties = parties;
        this.horloge = horloge;
    }

    public Jeu creer(JeuSaisie saisie) {
        Jeu jeu = new Jeu(validerNom(saisie.nom()));
        renseigner(jeu, saisie);
        return jeux.save(jeu);
    }

    public Jeu modifier(Long id, JeuSaisie saisie) {
        Jeu jeu = trouver(id);
        jeu.setNom(validerNom(saisie.nom()));
        renseigner(jeu, saisie);
        return jeux.save(jeu);
    }

    /** Refusée si le jeu a des parties ; supprime sinon le jeu, ses prêts et son image. */
    public void supprimer(Long id) {
        Jeu jeu = trouver(id);
        if (parties.aDesParties(id)) {
            throw new JeuInvalideException("suppression",
                    "Ce jeu a des parties : supprimez-les d'abord pour pouvoir le supprimer");
        }
        ImageSource image = jeu.getImage();
        prets.supprimerParJeu(id);
        jeux.delete(jeu);
        images.remplacer(image, null);
    }

    @Transactional(readOnly = true)
    public Jeu trouver(Long id) {
        return jeux.findById(id).orElseThrow(() -> new JeuIntrouvableException(id));
    }

    @Transactional(readOnly = true)
    public Page<VignetteJeu> lister(int page) {
        Page<Jeu> pageJeux = jeux.findAll(PageRequest.of(Math.max(page, 0), TAILLE_PAGE,
                Sort.by(Sort.Order.asc("nom").ignoreCase(), Sort.Order.asc("id"))));
        List<Long> ids = pageJeux.getContent().stream().map(Jeu::getId).toList();
        Map<Long, String> emprunteurs = ids.isEmpty() ? Map.of()
                : prets.emprunteursEnCours(ids).stream()
                        .collect(Collectors.toMap(EmprunteurJeu::jeuId, EmprunteurJeu::emprunteur, (a, b) -> a));
        return pageJeux.map(jeu -> new VignetteJeu(jeu, emprunteurs.get(jeu.getId())));
    }

    @Transactional(readOnly = true)
    public FicheJeu fiche(Long id) {
        Jeu jeu = trouver(id);
        return new FicheJeu(jeu, prets.trouverEnCours(id).orElse(null), prets.historique(id));
    }

    private void renseigner(Jeu jeu, JeuSaisie saisie) {
        jeu.setPrix(validerPrix(saisie.prix()));
        jeu.setDateAchat(validerDate(saisie.dateAchat()));
        jeu.setCooperatif(saisie.cooperatif());
        appliquerImage(jeu, saisie);
    }

    private String validerNom(String nom) {
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty()) {
            throw new JeuInvalideException("nom", "Le nom est obligatoire");
        }
        if (propre.length() > Jeu.NOM_MAX) {
            throw new JeuInvalideException("nom", "Le nom ne doit pas dépasser " + Jeu.NOM_MAX + " caractères");
        }
        return propre;
    }

    private BigDecimal validerPrix(BigDecimal prix) {
        if (prix == null) {
            return null;
        }
        if (prix.signum() < 0) {
            throw new JeuInvalideException("prix", "Le prix ne peut pas être négatif");
        }
        if (prix.stripTrailingZeros().scale() > 2) {
            throw new JeuInvalideException("prix", "Le prix admet au plus deux décimales");
        }
        if (prix.compareTo(Jeu.PRIX_MAX) > 0) {
            throw new JeuInvalideException("prix", "Le prix est trop élevé");
        }
        return prix.setScale(2);
    }

    private LocalDate validerDate(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now(horloge))) {
            throw new JeuInvalideException("dateAchat", "La date d'achat ne peut pas être dans le futur");
        }
        return date;
    }

    /** Applique l'image et la couleur de fond ; l'ancien fichier est supprimé après le commit. */
    private void appliquerImage(Jeu jeu, JeuSaisie saisie) {
        EntreeImage entree = saisie.image() == null ? EntreeImage.inchangee() : saisie.image();
        ImagePreparee nouvelle = null;
        boolean retirer = false;
        try {
            if (entree.aFichier()) {
                nouvelle = images.preparerUpload(entree.fichier());
            } else if (entree.aUrl()) {
                nouvelle = images.preparerUrl(entree.url());
            } else {
                retirer = entree.retirer();
            }
        } catch (ImageInvalideException e) {
            throw new JeuInvalideException("image", e.getMessage());
        }
        try {
            jeu.setCouleurFond(images.resoudreCouleur(saisie.couleur(), jeu.getCouleurFond(), nouvelle));
        } catch (ImageInvalideException e) {
            throw new JeuInvalideException("couleur", e.getMessage());
        }
        ImageSource ancienne = jeu.getImage();
        ImageSource finale = nouvelle != null ? nouvelle.source() : (retirer ? null : ancienne);
        images.remplacer(ancienne, finale);
        jeu.setImage(finale);
    }
}
