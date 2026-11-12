package com.alexgit95.ludalex.joueurs;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.images.ImageInvalideException;
import com.alexgit95.ludalex.images.ImageSource;
import com.alexgit95.ludalex.images.ServiceImages;

@Service
@Transactional
public class JoueurService {

    public static final int TAILLE_PAGE = 20;

    private final JoueurRepository joueurs;
    private final ServiceImages images;
    private final ParticipationsJoueur participations;

    public JoueurService(JoueurRepository joueurs, ServiceImages images, ParticipationsJoueur participations) {
        this.joueurs = joueurs;
        this.images = images;
        this.participations = participations;
    }

    public Joueur creer(String nom, EntreeImage avatar, EntreeImage imageVictoire) {
        String nomPropre = validerNom(nom, null);
        Joueur joueur = new Joueur(nomPropre);
        joueur.setAvatar(appliquer("avatar", null, avatar));
        joueur.setImageVictoire(appliquer("victoire", null, imageVictoire));
        return joueurs.save(joueur);
    }

    public Joueur modifier(Long id, String nom, EntreeImage avatar, EntreeImage imageVictoire) {
        Joueur joueur = trouverActif(id);
        joueur.changerNom(validerNom(nom, id));
        joueur.setAvatar(appliquer("avatar", joueur.getAvatar(), avatar));
        joueur.setImageVictoire(appliquer("victoire", joueur.getImageVictoire(), imageVictoire));
        return joueurs.save(joueur);
    }

    /** Anonymise le joueur s'il a des parties, le supprime sinon. */
    public void supprimer(Long id) {
        Joueur joueur = trouverActif(id);
        ImageSource avatar = joueur.getAvatar();
        ImageSource imageVictoire = joueur.getImageVictoire();
        if (participations.aDesParticipations(id)) {
            joueur.anonymiser();
            joueurs.save(joueur);
        } else {
            joueurs.delete(joueur);
        }
        images.remplacer(avatar, null);
        images.remplacer(imageVictoire, null);
    }

    @Transactional(readOnly = true)
    public Joueur trouverActif(Long id) {
        return joueurs.findById(id).filter(joueur -> !joueur.isAnonymise())
                .orElseThrow(() -> new JoueurIntrouvableException(id));
    }

    @Transactional(readOnly = true)
    public Page<Joueur> lister(int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE,
                org.springframework.data.domain.Sort.by("nomNormalise"));
        return joueurs.findByAnonymiseFalse(pageable);
    }

    /** Joueurs proposés pour une partie : les joueurs anonymisés en sont exclus. */
    @Transactional(readOnly = true)
    public List<Joueur> selectionnables() {
        return joueurs.findByAnonymiseFalseOrderByNomNormaliseAsc();
    }

    private String validerNom(String nom, Long idExclu) {
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty()) {
            throw new JoueurInvalideException("nom", "Le nom est obligatoire");
        }
        if (propre.length() > Joueur.NOM_MAX) {
            throw new JoueurInvalideException("nom", "Le nom ne doit pas dépasser " + Joueur.NOM_MAX + " caractères");
        }
        String normalise = propre.toLowerCase(java.util.Locale.ROOT);
        boolean existe = idExclu == null
                ? joueurs.existsByNomNormaliseAndAnonymiseFalse(normalise)
                : joueurs.existsByNomNormaliseAndAnonymiseFalseAndIdNot(normalise, idExclu);
        if (existe) {
            throw new JoueurInvalideException("nom", "Un joueur porte déjà ce nom");
        }
        return propre;
    }

    /** Renvoie la nouvelle image à conserver ; l'ancien fichier est supprimé après le commit. */
    private ImageSource appliquer(String champ, ImageSource actuelle, EntreeImage entree) {
        if (entree == null) {
            return actuelle;
        }
        try {
            ImageSource nouvelle;
            if (entree.aFichier()) {
                nouvelle = images.enregistrerUpload(entree.fichier());
            } else if (entree.aUrl()) {
                nouvelle = images.enregistrerUrl(entree.url());
            } else if (entree.retirer()) {
                nouvelle = null;
            } else {
                return actuelle;
            }
            images.remplacer(actuelle, nouvelle);
            return nouvelle;
        } catch (ImageInvalideException e) {
            throw new JoueurInvalideException(champ, e.getMessage());
        }
    }
}
