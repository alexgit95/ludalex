package com.alexgit95.ludalex.sauvegarde;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

/** Fabrique d'archives de sauvegarde pour les tests, avec des identifiants d'origine volontairement quelconques. */
final class ArchiveDeTest {

    static final String IMAGE = "11111111-1111-4111-8111-111111111111.png";

    static final String JOUEURS = "[{\"idSauvegarde\":9001,\"nom\":\"Alice\","
            + "\"avatar\":{\"type\":\"UPLOAD\",\"reference\":\"" + IMAGE + "\"},\"imageVictoire\":null,"
            + "\"anonymise\":false},"
            + "{\"idSauvegarde\":9002,\"nom\":\"Bruno\","
            + "\"avatar\":{\"type\":\"URL\",\"reference\":\"https://exemple.org/b.png\"},\"imageVictoire\":null,"
            + "\"anonymise\":false}]";
    static final String JEUX = "[{\"idSauvegarde\":7001,\"nom\":\"Catan\",\"image\":null,\"couleurFond\":\"#112233\","
            + "\"prix\":\"39.90\",\"dateAchat\":\"2024-05-01\",\"cooperatif\":false}]";
    static final String PARTIES = "[{\"idSauvegarde\":5001,\"jeuId\":7001,\"datePartie\":\"2025-01-02\","
            + "\"lieu\":\"Salon\",\"statut\":\"TERMINEE\",\"resultatCooperatif\":null,\"joueurSuivantId\":null,"
            + "\"termineeLe\":null}]";
    static final String EQUIPES = "[{\"idSauvegarde\":4001,\"partieId\":5001,\"nom\":\"Rouge\"}]";
    static final String PARTICIPATIONS = "[{\"idSauvegarde\":3001,\"partieId\":5001,\"joueurId\":9001,\"score\":10,"
            + "\"gagnant\":true,\"equipeId\":4001},{\"idSauvegarde\":3002,\"partieId\":5001,\"joueurId\":9002,"
            + "\"score\":null,\"gagnant\":false,\"equipeId\":null}]";
    static final String PRETS = "[{\"idSauvegarde\":2001,\"jeuId\":7001,\"emprunteur\":\"Voisin\","
            + "\"datePret\":\"2025-02-01\",\"dateRetour\":null}]";
    static final String MANIFESTE = "{\"formatVersion\":1,\"appVersion\":\"test\",\"date\":\"2026-11-12T10:00:00\","
            + "\"compteurs\":{\"joueurs\":2,\"jeux\":1,\"parties\":1,\"equipes\":1,\"participations\":2,"
            + "\"prets\":1,\"images\":1}}";

    private final Map<String, byte[]> entrees = new LinkedHashMap<>();

    private ArchiveDeTest() {
    }

    static ArchiveDeTest valide() {
        ArchiveDeTest archive = new ArchiveDeTest();
        archive.avec("donnees/joueurs.json", JOUEURS).avec("donnees/jeux.json", JEUX)
                .avec("donnees/parties.json", PARTIES).avec("donnees/equipes.json", EQUIPES)
                .avec("donnees/participations.json", PARTICIPATIONS).avec("donnees/prets.json", PRETS)
                .avec("images/" + IMAGE, png()).avec("manifest.json", MANIFESTE);
        return archive;
    }

    static ArchiveDeTest vide() {
        return new ArchiveDeTest();
    }

    ArchiveDeTest avec(String nom, String contenu) {
        return avec(nom, contenu.getBytes(StandardCharsets.UTF_8));
    }

    ArchiveDeTest avec(String nom, byte[] contenu) {
        entrees.put(nom, contenu);
        return this;
    }

    ArchiveDeTest sans(String nom) {
        entrees.remove(nom);
        return this;
    }

    /** Remplace un fragment du contenu texte d'une entrée existante. */
    ArchiveDeTest modifier(String nom, String ancien, String nouveau) {
        String texte = new String(entrees.get(nom), StandardCharsets.UTF_8);
        if (!texte.contains(ancien)) {
            throw new IllegalArgumentException("Fragment absent de " + nom + " : " + ancien);
        }
        return avec(nom, texte.replace(ancien, nouveau));
    }

    byte[] octets() {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(sortie)) {
            for (Map.Entry<String, byte[]> entree : entrees.entrySet()) {
                zip.putNextEntry(new ZipEntry(entree.getKey()));
                zip.write(entree.getValue());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }

    Path ecrire(Path dossier) {
        try {
            return Files.write(Files.createTempFile(dossier, "archive-", ".zip"), octets());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static byte[] png() {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", sortie);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }

    /** Signature PNG suivie de zéros : passe le contrôle de format, sert à tester les tailles. */
    static byte[] pngRemplie(int octets) {
        byte[] contenu = new byte[octets];
        byte[] signature = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
        System.arraycopy(signature, 0, contenu, 0, signature.length);
        return contenu;
    }
}
