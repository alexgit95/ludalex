# Changelog

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/). La version est identique dans le `pom.xml`.

## [0.13.3] - 2026-11-13

### Added
- Page Admin : affichage de la version du POM et des 7 premiers caractères du SHA de commit lorsqu'il est disponible; en exécution locale sans SHA, la version seule est affichée.

## [0.13.2] - 2026-11-13

### Changed
- La route `/` ouvre directement la ludothèque en redirigeant vers `/jeux`; le lancement PWA et le logo continuent d'utiliser `/`.

### Fixed
- Ludothèque : le titre, le bouton « Ajouter un jeu » et le message d'absence de jeux sont alignés sur le bord gauche de la grille centrée, sur petits et grands écrans.

## [0.13.0] - 2026-11-13

### Added
- Écran Affichage, partie en cours : cartouche « VS » (épées croisées en SVG, pulsation de 4 % sur 2,8 s, désactivée avec `prefers-reduced-motion`) ; duel, VS entre chaque paire à partir de trois joueurs, colonnes d'équipes nommées séparées par le VS, libellé « Ensemble » pour un jeu coopératif, aucun VS pour un joueur seul.
- `PresentationJoueurs` (mode et groupes d'affichage) exposé par `EtatAffichage.presentation()`, avec tests unitaires.

### Changed
- Écran Affichage : l'image du jeu devient le fond (floutée, assombrie) sur la couleur de fond du jeu, aussi pendant la pause ; le titre du jeu reste affiché en haut ; QR code réduit en coin.
- Cache du service worker `ludalex-statique-0.13.0`.

### Fixed
- Écran Affichage : les avatars des joueurs n'étaient plus rognés en bas de l'écran et sont nettement plus grands (taille selon la hauteur de l'écran, grille `minmax(0, 1fr)`).

## [0.12.2] - 2026-11-12

### Added
- Healthcheck de l'application dans `docker-compose.portainer.yml` : requête HTTP locale sur le port 8181 via bash (`/dev/tcp`, sans curl), toutes les 30 s, 120 s de démarrage toléré ; aucun changement de code.

## [0.12.1] - 2026-11-12

### Added
- `docker-compose.portainer.yml` : stack prête à déployer dans Portainer ou Arcane (image Docker Hub arm64, PostgreSQL, volumes, variables obligatoires contrôlées) ; aucun changement de code ni de pipeline.

## [0.12.0] - 2026-11-12

### Added
- Menu burger sur petit écran (icône en trois lianes, panneau parchemin sous l'en-tête, `aria-expanded`, fermeture au clic extérieur et sur Échap) ; sans JavaScript la navigation reste visible.
- Aperçu (image et nom) du jeu choisi dans le formulaire de déclaration d'une partie, mis à jour par le fragment HTMX existant (`hx-swap-oob`).
- Décor SVG de coupe (`static/decor/coupe.svg`).

### Changed
- Ludothèque : le champion passe sous le titre, sur un petit parchemin avec coupe, avatar et nom (tous les champions à égalité, à la ligne), hauteur réservée pour l'alignement.
- Ludothèque : grille limitée à 2 colonnes puis 3 dès 40 rem, centrée (largeur 40 rem).
- `PartieController` expose le jeu sélectionné au formulaire et au fragment de résultat ; cache du service worker `ludalex-statique-0.12.0`.
- Tests : aperçu du jeu, ligne champion réservée, burger accessible, grille, décors.

## [0.11.0] - 2026-11-13

### Added
- Charte graphique « heroic fantasy » sobre : palette crème / parchemin en variables CSS, lierres SVG très discrets (`static/decor/`), titres en serif, texte lisible, focus visible doré.
- `CharteGraphiqueTest` : contrastes WCAG AA des couples texte/fond, absence de ressource externe et de couleur en dur hors variables, présence des décors.

### Changed
- `ludalex.css` et `affichage.css` entièrement restylés (en-tête, navigation, formulaires, boutons, cartes, tableaux, `details`, progression, pages d'erreur, page publique de fin de partie, Affichage avec cartouches parchemin lisibles sur toute couleur de jeu) ; aucun changement fonctionnel.
- Couleurs de thème PWA (`theme-color`, manifeste) alignées ; cache du service worker `ludalex-statique-0.11.0` incluant `/decor/`.
- Les couleurs de fond des jeux servent de cadre ; le texte repose sur un cartouche clair.

## [0.10.0] - 2026-11-12

### Added
- Page Admin **Import MyLudo** (`/admin/import-myludo`, JWT Cloudflare exigé en `prod`, CSRF) : envoi des exports JSON de la collection et/ou des parties, analyse à blanc, correspondance des joueurs, confirmation, import.
- Import du **delta seul** : une partie du fichier est déjà présente si une partie en base du même jeu et du même jour partage au moins un joueur avec elle, chaque partie en base n'en couvrant qu'une (appariement exact d'abord, puis meilleur recouvrement). Les manches identiques d'un même jour sont conservées, un réimport n'ajoute rien, les parties existantes ne sont jamais modifiées.
- Jeux : créés depuis la collection (nom, prix, date d'acquisition) ou depuis les parties ; jeu existant non modifié hormis prix et date d'achat vides ; caractère coopératif déduit des parties puis du mécanisme « Coopération ». Joueurs : rapprochement par nom normalisé, choix modifiable, doublons de nom d'une partie numérotés (« Anonyme », « Anonyme 2 »).
- Conversion des parties : scores, gagnants, équipes (4 maximum), résultat victoire/défaite des jeux coopératifs, lieu ; parties ignorées avec leur raison (exclue, future, sans joueur, sans gagnant en jeu non coopératif) ; avertissements (lieu tronqué, équipes ignorées, prix ou date d'achat invalides).
- Barre de progression par phases avec compteur et élément en cours, suivie par interrogation HTMX toutes les secondes (204 tant que l'état est à jour) ; une seule analyse ou un seul import à la fois, sur un fil dédié.
- Rapport de réussite (compteurs, ignorées par raison, avertissements) ou d'erreur diagnostiquable (fichier, entrée, jeu et date, champ, cause, ligne et colonne d'un JSON mal formé) ; message générique et journalisation pour une erreur inattendue ; contenu des fichiers toujours échappé à l'affichage.
- Import atomique (une transaction, lots de 500) : le plan est recalculé sur l'état réel de la base juste avant l'écriture ; aucune modification de schéma ni du format de sauvegarde.
- Réglages `ludalex.import-myludo.duree-session` (30m) et `ludalex.import-myludo.lignes-max` (200). Durée, commentaire, notes, images et métadonnées MyLudo ne sont pas importés.
- Tests : lecteurs, appariement, import sur base réelle avec les exports d'exemple (manches multiples, coopératif, équipes, « Anonyme », deuxième import, export plus récent, existants intacts, échec annulé), orchestration (verrou, expiration, erreurs), contrôleur et rendu (progression, plan, rapport échappé, erreur), JWT exigé en `prod`.

### Changed
- Page Admin : lien vers l'import ; styles de progression et de détails dans `ludalex.css`.
- Aucun changement de schéma de base de données.

## [0.9.0] - 2026-11-12

### Added
- Page Admin **Sauvegarde et restauration** (`/admin/sauvegarde`, JWT Cloudflare exigé en `prod`).
- Export : un seul ZIP `ludalex-sauvegarde-<date>.zip` écrit en flux dans la réponse HTTP (lecture par lots de 500 sans entités gérées, images copiées par flux). Format v1 : `donnees/*.json` (joueurs, jeux, parties, équipes, participations, prêts) indépendant du moteur, `images/*` pour les images uploadées réellement présentes (une image manquante est exportée comme absente), `manifest.json` (version du format, version applicative, date, compteurs). Tokens de fin de partie exclus ; images par URL conservées comme simple adresse.
- Restauration par réupload du ZIP sans modification, y compris d'un moteur à l'autre (SQLite ↔ PostgreSQL) : nouveaux identifiants générés avec table de correspondance, sans dépendre des séquences ni auto-incréments.
- Protection de la restauration : mot de confirmation `RESTAURER` (en-tête), jeton CSRF, ZIP reçu en corps brut puis écrit dans un fichier temporaire plafonné (`ludalex.sauvegarde.taille-max`, 512MB), supprimé en toutes circonstances, une seule restauration à la fois (409 sinon), contrôle de l'espace disque.
- Validation complète avant toute suppression : chemins dangereux (zip slip), entrées hors liste, nombre d'entrées, taille et taux de décompression mesurés à la lecture, images de format réel autorisé et de 2 Mo maximum, noms d'images générés par l'application, version de format, valeurs des enregistrements, adresses d'image http(s), références entre fichiers et compteurs du manifeste.
- Application atomique : suppression et insertion par lots dans une seule transaction (rollback complet en cas d'échec) ; images préparées dans un répertoire caché du volume puis installées après le commit, images orphelines supprimées, restes d'une restauration interrompue nettoyés au démarrage.
- Réglages `ludalex.sauvegarde.taille-max`, `taille-decompressee-max` (1GB), `entrees-max` (10000), `ratio-max` (200) et `repertoire-temporaire`.
- Tests : validation (51 cas dont zip slip, bombe de décompression, images non conformes, version inconnue, références incohérentes, ZIP corrompu), orchestration (nettoyage, verrou, taille), aller-retour sur base réelle (relations, images, tokens, orphelines, 1 100 enregistrements par lots, archive aux identifiants quelconques, atomicité), exhaustivité des entités exportées, contrôleur (CSRF, confirmation, 400/413) et JWT exigé en `prod`.

### Changed
- `StockageImages` expose le volume (`racine()`) et la validation d'un nom de fichier (`estNomValide`).
- Aucun changement de schéma de base de données.

## [0.8.0] - 2026-11-12

### Added
- Écran Affichage (`/affichage`, JWT Cloudflare exigé en `prod`) : partie en cours (image et couleur de fond du jeu, avatars, QR code), pause (joueur suivant en grand, sans QR), victoire animée (15 s, défaite coopérative incluse) et veille.
- Mise à jour par interrogation HTMX toutes les 3 s de `/affichage/etat` : réponse 204 tant que l'état (signature) n'a pas changé, fragment sinon ; rechargement complet après trois échecs (session Cloudflare expirée) et demande de maintien d'écran allumé.
- QR code de fin de partie généré côté serveur en SVG avec ZXing `core` (nouvelle dépendance Java pure, compatible arm64). Le token en clair n'est jamais stocké en base : gardé en mémoire, il est régénéré à chaque ouverture de la page (l'ancien est invalidé) et renouvelé lorsqu'il expire dans moins de 24 h.
- Origine publique du QR déduite de la requête (`X-Forwarded-*`) ou imposée par `ludalex.affichage.url-publique` ; réglages `ludalex.affichage.duree-victoire` (15s) et `ludalex.affichage.renouvellement-token` (24h).
- Diaporama de veille : fondu à deux couches avec une seule image préchargée, images de `static/diaporama/` énumérées au démarrage, quatre illustrations SVG embarquées, fond neutre LUDALEX si le dossier est vide.
- Tests : QR (décodage réel, fidélité du SVG), origine publique, renouvellement du token, états de l'écran (horloge figée), parcours web sur base réelle (QR, pause, victoire, défaite, 204), images du diaporama, JWT exigé en `prod`.

### Changed
- `docs/fin-de-partie-qr.md` : comportement du QR côté écran Affichage et nouveaux réglages.
- Aucun changement de schéma de base de données.

## [0.7.0] - 2026-11-12

### Added
- Parties en direct (`/parties/direct`) : lancement (jeu, joueurs actifs, équipes facultatives), pause avec le joueur à qui c'était de jouer (obligatoire et participant), reprise, fin avec gagnants (plusieurs possibles), scores facultatifs ou résultat victoire/défaite pour un jeu coopératif.
- Une seule partie active (en cours ou en pause) à la fois : lancer une seconde partie impose de terminer d'abord la précédente en renseignant sa fin ; la fin et le lancement sont atomiques (si le lancement est invalide, la partie précédente reste active). Lancements simultanés sérialisés (l'application n'a qu'une instance).
- Lien public de fin de partie `/fin/{token}`, accessible sans JWT Cloudflare : terminer la partie (gagnants, scores facultatifs, résultat coopératif) ou la mettre en pause. Le token est la seule autorisation : 256 bits aléatoires, seule l'empreinte SHA-256 est stockée, rattaché à une partie, **à usage unique** (consommation atomique, annulée si l'action échoue), valable 7 jours, invalidé par la pause, la fin ou un nouveau token.
- Réponse identique (404, page neutre) pour un lien inconnu, mal formé, expiré, déjà utilisé ou dont la partie n'est plus en cours ; la page n'expose que le jeu et les joueurs de sa partie, sans lien ni ressource vers le reste de l'application.
- Protection des liens : limitation de débit par adresse (30 requêtes par minute, 429 avec `Retry-After`) avant Spring Security et avant tout accès à la base ; en-têtes `Cache-Control: no-store`, `Referrer-Policy: no-referrer`, `X-Robots-Tag: noindex`.
- Réglages `ludalex.fin.validite` (7d), `ludalex.fin.limite-par-minute` (30) et `ludalex.fin.en-tete-ip` ; en `prod`, l'adresse du client est lue dans `CF-Connecting-IP`.
- Tables `token_fin_partie` et colonne `terminee_le` de `partie` (voir `docs/database.puml`).
- Tests : règles de la fin en direct, token (génération, hachage, expiration, usage unique), cycle de vie sur base réelle (atomicité, retour arrière, lancements et consommations simultanés), page publique (inconnu, expiré, utilisé, autre partie, CSRF, en-têtes, 429), écrans du propriétaire, accès sans JWT limité à `/fin/**` en `prod`.

### Changed
- La suppression d'une partie supprime aussi ses tokens de fin de partie.
- La liste des parties n'offre plus la modification d'une partie en cours ou en pause et propose le lien « Partie en direct ».
- `PartieService` : constructeur et règles de gagnants partagés entre parties déclarées et parties en direct.

## [0.6.0] - 2026-11-12

### Added
- Statistiques par jeu sur la fiche (chargement HTMX, `/jeux/{id}/statistiques`) : classement des joueurs (parties jouées, victoires, taux de victoire arrondi), champion(s) et dix dernières parties terminées (date, lieu, joueurs, scores, gagnants).
- Champion(s) d'un jeu : le ou les joueurs ayant le plus de victoires (au moins une) ; en cas d'égalité tous les ex æquo ; aucun minimum de parties ; jeux coopératifs et compétitifs regroupés (une victoire coopérative compte pour chaque participant) ; les joueurs anonymisés restent dans le classement mais ne peuvent pas être champions.
- Symbole de champion sur les vignettes de la liste des jeux : avatar de chaque champion, plusieurs en cas d'égalité, rien sans victoire.
- Calculs par requêtes agrégées sur les parties terminées uniquement (parties en cours ou en pause ignorées) ; les champions d'une page de 24 jeux s'obtiennent en un nombre constant de requêtes (vérifié par test).
- Interface `ChampionsDesJeux` dans le module des jeux, implémentée par les statistiques.
- Tests : service, requêtes agrégées JPA, intégration (égalité, anonymisation, coopératif, dix dernières parties, nombre de requêtes), contrôleur.

### Changed
- La fiche d'un jeu charge désormais sa zone de statistiques ; la vignette de la liste affiche les champions.
- `PartieRepository` : requête des dernières parties d'un jeu par statut.

## [0.5.0] - 2026-11-12

### Added
- Parties (`/parties`) : déclaration d'une partie terminée (jeu, joueurs, date du jour par défaut, lieu facultatif, gagnants, scores facultatifs), modification, suppression avec confirmation HTMX, liste paginée (20 par page) de la plus récente à la plus ancienne avec filtre par jeu.
- Gagnants multiples ; un gagnant doit participer à la partie ; un joueur ne peut figurer qu'une fois ; date non future ; joueurs anonymisés refusés pour une nouvelle partie (conservés dans une partie existante).
- Équipes propres à une partie (4 au plus, nom facultatif, un joueur par équipe) : une équipe gagnante rend gagnants tous ses membres ; une équipe sans membre est refusée.
- Jeux coopératifs : résultat victoire ou défaite à la place des gagnants ; une victoire compte pour chaque participant. Le formulaire s'adapte au jeu choisi (HTMX).
- Lieu en texte libre (100 caractères) avec autocomplétion HTMX sur les lieux déjà saisis (10 suggestions au plus, sans doublon, insensible à la casse, caractères `%` et `_` traités littéralement).
- Tables `partie`, `participation` et `equipe` (voir `docs/database.puml`) ; champs de pause (`statut`, `joueur_suivant_id`) prévus pour le change `partie-en-cours`.
- Tests : règles métier (Mockito), persistance réelle (clés étrangères, modification en place, suppression avec équipes, anonymisation), contrôleur et autocomplétion.

### Changed
- Les interfaces `ParticipationsJoueur` et `PartiesJeu` sont désormais implémentées par les parties réelles : un joueur ayant des parties est anonymisé à la suppression, et la suppression d'un jeu ayant des parties est refusée. Les implémentations provisoires sont supprimées.
- `JeuService.tousParNom()` ajouté pour les listes de choix.

## [0.4.0] - 2026-11-12

### Added
- Ludothèque (`/jeux`) : jeux avec nom, image (upload ou URL), prix, date d'achat et indicateur coopératif ; liste en vignettes paginées (24 par page, fond de la couleur du jeu), fiche détail, création, modification et suppression avec confirmation HTMX.
- Couleur de fond calculée à l'enregistrement, modifiable à la main (case « Choisir la couleur manuellement ») ; conservée si l'image ne change pas, recalculée si elle change. Seules les couleurs `#RRGGBB` sont acceptées.
- Contrôles : nom obligatoire (200 caractères), prix positif avec deux décimales au plus, date d'achat non future.
- Prêts : prêter un jeu à une personne (texte libre, 100 caractères), enregistrer le retour, historique complet sur la fiche, emprunteur courant sur la vignette. Un seul prêt en cours par jeu, garanti par un verrou pessimiste (test de concurrence).
- Suppression d'un jeu refusée s'il a des parties (interface `PartiesJeu`, implémentation provisoire à remplacer par le change `parties`) ; sinon suppression du jeu, de ses prêts et de son image.
- Emplacements réservés pour le symbole de champion (liste) et les statistiques (fiche), alimentés par le change `statistiques`.
- Tables `jeu` et `pret` (voir `docs/database.puml`) ; bean `Clock` pour les dates du jour.
- Tests : services, dépôts JPA, contrôleur, concurrence des prêts.

### Changed
- `EntreeImage` déplacé du package `joueurs` vers `images` pour être partagé avec les jeux.
- Fragment de saisie de couleur : case à cocher pour choisir la couleur à la main.

## [0.3.0] - 2026-11-12

### Added
- Gestion des joueurs (Admin, `/admin/joueurs`) : fiches sans compte avec nom, avatar et image de victoire (fichier uploadé ou URL), liste paginée (20 par page), création, modification, retrait d'une image.
- Nom obligatoire (100 caractères maximum) et unique parmi les joueurs actifs, sans tenir compte de la casse (colonne `nom_normalise`, indépendante du moteur).
- Suppression par **anonymisation** quand le joueur a des parties (nom « Joueur supprimé », images effacées du volume, fiche conservée), suppression définitive sinon, avec confirmation HTMX.
- Les joueurs anonymisés ne sont ni modifiables ni proposés pour une partie (`JoueurService.selectionnables`).
- Page `/admin` d'accueil de l'administration.
- Interface `ParticipationsJoueur` avec implémentation provisoire (aucune participation), à remplacer par le change `parties`.
- Table `joueur` (voir `docs/database.puml`).
- Tests : service, dépôt JPA, contrôleur d'administration (création, doublon, image invalide, CSRF, modification, suppression, pagination).

### Changed
- `ServiceImages` : `enregistrerUpload` et `enregistrerUrl` pour les images sans couleur de fond (aucun téléchargement distant).

## [0.2.0] - 2026-11-12

### Added
- Composant d'image réutilisable (`ImageSource`) : fichier uploadé (JPEG, PNG, GIF, WebP, 2 Mo maximum, type vérifié par signature) stocké sur le volume sous un nom généré, ou simple URL http(s) jamais copiée.
- Restitution en flux des images du volume (`GET /images/{nom}`, cache privé d'un an) sans possibilité de sortir du volume.
- Protection SSRF des requêtes sortantes : refus des adresses de boucle, privées, de lien local, métadonnées cloud, ULA, CGNAT ; contrôle à la résolution DNS de la connexion (anti DNS rebinding), redirections revalidées (5 maximum), délai et taille plafonnés.
- Calcul unique de la couleur de fond à l'enregistrement : décodage avec sous-échantillonnage, classe de couleur la plus fréquente (blanc, noir et transparent ignorés), repli neutre `#3a3a3a`, choix manuel conservé.
- Lecture du format WebP (TwelveMonkeys `imageio-webp`, Java pur, compatible arm64).
- Fragments Thymeleaf de saisie (upload ou URL, aperçu, couleur) et d'affichage d'image avec image par défaut.
- Configuration `ludalex.images.repertoire` (en Docker : `LUDALEX_IMAGES_REPERTOIRE`, valeur `/data/images`) et limites d'upload multipart de 2 Mo.
- Tests : formats, stockage, traversal de chemin, SSRF, téléchargement (taille, redirections, délai), couleur, service, contrôleur.

### Changed
- Aucune modification de comportement existant ; dépendances ajoutées : `imageio-webp` et `httpclient5`.

## [0.1.0] - 2026-11-12

### Added
- Socle de l'application Spring Boot 4 / Java 25 (`com.alexgit95.ludalex`), Maven Wrapper pour le build Docker.
- Profils `dev` (SQLite, HTTP, sans authentification, profil par défaut) et `prod` (PostgreSQL, cookies `Secure`, `X-Forwarded-*`).
- Contrôle d'accès par Cloudflare Access en `prod` : validation du JWT `Cf-Access-Jwt-Assertion` (signature, expiration, émetteur, audience), 401 sans JWT valide, 403 en cas de mauvaise audience, aucune identité stockée.
- Démarrage refusé en `prod` sans `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD` ou si le mode d'accès n'est pas `cloudflare` ; `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` obligatoires.
- Exception de sécurité limitée au chemin `/fin/**` (le contrôleur sera ajouté par le change `partie-en-cours`).
- Protection CSRF sur toutes les requêtes modifiantes, jeton transmis aux requêtes HTMX.
- Interface Thymeleaf + HTMX (servi en local), CSS maison, pages d'erreur sans détail technique, PWA installable (manifeste, icônes, service worker limité aux ressources statiques).
- `Dockerfile` avec `-Xmx384m`, GC série, mode headless et volume `/data/images` ; `docker-compose.yml` (application + PostgreSQL) et `.env.example`.
- Tests : JWT valide, expiré, mauvaise audience, mauvais émetteur, signature inconnue, absent, header d'identité forgé, accès libre en `dev`, exception `/fin/**`, CSRF, démarrage `prod`.

### Changed
- Version du `pom.xml` fixée à `0.1.0` (balise `<version>` valide).
- `Dockerfile` : l'ENTRYPOINT lit les options JVM dans `JAVA_OPTS`.
