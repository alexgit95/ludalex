package com.alexgit95.ludalex.parties;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuRepository;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurRepository;

@Service
@Transactional
public class PartieService {

    public static final int TAILLE_PAGE = 20;
    public static final int LIEUX_MAX = 10;
    public static final int EQUIPES_MAX = 4;

    private final PartieRepository parties;
    private final JeuRepository jeux;
    private final JoueurRepository joueurs;
    private final TokenFinPartieRepository tokens;
    private final Clock horloge;

    public PartieService(PartieRepository parties, JeuRepository jeux, JoueurRepository joueurs,
            TokenFinPartieRepository tokens, Clock horloge) {
        this.parties = parties;
        this.jeux = jeux;
        this.joueurs = joueurs;
        this.tokens = tokens;
        this.horloge = horloge;
    }

    public Partie declarer(PartieSaisie saisie) {
        Partie partie = new Partie(null, LocalDate.now(horloge));
        appliquer(partie, saisie, Set.of());
        return parties.save(partie);
    }

    public Partie modifier(Long id, PartieSaisie saisie) {
        Partie partie = parties.trouverAvecJeu(id).orElseThrow(() -> new PartieIntrouvableException(id));
        if (partie.getStatut() != StatutPartie.TERMINEE) {
            throw new PartieInvalideException("statut", "Seule une partie terminée peut être modifiée ici");
        }
        Set<Long> dejaPresents = partie.getParticipations().stream()
                .map(participation -> participation.getJoueur().getId()).collect(Collectors.toSet());
        appliquer(partie, saisie, dejaPresents);
        return parties.save(partie);
    }

    /** Supprime la partie, ses participations, ses équipes et ses tokens de fin de partie. */
    public void supprimer(Long id) {
        Partie partie = parties.findById(id).orElseThrow(() -> new PartieIntrouvableException(id));
        // La participation référence l'équipe : on la détache pour supprimer les deux dans le bon ordre.
        partie.getParticipations().forEach(participation -> participation.setEquipe(null));
        parties.flush();
        tokens.supprimerParPartie(id);
        parties.delete(partie);
    }

