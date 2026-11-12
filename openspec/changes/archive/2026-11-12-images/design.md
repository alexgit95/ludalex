## Context

Trois familles d'images (jeu, avatar de joueur, image de victoire) et un besoin de couleur de fond pour l'écran Affichage. Contraintes : Raspberry Pi (arm64, mémoire limitée), `-Xmx` borné, protection SSRF obligatoire, images uploadées sur volume, images par URL jamais copiées, bibliothèque tierce pour WebP. Précondition : `socle`.

## Goals / Non-Goals

**Goals:**
- Un type `ImageSource` unique réutilisé par jeux et joueurs.
- Calcul de couleur peu coûteux (quelques ms par image, quelques Mo de mémoire).
- Validation SSRF centralisée, réutilisable par tout futur appel sortant.

**Non-Goals:**
- Redimensionnement ou génération de miniatures.
- Cache serveur d'images distantes.
- Géocodage ou tout autre appel sortant (hors périmètre).

## Decisions

- **`ImageSource` embeddable** (`type` UPLOAD|URL, `reference` = nom de fichier ou URL, `couleurFond` portée par l'entité jeu). Colonnes standards `VARCHAR`, compatibles SQLite et PostgreSQL.
- **Nom de fichier généré** (UUID) et chemin résolu puis vérifié contre la racine du volume (`normalize` + `startsWith`) : empêche la traversée de chemin. Le type réel est détecté par signature (octets magiques), pas par extension ni `Content-Type` déclaré.
- **Restitution** par un contrôleur en flux (`ResponseEntity<Resource>`), `Cache-Control: max-age` long puisque le nom change à chaque nouveau fichier.
- **Calcul de couleur** : `ImageReader` avec `setSourceSubsampling` choisi pour viser ≤ 64 px de côté, puis histogramme sur 4 bits par canal (4096 classes) ; pixels alpha faible, quasi blancs et quasi noirs ignorés ; la couleur retenue est le centre de la classe la plus peuplée. Alternative rejetée : moyenne des pixels (résultat terne). Alternative rejetée : canvas navigateur (illisible pour les images distantes sans CORS).
- **WebP** : bibliothèque Java tierce branchée sur ImageIO (SPI) ; critère de choix : fonctionne sur arm64 sans bibliothèque native à compiler, supporte le sous-échantillonnage. Un test d'intégration sur image WebP verrouille ce comportement.
- **SSRF** : service `UrlSortanteValidateur` : schéma http(s) ; résolution DNS ; refus des plages loopback, privées (RFC 1918), link-local, ULA IPv6 et `169.254.169.254` ; connexion directe sur l'adresse validée (évite le DNS rebinding) ; redirections suivies manuellement (5 maximum) avec revalidation à chaque saut ; délai de 5 s ; taille de téléchargement plafonnée (2 Mo, comme les uploads).
- **Repli** : toute exception du calcul est journalisée (sans l'URL complète si elle porte un jeton) et produit la couleur neutre `#3a3a3a`.
- **Upload** : limite 2 Mo configurée dans Spring (`max-file-size`) et revérifiée côté service.

## Risks / Trade-offs

- [Image WebP non décodable sur arm64] → test d'intégration dédié ; repli neutre + modification manuelle.
- [Image distante modifiée après coup, couleur périmée] → acceptable ; couleur modifiable à la main.
- [DNS rebinding] → connexion sur l'adresse déjà validée.
- [Bombe de décompression (petite taille, énormes dimensions)] → lecture des dimensions avant décodage, refus au-delà d'un plafond de pixels.
- [Fichiers orphelins sur le volume après échec d'enregistrement] → écriture avant commit avec suppression sur rollback ; tâche de nettoyage décrite dans `sauvegarde`.

## Open Questions

- Choix exact de la bibliothèque WebP, à valider par un test sur Raspberry Pi.
