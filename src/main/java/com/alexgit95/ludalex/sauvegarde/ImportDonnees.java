package com.alexgit95.ludalex.sauvegarde;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.zip.ZipFile;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.images.ImageSource;
import com.alexgit95.ludalex.images.TypeImage;
import com.alexgit95.ludalex.images.CopieImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.Pret;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Equipe;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.parties.StatutPartie;
import com.alexgit95.ludalex.parties.TokenFinPartie;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.EquipeDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.ImageDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.JeuDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.JoueurDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.ParticipationDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.PartieDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.PretDto;

import jakarta.persistence.EntityManager;

/**
 * Remplace toutes les données dans une seule transaction : suppression dans l'ordre des dépendances puis insertion
 * par lots. De nouveaux identifiants sont générés ; une table de correspondance reconstruit les relations, ce qui
 * rend la restauration indépendante du moteur (séquences PostgreSQL, auto-incrément SQLite).
 */
@Component
public class ImportDonnees {

    private final EntityManager em;

    public ImportDonnees(EntityManager em) {
        this.em = em;
    }

    @Transactional
    public void remplacer(ZipFile zip) {
        supprimerTout();

        Map<Long, Long> joueurs = new HashMap<>();
        Map<Long, Long> jeux = new HashMap<>();
        Map<Long, Long> parties = new HashMap<>();
        Map<Long, Long> equipes = new HashMap<>();
        Compteur compteur = new Compteur();

        lire(zip, FormatSauvegarde.JOUEURS, JoueurDto.class, d -> {
            Joueur joueur = new Joueur(d.nom());
            if (d.anonymise()) {
                joueur.anonymiser();
            } else {
                joueur.setAvatar(image(d.avatar()));
                joueur.setImageVictoire(image(d.imageVictoire()));
            }
            persister(joueur, compteur);
            joueurs.put(d.idSauvegarde(), joueur.getId());
        });
        lire(zip, FormatSauvegarde.JEUX, JeuDto.class, d -> {
            Jeu jeu = new Jeu(d.nom());
            jeu.setImage(image(d.image()));
            if (d.couleurFond() != null) {
                jeu.setCouleurFond(d.couleurFond());
            }
            jeu.setPrix(d.prix() == null ? null : new BigDecimal(d.prix()));
            jeu.setDateAchat(d.dateAchat() == null ? null : LocalDate.parse(d.dateAchat()));
            jeu.setCooperatif(d.cooperatif());
            jeu.setPlusieursVariantes(Boolean.TRUE.equals(d.plusieursVariantes()));
            persister(jeu, compteur);
            jeux.put(d.idSauvegarde(), jeu.getId());
        });
        lire(zip, FormatSauvegarde.PARTIES, PartieDto.class, d -> {
            Partie partie = new Partie(em.getReference(Jeu.class, trouver(jeux, d.jeuId())),
                    LocalDate.parse(d.datePartie()));
            partie.setLieu(d.lieu());
            partie.setStatut(StatutPartie.valueOf(d.statut()));
            if (d.resultatCooperatif() != null) {
                partie.setResultatCooperatif(ResultatCooperatif.valueOf(d.resultatCooperatif()));
            }
            if (d.joueurSuivantId() != null) {
                partie.setJoueurSuivant(em.getReference(Joueur.class, trouver(joueurs, d.joueurSuivantId())));
            }
            if (d.termineeLe() != null) {
                partie.setTermineeLe(Instant.parse(d.termineeLe()));
            }
            partie.setVariante(d.variante());
            persister(partie, compteur);
            parties.put(d.idSauvegarde(), partie.getId());
        });
        lire(zip, FormatSauvegarde.EQUIPES, EquipeDto.class, d -> {
            Equipe equipe = new Equipe(em.getReference(Partie.class, trouver(parties, d.partieId())), d.nom());
            persister(equipe, compteur);
            equipes.put(d.idSauvegarde(), equipe.getId());
        });
        lire(zip, FormatSauvegarde.PARTICIPATIONS, ParticipationDto.class, d -> {
            Participation participation = new Participation(
                    em.getReference(Partie.class, trouver(parties, d.partieId())),
                    em.getReference(Joueur.class, trouver(joueurs, d.joueurId())));
            participation.setScore(d.score());
            participation.setGagnant(d.gagnant());
            if (d.equipeId() != null) {
                participation.setEquipe(em.getReference(Equipe.class, trouver(equipes, d.equipeId())));
            }
            persister(participation, compteur);
        });
        lire(zip, FormatSauvegarde.PRETS, PretDto.class, d -> {
            Pret pret = new Pret(em.getReference(Jeu.class, trouver(jeux, d.jeuId())), d.emprunteur(),
                    LocalDate.parse(d.datePret()));
            if (d.dateRetour() != null) {
                pret.clore(LocalDate.parse(d.dateRetour()));
            }
            persister(pret, compteur);
        });
        em.flush();
        em.clear();
    }

    /** Ordre inverse des dépendances ; les tokens de fin de partie ne sont jamais restaurés. */
    private void supprimerTout() {
        for (Class<?> type : new Class<?>[] { CopieImage.class, TokenFinPartie.class, Participation.class, Equipe.class,
                Partie.class, Pret.class, Jeu.class, Joueur.class }) {
            em.createQuery("delete from " + type.getSimpleName()).executeUpdate();
        }
        em.clear();
    }

    private <T> void lire(ZipFile zip, String fichier, Class<T> type, Consumer<T> action) {
        LectureSauvegarde.lireTableau(zip, fichier, type, UnaryOperator.identity(), action);
    }

    private void persister(Object entite, Compteur compteur) {
        em.persist(entite);
        if (++compteur.valeur % SauvegardeExport.LOT == 0) {
            em.flush();
            em.clear();
        }
    }

    private static Long trouver(Map<Long, Long> correspondance, Long ancien) {
        Long nouveau = correspondance.get(ancien);
        if (nouveau == null) {
            throw new IllegalStateException("Référence de sauvegarde sans correspondance");
        }
        return nouveau;
    }

    private ImageSource image(ImageDto image) {
        if (image == null) {
            return null;
        }
        if (TypeImage.URL.name().equals(image.type())) {
            CopieImage copie = em.find(CopieImage.class, CopieImage.cle(image.reference()));
            if (copie == null) {
                copie = new CopieImage(image.reference());
                em.persist(copie);
            }
            if (image.copie() != null) { copie.disponible(image.copie(), 0); }
        }
        return TypeImage.UPLOAD.name().equals(image.type()) ? ImageSource.upload(image.reference())
                : ImageSource.url(image.reference());
    }

    private static final class Compteur {
        int valeur;
    }
}