    @Transactional(readOnly = true)
    public Page<LignePartie> lister(Long jeuId, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE,
                Sort.by(Sort.Order.desc("datePartie"), Sort.Order.desc("id")));
        Page<Partie> pageParties = jeuId == null ? parties.findAll(pageable) : parties.findByJeuId(jeuId, pageable);
        List<Long> ids = pageParties.getContent().stream().map(Partie::getId).toList();
        Map<Long, List<Participation>> parPartie = ids.isEmpty() ? Map.of()
                : parties.participationsDesParties(ids).stream()
                        .collect(Collectors.groupingBy(participation -> participation.getPartie().getId()));
        return pageParties.map(partie -> new LignePartie(partie, parPartie.getOrDefault(partie.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public DetailPartie detail(Long id) {
        Partie partie = parties.trouverAvecJeu(id).orElseThrow(() -> new PartieIntrouvableException(id));
        List<Participation> participations = parties.participationsDesParties(List.of(id));
        List<Equipe> equipes = new ArrayList<>(partie.getEquipes());
        return new DetailPartie(partie, participations, equipes);
    }

    /** Lieux déjà saisis commençant par le texte tapé (dix au plus, sans doublon). */
    @Transactional(readOnly = true)
    public List<String> suggererLieux(String saisie) {
        String propre = saisie == null ? "" : saisie.trim().toLowerCase(Locale.ROOT);
        if (propre.isEmpty()) {
            return List.of();
        }
        String motif = propre.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return parties.lieuxCommencantPar(motif, PageRequest.of(0, LIEUX_MAX));
    }

    private void appliquer(Partie partie, PartieSaisie saisie, Set<Long> dejaPresents) {
        Jeu jeu = saisie.jeuId() == null ? null : jeux.findById(saisie.jeuId()).orElse(null);
        if (jeu == null) {
            throw new PartieInvalideException("jeu", "Choisissez un jeu");
        }
        partie.setJeu(jeu);
        partie.setDatePartie(validerDate(saisie.date()));
        partie.setLieu(validerLieu(saisie.lieu()));

        List<Long> ids = valeurs(saisie.joueurIds());
        Map<Long, Joueur> participants = chargerJoueurs(ids, dejaPresents);
        Map<Long, Integer> scores = saisie.scores() == null ? Map.of() : saisie.scores();
        if (scores.entrySet().stream().anyMatch(score -> score.getValue() != null && !ids.contains(score.getKey()))) {
            throw new PartieInvalideException("scores", "Un score concerne un joueur qui ne participe pas");
        }

        Map<Integer, List<Long>> membres = grouperEquipes(saisie, ids);
        Set<Long> gagnants = determinerGagnants(partie, saisie, jeu, ids, membres);

        remplacerEquipesEtParticipations(partie, saisie.equipeDe(), saisie.equipeNoms(), ids, participants, scores,
                gagnants, membres);
    }

    /** Crée (sans la clore) une partie en direct : joueurs actifs, équipes facultatives, aucun résultat. */
    Partie creerEnCours(LancementSaisie lancement) {
        Jeu jeu = lancement.jeuId() == null ? null : jeux.findById(lancement.jeuId()).orElse(null);
        if (jeu == null) {
            throw new PartieInvalideException("jeu", "Choisissez un jeu");
        }
        Partie partie = new Partie(jeu, LocalDate.now(horloge));
        partie.setStatut(StatutPartie.EN_COURS);
        List<Long> ids = valeurs(lancement.joueurIds());
        Map<Long, Joueur> participants = chargerJoueurs(ids, Set.of());
        Map<Integer, List<Long>> membres = grouperEquipes(lancement.equipeDe(), lancement.equipeNoms(), ids);
        remplacerEquipesEtParticipations(partie, lancement.equipeDe(), lancement.equipeNoms(), ids, participants,
                Map.of(), Set.of(), membres);
        return parties.save(partie);
    }

    /** Termine une partie en direct : gagnants, scores facultatifs, horodatage de fin. */
    void cloturer(Partie partie, FinSaisie fin) {
        List<Participation> participations = partie.getParticipations();
        List<Long> ids = participations.stream().map(participation -> participation.getJoueur().getId()).toList();
        Map<Long, Integer> scores = fin.scores() == null ? Map.of() : fin.scores();
        if (scores.entrySet().stream().anyMatch(score -> score.getValue() != null && !ids.contains(score.getKey()))) {
            throw new PartieInvalideException("scores", "Un score concerne un joueur qui ne participe pas");
        }
        Set<Long> viaEquipes = new LinkedHashSet<>();
        for (Long equipeId : valeurs(fin.equipesGagnantes())) {
            boolean trouvee = false;
            for (Participation participation : participations) {
                if (participation.getEquipe() != null && equipeId.equals(participation.getEquipe().getId())) {
                    viaEquipes.add(participation.getJoueur().getId());
                    trouvee = true;
                }
            }
            if (!trouvee) {
                throw new PartieInvalideException("gagnants", "Cette équipe ne fait pas partie de la partie");
            }
        }
        Set<Long> gagnants = gagnantsDe(partie, partie.getJeu(), ids, valeurs(fin.gagnantIds()), viaEquipes,
                fin.resultat());
        for (Participation participation : participations) {
            Long joueurId = participation.getJoueur().getId();
            participation.setScore(scores.get(joueurId));
            participation.setGagnant(gagnants.contains(joueurId));
        }
        partie.setStatut(StatutPartie.TERMINEE);
        partie.setTermineeLe(Instant.now(horloge));
        partie.setJoueurSuivant(null);
        parties.save(partie);
    }

    private LocalDate validerDate(LocalDate date) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        if (date == null) {
            return aujourdhui;
        }
        if (date.isAfter(aujourdhui)) {
            throw new PartieInvalideException("date", "La date de la partie ne peut pas être dans le futur");
        }
        return date;
    }

    private String validerLieu(String lieu) {
        if (!StringUtils.hasText(lieu)) {
            return null;
        }
        String propre = lieu.trim();
        if (propre.length() > Partie.LIEU_MAX) {
            throw new PartieInvalideException("lieu", "Le lieu ne doit pas dépasser " + Partie.LIEU_MAX + " caractères");
        }
        return propre;
    }

    private Map<Long, Joueur> chargerJoueurs(List<Long> ids, Set<Long> dejaPresents) {
        if (ids.isEmpty()) {
            throw new PartieInvalideException("joueurs", "Sélectionnez au moins un joueur");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new PartieInvalideException("joueurs", "Un joueur ne peut participer qu'une fois à la partie");
        }
        Map<Long, Joueur> parId = joueurs.findAllById(ids).stream()
                .collect(Collectors.toMap(Joueur::getId, Function.identity()));
        for (Long id : ids) {
            Joueur joueur = parId.get(id);
            if (joueur == null || (joueur.isAnonymise() && !dejaPresents.contains(id))) {
                throw new PartieInvalideException("joueurs", "Joueur indisponible pour cette partie");
            }
        }
        return parId;
    }

    /** Numéro d'équipe vers membres ; refuse une équipe nommée sans membre. */
    private Map<Integer, List<Long>> grouperEquipes(PartieSaisie saisie, List<Long> ids) {
        return grouperEquipes(saisie.equipeDe(), saisie.equipeNoms(), ids);
    }

    private Map<Integer, List<Long>> grouperEquipes(Map<Long, Integer> equipeDeSaisi,
            Map<Integer, String> nomsSaisis, List<Long> ids) {
        Map<Integer, List<Long>> membres = new TreeMap<>();
        Map<Long, Integer> equipeDe = equipeDeSaisi == null ? Map.of() : equipeDeSaisi;
        for (Map.Entry<Long, Integer> entree : equipeDe.entrySet()) {
            Integer numero = entree.getValue();
            if (numero == null) {
                continue;
            }
            if (numero < 1 || numero > EQUIPES_MAX) {
                throw new PartieInvalideException("equipes", "Numéro d'équipe invalide");
            }
            if (!ids.contains(entree.getKey())) {
                throw new PartieInvalideException("equipes", "Une équipe contient un joueur qui ne participe pas");
            }
            membres.computeIfAbsent(numero, n -> new ArrayList<>()).add(entree.getKey());
        }
        Map<Integer, String> noms = nomsSaisis == null ? Map.of() : nomsSaisis;
        for (Map.Entry<Integer, String> nom : noms.entrySet()) {
            if (StringUtils.hasText(nom.getValue()) && !membres.containsKey(nom.getKey())) {
                throw new PartieInvalideException("equipes", "L'équipe " + nom.getKey() + " n'a aucun membre");
            }
            if (nom.getValue() != null && nom.getValue().trim().length() > Equipe.NOM_MAX) {
                throw new PartieInvalideException("equipes",
                        "Le nom d'une équipe ne doit pas dépasser " + Equipe.NOM_MAX + " caractères");
            }
        }
        return membres;
    }

    private Set<Long> determinerGagnants(Partie partie, PartieSaisie saisie, Jeu jeu, List<Long> ids,
            Map<Integer, List<Long>> membres) {
        Set<Long> viaEquipes = new LinkedHashSet<>();
        if (!jeu.isCooperatif()) {
            for (Integer numero : valeurs(saisie.equipesGagnantes())) {
                List<Long> equipe = membres.get(numero);
                if (equipe == null) {
                    throw new PartieInvalideException("gagnants", "L'équipe gagnante " + numero + " n'a aucun membre");
                }
                viaEquipes.addAll(equipe);
            }
        }
        return gagnantsDe(partie, jeu, ids, valeurs(saisie.gagnantIds()), viaEquipes, saisie.resultat());
    }

    /** Règles communes aux parties déclarées et aux parties en direct. */
    private Set<Long> gagnantsDe(Partie partie, Jeu jeu, List<Long> ids, List<Long> gagnantIds, Set<Long> viaEquipes,
            ResultatCooperatif resultat) {
        if (jeu.isCooperatif()) {
            if (resultat == null) {
                throw new PartieInvalideException("resultat", "Indiquez si la partie est gagnée ou perdue");
            }
            partie.setResultatCooperatif(resultat);
            return resultat == ResultatCooperatif.VICTOIRE ? new LinkedHashSet<>(ids) : Set.of();
        }
        partie.setResultatCooperatif(null);
        Set<Long> gagnants = new LinkedHashSet<>(gagnantIds);
        gagnants.addAll(viaEquipes);
        if (gagnants.isEmpty()) {
            throw new PartieInvalideException("gagnants", "Désignez au moins un gagnant");
        }
        if (!ids.containsAll(gagnants)) {
            throw new PartieInvalideException("gagnants", "Un gagnant ne participe pas à la partie");
        }
        return gagnants;
    }

    /** Met à jour les participations en place (contrainte d'unicité) puis recrée les équipes. */
    private void remplacerEquipesEtParticipations(Partie partie, Map<Long, Integer> equipeDeSaisi,
            Map<Integer, String> nomsSaisis, List<Long> ids,
            Map<Long, Joueur> participants, Map<Long, Integer> scores, Set<Long> gagnants,
            Map<Integer, List<Long>> membres) {
        Map<Integer, Equipe> equipes = new TreeMap<>();
        Map<Integer, String> noms = nomsSaisis == null ? Map.of() : nomsSaisis;
        partie.getEquipes().clear();
        for (Integer numero : membres.keySet()) {
            String nom = StringUtils.hasText(noms.get(numero)) ? noms.get(numero).trim() : "Équipe " + numero;
            Equipe equipe = new Equipe(partie, nom);
            equipes.put(numero, equipe);
            partie.getEquipes().add(equipe);
        }

        Map<Long, Participation> existantes = partie.getParticipations().stream()
                .collect(Collectors.toMap(participation -> participation.getJoueur().getId(), Function.identity()));
        partie.getParticipations().removeIf(participation -> !ids.contains(participation.getJoueur().getId()));
        Map<Long, Integer> equipeDe = equipeDeSaisi == null ? Map.of() : equipeDeSaisi;
        for (Long id : ids) {
            Participation participation = existantes.get(id);
            if (participation == null) {
                participation = new Participation(partie, participants.get(id));
                partie.getParticipations().add(participation);
            }
            participation.setScore(scores.get(id));
            participation.setGagnant(gagnants.contains(id));
            participation.setEquipe(equipeDe.get(id) == null ? null : equipes.get(equipeDe.get(id)));
        }
    }

    private static <T> List<T> valeurs(Collection<T> valeurs) {
        return valeurs == null ? List.of() : valeurs.stream().filter(valeur -> valeur != null).toList();
    }
}
