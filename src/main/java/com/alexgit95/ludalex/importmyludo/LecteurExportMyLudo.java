package com.alexgit95.ludalex.importmyludo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Component;

import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.JeuMyLudo;
import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.JoueurMyLudo;
import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.PartieMyLudo;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecteurs des deux exports MyLudo (tableaux JSON, un objet par jeu ou par partie), lus en flux. Le format est
 * isolé ici pour absorber une évolution de l'export.
 */
@Component
public class LecteurExportMyLudo {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    List<JeuMyLudo> lireCollection(byte[] contenu, String fichier) {
        List<JeuMyLudo> jeux = new ArrayList<>();
        lireTableau(contenu, fichier, (numero, objet) -> {
            if (objet.has("Joueurs")) {
                throw new ImportMyLudoException(fichier, numero, null, "Joueurs",
                        "ce fichier ressemble à un export de parties : déposez-le dans la zone « Parties »");
            }
            String titre = texteObligatoire(objet, "Titre", Jeu.NOM_MAX, fichier, numero, null);
            String contexte = titre;
            BigDecimal prix = decimal(objet.get("Prix indicatif"), fichier, numero, contexte, "Prix indicatif");
            LocalDate date = date(objet.get("Date d'acquisition"), fichier, numero, contexte,
                    "Date d'acquisition", false);
            boolean cooperatif = false;
            JsonNode mecanismes = objet.get("Mécanisme(s)");
            if (mecanismes != null && mecanismes.isArray()) {
                for (JsonNode mecanisme : mecanismes.values()) {
                    cooperatif |= "Coopération".equals(mecanisme.asString(""));
                }
            }
            jeux.add(new JeuMyLudo(numero, titre, prix, date, cooperatif));
        });
        return jeux;
    }

    List<PartieMyLudo> lireParties(byte[] contenu, String fichier) {
        List<PartieMyLudo> parties = new ArrayList<>();
        lireTableau(contenu, fichier, (numero, objet) -> {
            if (!objet.has("Joueurs") && objet.has("Date d'acquisition")) {
                throw new ImportMyLudoException(fichier, numero, null, "Date d'acquisition",
                        "ce fichier ressemble à un export de collection : déposez-le dans la zone « Collection »");
            }
            String titre = texteObligatoire(objet, "Titre", Jeu.NOM_MAX, fichier, numero, null);
            LocalDate date = date(objet.get("Date"), fichier, numero, titre, "Date", true);
            String contexte = titre + ", " + date;
            String lieu = objet.has("Lieu") ? objet.get("Lieu").asString("").trim() : "";
            JsonNode liste = objet.get("Joueurs");
            if (liste == null || !liste.isArray()) {
                throw new ImportMyLudoException(fichier, numero, contexte, "Joueurs", "liste des joueurs absente");
            }
            List<JoueurMyLudo> joueurs = new ArrayList<>();
            for (JsonNode joueur : liste.values()) {
                if (!joueur.isObject()) {
                    throw new ImportMyLudoException(fichier, numero, contexte, "Joueurs",
                            "un joueur n'est pas un objet");
                }
                String nom = texteObligatoire(joueur, "Nom", Joueur.NOM_MAX, fichier, numero, contexte);
                Integer score = entier(joueur.get("Score"), fichier, numero, contexte, "Score");
                boolean victoire = joueur.has("Victoire") && joueur.get("Victoire").asBoolean(false);
                String equipe = joueur.has("Équipe") ? joueur.get("Équipe").asString("").trim() : "";
                joueurs.add(new JoueurMyLudo(nom, score, victoire, equipe.isEmpty() ? null : equipe));
            }
            parties.add(new PartieMyLudo(numero, titre, date, lieu, joueurs, ouiNon(objet, "Coop"),
                    ouiNon(objet, "Coop Victoire"), ouiNon(objet, "Exclue")));
        });
        return parties;
    }

    private void lireTableau(byte[] contenu, String fichier, BiConsumer<Integer, JsonNode> action) {
        try (InputStream flux = new ByteArrayInputStream(contenu);
                JsonParser lecteur = MAPPER.createParser(flux)) {
            if (lecteur.nextToken() != JsonToken.START_ARRAY) {
                throw new ImportMyLudoException(fichier, null, null, null,
                        "le fichier doit contenir un tableau JSON d'enregistrements");
            }
            int numero = 0;
            JsonToken jeton;
            while ((jeton = lecteur.nextToken()) != JsonToken.END_ARRAY) {
                numero++;
                if (jeton != JsonToken.START_OBJECT) {
                    throw new ImportMyLudoException(fichier, numero, null, null, "l'entrée n'est pas un objet JSON");
                }
                action.accept(numero, MAPPER.readTree(lecteur));
            }
        } catch (JacksonException e) {
            throw new ImportMyLudoException(fichier, null, null, null, "JSON mal formé",
                    e.getLocation() == null ? null : e.getLocation().getLineNr(),
                    e.getLocation() == null ? null : e.getLocation().getColumnNr());
        } catch (IOException e) {
            throw new ImportMyLudoException(fichier, null, null, null, "fichier illisible");
        }
    }

    private static String texteObligatoire(JsonNode objet, String champ, int max, String fichier, int numero,
            String contexte) {
        JsonNode valeur = objet.get(champ);
        String texte = valeur == null || !valeur.isString() ? "" : valeur.asString("").trim();
        if (texte.isEmpty()) {
            throw new ImportMyLudoException(fichier, numero, contexte, champ, "valeur absente");
        }
        if (texte.length() > max) {
            throw new ImportMyLudoException(fichier, numero, contexte, champ,
                    "valeur trop longue (" + max + " caractères au maximum)");
        }
        return texte;
    }

    private static LocalDate date(JsonNode valeur, String fichier, int numero, String contexte, String champ,
            boolean obligatoire) {
        String texte = valeur == null || valeur.isNull() ? "" : valeur.asString("").trim();
        if (texte.isEmpty()) {
            if (obligatoire) {
                throw new ImportMyLudoException(fichier, numero, contexte, champ, "date absente");
            }
            return null;
        }
        try {
            return LocalDate.parse(texte);
        } catch (DateTimeParseException e) {
            throw new ImportMyLudoException(fichier, numero, contexte, champ,
                    "date illisible « " + texte + " » (format attendu AAAA-MM-JJ)");
        }
    }

    private static BigDecimal decimal(JsonNode valeur, String fichier, int numero, String contexte, String champ) {
        if (valeur == null || valeur.isNull() || (valeur.isString() && valeur.asString("").isBlank())) {
            return null;
        }
        if (!valeur.isNumber()) {
            throw new ImportMyLudoException(fichier, numero, contexte, champ, "nombre attendu");
        }
        BigDecimal nombre = valeur.decimalValue();
        return nombre.signum() <= 0 ? null : nombre;
    }

    private static Integer entier(JsonNode valeur, String fichier, int numero, String contexte, String champ) {
        if (valeur == null || valeur.isNull()) {
            return null;
        }
        if (!valeur.isNumber() || !valeur.canConvertToInt() || !valeur.isIntegralNumber()) {
            throw new ImportMyLudoException(fichier, numero, contexte, champ, "nombre entier attendu");
        }
        return valeur.intValue();
    }

    private static boolean ouiNon(JsonNode objet, String champ) {
        JsonNode valeur = objet.get(champ);
        return valeur != null && "oui".equalsIgnoreCase(valeur.asString("").trim());
    }
}
