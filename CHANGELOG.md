# Changelog

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/). La version est identique dans le `pom.xml`.

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
