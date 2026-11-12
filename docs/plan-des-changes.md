# Plan des changes OpenSpec

Ordre d'exécution : chaque change suppose les précédents terminés. Un change = une version du `pom.xml` et du `CHANGELOG.md`.

| # | Change | Contenu | Dépend de |
|---|---|---|---|
| 1 | `socle` | Projet Spring Boot, profils `dev`/`prod`, sécurité (Cloudflare Access en `prod`, libre en `dev`, CSRF), exception `/fin/**`, Thymeleaf + HTMX, PWA minimale, Dockerfile (`-Xmx`, headless), `README.md`, `CHANGELOG.md`, `docs/database.puml` | - |
| 2 | `images` | Stockage sur volume, images par URL, protection SSRF, bibliothèque WebP, calcul de la couleur de fond (sous-échantillonnage), repli manuel | 1 |
| 3 | `joueurs` | Fiches joueurs (nom, avatar, image de victoire), écran d'administration | 2 |
| 4 | `ludotheque` | Jeux (nom, image, prix, date d'achat, coopératif), liste, détail, prêt (à qui) | 2 |
| 5 | `parties` | Saisie d'une partie (joueurs, date, lieu libre avec autocomplétion des lieux déjà saisis, gagnant, scores), équipes, jeux coopératifs | 3, 4 |
| 6 | `statistiques` | Champion par jeu (égalité : tous les avatars), taux de victoire, meilleur joueur, dernières parties ; icône champion dans la liste des jeux ; requêtes agrégées | 5 |
| 7 | `partie-en-cours` | Lancement, pause (joueur qui devait jouer), reprise, token à usage unique, page `/fin/{token}` | 5 |
| 8 | `affichage` | Écran Affichage (image, couleur de fond, avatars, QR code), mise à jour en direct, animation de victoire, diaporama celtique/médiéval | 2, 7 |
| 9 | `sauvegarde` | Export ZIP unique (données + images), restauration complète par réupload sans modification, contrôles du ZIP | 2 à 8 |
| 10 | `import-myludo` (v2) | Import des statistiques depuis MyLudo | 5 |

Les changes 3 et 4 sont indépendants et peuvent être faits dans n'importe quel ordre. Le change 6 peut être fait avant ou après le 7.

Le change 9 vient en dernier parce que le format du ZIP doit couvrir toutes les entités ; il impose en revanche des données indépendantes du moteur (SQLite / PostgreSQL) dès le change 1.

## Décisions

| Sujet | Décision | Change |
|---|---|---|
| Diaporama | Usage personnel, licence sans importance ; images embarquées dans l'application | 8 |
| Session Cloudflare | 1 mois (réglage côté Cloudflare) ; l'écran Affichage se reconnecte à l'expiration | 8 |
| Durée du token | 7 jours, usage unique | 7 |
| Prêt | Historique conservé ; emprunteur en texte libre | 4 |
| Gagnant | Plusieurs gagnants possibles | 5 |
| Scores | Facultatifs, y compris via le QR (la page `/fin/{token}` permet de les saisir en option) | 5, 7 |
| Parties simultanées | Une seule partie en cours à la fois ; lancer une seconde partie impose de terminer d'abord la précédente en renseignant les champs de fin de partie (gagnants, scores facultatifs) | 7 |
| Suppression d'un joueur | Anonymisation : ses parties sont conservées, son nom et ses images sont effacés | 3 |
| Statistiques | Pas de minimum de parties ; jeux coopératifs et compétitifs regroupés ; égalité : tous les avatars des premiers | 6 |
| Jeux coopératifs | Une victoire compte pour chaque participant | 5, 6 |
| Upload | 2 Mo maximum ; formats standards (JPEG, PNG, GIF, WebP) | 2 |
| Format du ZIP | Numéro de version du format inclus dans le ZIP | 9 |
| Lieu | Texte libre, avec autocomplétion des lieux déjà saisis ; pas de géocodage | 5 |

Il ne reste aucun point ouvert.
