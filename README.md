# LUDALEX

Application web (PWA) **mono-utilisateur** de gestion de ludothèque personnelle : jeux, prêts, parties, statistiques et écran d'affichage. Les joueurs sont de simples fiches déclarées dans l'application, sans compte. Cible de déploiement : Raspberry Pi.

Les spécifications détaillées sont dans le dossier `openspec/` ; le plan des changes est dans `docs/plan-des-changes.md`.

## État d'avancement

Version courante : **0.13.2**. Disponible : profils, sécurité, interface de base, PWA, conteneurisation, images, joueurs, ludothèque, parties, statistiques, parties en direct, écran Affichage, sauvegarde/restauration import MyLudo et charte graphique heroic fantasy.

## Parties en direct

L'écran **Parties > Partie en direct** (`/parties/direct`) suit une partie pendant qu'elle se joue.

- **Lancer** : choisir le jeu et les joueurs (équipes facultatives). Un lien/QR de fin de partie est alors généré.
- **Une seule partie à la fois** : lancer une seconde partie demande d'abord de terminer la précédente (gagnants, scores facultatifs ou victoire/défaite). Si le lancement est invalide, la partie précédente reste inchangée.
- **Pause** : indiquer à quel joueur c'était de jouer ; **Reprendre** relance la partie et génère un nouveau lien.
- **Terminer** : désigner le ou les gagnants (ou une équipe), scores en option ; pour un jeu coopératif, victoire ou défaite.

### Lien de fin de partie (`/fin/{token}`)

N'importe quel appareil peut terminer ou mettre en pause la partie via ce lien, **sans authentification Cloudflare**. Le token est la seule autorisation : aléatoire (256 bits), stocké haché, à usage unique, valable 7 jours, limité à sa partie, et il ne révèle rien d'autre que le jeu et les joueurs de cette partie. Un lien inconnu, expiré ou déjà utilisé donne toujours la même page neutre. Le nombre de requêtes par adresse est limité (30 par minute).

Il faut une règle Cloudflare Access de type Bypass limitée à `/fin/*` : voir `docs/fin-de-partie-qr.md`. Le QR code qui encode ce lien est affiché par l'écran Affichage (ci-dessous).

## Écran Affichage

`/affichage` est conçu pour tourner en continu sur une télé ou une tablette (plein écran, lisible de loin). Il se met à jour tout seul (interrogation légère toutes les 3 s) :

- **Partie en cours** : l'image du jeu sert de fond (floutée, assombrie) sur la couleur de fond du jeu ; titre du jeu en haut, QR code de fin de partie en coin, joueurs en grand sur toute la hauteur restante (jamais rognés). Un cartouche **VS** (épées croisées, pulsation lente, désactivée si le navigateur demande un mouvement réduit) matérialise l'affrontement : duel face à face, VS entre chaque paire de joueurs à partir de trois, parties par équipes en colonnes nommées séparées par le VS (joueurs sans équipe dans un groupe à part), libellé « Ensemble » pour un jeu coopératif, aucun VS pour un joueur seul.
- **Partie en pause** : le joueur à qui c'est de jouer, en grand, sans QR code, sur le même fond.
- **Victoire** : animation du ou des vainqueurs pendant 15 s après la fin de la partie (message de défaite pour une défaite coopérative), puis retour à la veille.
- **Veille** : diaporama en fondu d'illustrations (château, village, nœud celtique, menhirs). Pour en ajouter, déposer des images (`svg`, `png`, `jpg`, `webp`, 2 Mo maximum) dans `src/main/resources/static/diaporama/` et reconstruire ; sans image, l'écran affiche LUDALEX.

Le QR change à chaque ouverture ou rechargement de la page (un QR scanné avant le rechargement n'est plus valable). L'écran demande au navigateur de rester allumé lorsque c'est possible.

La session Cloudflare Access dure un mois : à l'expiration, après trois échecs de mise à jour, la page se recharge et Cloudflare redemande la connexion ; une intervention humaine est donc nécessaire une fois par mois.

Réglages : `ludalex.affichage.url-publique` (origine du QR, déduite de la requête par défaut ; utile en `dev` pour qu'un téléphone joigne le poste), `ludalex.affichage.duree-victoire` (`15s`), `ludalex.affichage.renouvellement-token` (`24h`).

## Sauvegarde et restauration

L'écran **Admin > Sauvegarde et restauration** (`/admin/sauvegarde`) permet de ne rien perdre si la carte SD du Raspberry Pi meurt ou si le moteur de base change.

- **Sauvegarde** : « Télécharger la sauvegarde » fournit un seul fichier `ludalex-sauvegarde-<date>.zip` avec les données (joueurs, jeux, prêts, parties, participations, équipes) et les images uploadées. Les images saisies par adresse web ne sont conservées que sous forme d'adresse. Les tokens de fin de partie et la configuration ne sont pas sauvegardés. Le fichier est écrit en flux (lecture par lots de 500 enregistrements), sans charger la base en mémoire.
- **Restauration** : on ré-uploade le ZIP **tel quel**, sans le modifier. Elle **remplace tout** (données et images) et n'est lancée qu'après saisie du mot `RESTAURER` et avec un jeton CSRF valide. Une sauvegarde faite en `dev` (SQLite) se restaure en `prod` (PostgreSQL) et inversement. Les liens de fin de partie existants deviennent invalides.
- **Sûreté** : le ZIP est entièrement validé avant la moindre suppression (chemins dangereux ou « zip slip », entrées hors `manifest.json`, `donnees/`, `images/`, nombre d'entrées, taille et taux de décompression, images réelles PNG/JPEG/GIF/WebP de 2 Mo maximum, version du format, références cohérentes, compteurs du manifeste). Le remplacement des données est une seule transaction : en cas d'échec, données et images précédentes sont conservées. Une seule restauration à la fois.
- **Limites** : le ZIP est reçu comme corps brut de la requête (pas en multipart) et écrit dans un fichier temporaire. Réglages : `ludalex.sauvegarde.taille-max` (512MB), `ludalex.sauvegarde.taille-decompressee-max` (1GB), `ludalex.sauvegarde.entrees-max` (10000), `ludalex.sauvegarde.ratio-max` (200) et `ludalex.sauvegarde.repertoire-temporaire` (répertoire temporaire du système par défaut, variable Docker `LUDALEX_SAUVEGARDE_REPERTOIRE_TEMPORAIRE`). Derrière Cloudflare, l'envoi est limité par l'offre (100 Mo en gratuit).

### Format de sauvegarde (version 1)

```
manifest.json            formatVersion, appVersion, date, compteurs par type
donnees/joueurs.json     un tableau JSON par type : joueurs, jeux, parties, equipes, participations, prets
images/<uuid>.<ext>      images uploadées référencées par les données
```

Les dates sont en ISO-8601, les décimaux en chaînes, les images par `{type: UPLOAD|URL, reference}`. Les identifiants d'origine (`idSauvegarde`) ne servent que de références : de nouveaux identifiants sont générés à la restauration, ce qui rend le format indépendant du moteur. **Règle d'évolution** : toute nouvelle entité ou colonne doit être ajoutée au format (un test vérifie que chaque entité JPA est exportée) ; un changement incompatible incrémente `formatVersion`, la restauration refuse toute version inconnue, et une évolution du format est un breaking change documenté dans le changelog.

## Import MyLudo

L'écran **Admin > Import MyLudo** (`/admin/import-myludo`) reprend votre historique MyLudo : jeux possédés, joueurs et parties (date, lieu, scores, équipes, résultat coopératif) ; les statistiques se recalculent ensuite depuis les parties.

1. **Fichiers** : envoyer l'export de la **collection** et/ou celui des **parties** (JSON, 2 Mo maximum chacun). Exemples anonymisés : `docs/exemple-my-ludo/`.
2. **Analyse** (rien n'est écrit) : nombre de jeux et de parties à ajouter, déjà présentes ou ignorées (avec la raison), avertissements, et **correspondance des joueurs** : chaque nom de l'export est associé par défaut à la fiche de même nom (casse, accents et espaces ignorés), au choix à une autre fiche ou à un nouveau joueur.
3. **Import** avec barre de progression (phase, `n sur total`, élément en cours), puis un message de réussite avec les compteurs, ou un message d'erreur précis : fichier, numéro d'entrée, jeu et date, champ, cause, et ligne/colonne d'un JSON mal formé. En cas d'erreur, rien n'est enregistré (une seule transaction).

**Delta seulement, rien n'est écrasé.** Une partie du fichier est considérée comme déjà présente si, pour le même jeu et le même jour, une partie en base partage au moins un joueur avec elle ; chaque partie en base ne « couvre » qu'une seule partie du fichier, si bien que plusieurs manches identiques le même jour sont conservées. Réimporter le même fichier n'ajoute rien, un export plus récent n'ajoute que les nouvelles parties. Les parties existantes ne sont jamais modifiées ; un jeu existant n'est pas modifié (seuls son prix et sa date d'achat vides sont complétés).

**Conversion** : un jeu est coopératif si une de ses parties l'est (ou par le mécanisme « Coopération ») ; une partie coopérative devient une victoire ou une défaite ; scores tous nuls = pas de score ; deux joueurs de même nom dans une partie (« Anonyme ») deviennent « Anonyme » et « Anonyme 2 » ; un prix de 0 est considéré inconnu. **Parties ignorées** (listées avec leur raison) : exclues dans MyLudo, datées dans le futur, sans joueur, ou sans gagnant dans un jeu non coopératif. **Non importés** : durée, commentaire, notes, images et métadonnées des jeux (éditeurs, thèmes...).

L'analyse ou le rapport est conservé en mémoire 30 minutes (`ludalex.import-myludo.duree-session`), les listes détaillées sont limitées à 200 lignes (`ludalex.import-myludo.lignes-max`). Une seule analyse ou un seul import à la fois.

## Charte graphique

Thème « heroic fantasy » sobre, sur toute l'application (y compris l'écran Affichage) : fond crème et parchemin très discret, petits lierres en filigrane (coin de l'en-tête, séparateurs de titres, coins des cartes), titres en police à empattements et texte en sans-serif pour rester très lisible. Aucune police ni ressource externe : polices du système uniquement.

- Les couleurs sont des variables CSS au début de `static/css/ludalex.css` (`:root`) ; `affichage.css` les réutilise. Les contrastes texte/fond respectent WCAG AA (vérifié par `CharteGraphiqueTest`).
- **Menu burger** : sur petit écran (< 40 rem) avec JavaScript, la navigation est repliée derrière un bouton (icône en trois lianes) qui ouvre un panneau parchemin sous l'en-tête ; fermeture au clic extérieur ou sur Échap ; sans JavaScript, la navigation reste affichée.
- Les décors sont de petits SVG dans `static/decor/` (`parchemin.svg`, `lierre-coin.svg`, `lierre-separateur.svg`) ; pour en ajouter un, le déposer dans ce dossier et le référencer par une variable `--decor-*`.
- La couleur de fond propre à chaque jeu est conservée comme cadre ; le texte repose toujours sur un cartouche clair.
- Pour changer de police, modifier `--police-titre` / `--police-texte` (une police embarquée devra être placée dans `static/`, jamais chargée depuis un CDN).

## Statistiques

La fiche d'un jeu affiche ses statistiques, calculées sur les parties **terminées** :

- **Classement** : parties jouées, victoires et taux de victoire (victoires / parties) de chaque joueur, sans minimum de parties. Jeux coopératifs et compétitifs sont traités de la même façon : une victoire coopérative compte pour chaque participant.
- **Champion** : le joueur qui a le plus de victoires (au moins une). En cas d'égalité, tous les premiers sont champions. Un joueur anonymisé reste dans le classement mais ne peut pas être champion.
- **Liste des jeux** : chaque vignette porte l'avatar du ou des champions.
- **Dix dernières parties** : date, lieu, joueurs, scores et gagnants.

## Parties

L'écran **Parties** (`/parties`) liste les parties de la plus récente à la plus ancienne (filtre par jeu) et permet d'en déclarer après coup.

- **Déclaration** : choisir le jeu (son image s'affiche dès le choix pour confirmer visuellement), cocher les joueurs, indiquer la date (aujourd'hui par défaut, jamais dans le futur), le lieu (facultatif) et les scores (facultatifs).
- **Jeu compétitif** : désigner au moins un gagnant parmi les participants ; plusieurs gagnants sont possibles.
- **Jeu coopératif** : choisir « Victoire » ou « Défaite » ; une victoire compte pour chaque participant.
- **Équipes** : jusqu'à 4 équipes propres à la partie (nom facultatif) ; désigner une équipe gagnante rend gagnants tous ses membres.
- **Lieu** : texte libre ; en tapant, les lieux déjà saisis sont proposés (10 au plus).
- **Modification et suppression** : une partie peut être corrigée ou supprimée (confirmation). Un joueur anonymisé reste dans ses anciennes parties mais ne peut pas être ajouté à une nouvelle.
- Un jeu ayant des parties ne peut pas être supprimé ; un joueur ayant des parties est anonymisé au lieu d'être supprimé.

## Ludothèque

L'écran **Ludothèque** (`/jeux`) liste les jeux en vignettes (image, titre dessous, fond de la couleur du jeu), 2 par ligne sur petit écran et 3 sur grand écran. L'ouverture de `/` (logo ou lancement PWA) redirige vers cette liste; son titre et l'action « Ajouter un jeu » sont alignés sur le bord gauche de la grille centrée. Sous le titre de chaque vignette, un petit parchemin affiche la coupe, l'avatar et le nom du ou des champions (à la ligne en cas d'égalité) ; la hauteur est réservée pour les jeux sans champion.

- **Jeu** : nom (obligatoire), image (upload ou URL), prix (positif, deux décimales), date d'achat (pas dans le futur), jeu coopératif ou non. Sans image, une image par défaut est affichée.
- **Couleur de fond** : calculée à l'enregistrement depuis l'image ; « Choisir la couleur manuellement » permet de la corriger. Elle est conservée tant que l'image ne change pas.
- **Fiche** : date d'achat, prix, type, prêt en cours et historique des prêts ; zone réservée aux statistiques de parties.
- **Prêts** : « Prêter » demande l'emprunteur (texte libre) et la date (aujourd'hui par défaut) ; « Enregistrer le retour » clot le prêt. Un jeu ne peut être prêché qu'à une personne à la fois ; l'historique est conservé, l'emprunteur courant apparaît sur la vignette.
- **Suppression** : avec confirmation ; refusée si le jeu a des parties. Elle supprime aussi l'historique des prêts et l'image uploadée.

## Administration des joueurs

L'écran **Admin > Joueurs** (`/admin/joueurs`) gère les fiches de joueurs, sans compte ni lien avec Cloudflare.

- Un joueur a un **nom** (obligatoire, 100 caractères, unique sans tenir compte de la casse), un **avatar** et une **image de victoire** facultatifs (upload ou URL, voir Images).
- Sans image, une image par défaut est affichée.
- La **suppression** demande une confirmation. Un joueur qui a des parties est **anonymisé** : son nom devient « Joueur supprimé », ses images sont effacées et ses parties restent consultables. Sans partie, il est supprimé définitivement.
- Un joueur anonymisé n'est plus modifiable ni proposé pour une nouvelle partie.

## Images

Chaque image (jeu, joueur) est soit un **fichier uploadé**, soit une **URL**.

- **Upload** : JPEG, PNG, GIF ou WebP, 2 Mo maximum, type vérifié sur le contenu. Le fichier est stocké sur le volume (`ludalex.images.repertoire`, par défaut `./data/images`, `/data/images` en Docker) sous un nom généré et servi par `/images/{nom}`.
- **URL** : seule l'adresse est conservée ; l'image n'est jamais copiée et reste résolue par le navigateur.
- **Couleur de fond** : calculée une seule fois à l'enregistrement (image réduite, couleur la plus fréquente, hors blanc, noir et transparent), stockée et modifiable à la main ; `#3a3a3a` en cas d'échec. Pour une URL, l'image est téléchargée une seule fois, uniquement pour ce calcul.
- **Sécurité SSRF** : tout téléchargement refuse les adresses locales ou privées (y compris après redirection ou résolution DNS) et plafonne délai et taille. Une URL du réseau local peut être enregistrée, mais sa couleur reste neutre.

## Technologies

- Java 25, Spring Boot 4.x, Spring Security, Spring Data JPA
- Thymeleaf + HTMX (servi localement), CSS maison, PWA minimale
- SQLite (dev) et PostgreSQL (prod), schéma généré par Hibernate (pas de migration)
- Docker / docker-compose

## Profils

| Profil | Usage | Base | Authentification |
|---|---|---|---|
| `dev` (défaut) | Développement local, HTTP | SQLite (`ludalex-dev.db`) | **Aucune** |
| `prod` | Docker, HTTPS terminé par Cloudflare | PostgreSQL | **Cloudflare Access obligatoire** |

### Variables d'environnement (prod)

| Variable | Rôle |
|---|---|
| `DB_URL` | URL JDBC PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | Identifiants de la base |
| `CF_ACCESS_TEAM_DOMAIN` | Domaine d'équipe Cloudflare (`xxx.cloudflareaccess.com`) |
| `CF_ACCESS_AUD` | Audience (AUD) de l'application Cloudflare Access |
| `JAVA_OPTS` | Optionnelle, options JVM (défaut : `-Xmx384m -XX:+UseSerialGC -Djava.awt.headless=true`) |
| `LUDALEX_IMAGES_REPERTOIRE` | Optionnelle, volume des images (défaut Docker : `/data/images`) |

Toutes les variables obligatoires manquantes font échouer le démarrage avec un message explicite.

## Authentification

L'application ne gère ni compte, ni mot de passe, ni rôle.

- **prod** : chaque requête doit porter un JWT Cloudflare Access valide (header `Cf-Access-Jwt-Assertion`) : signature, expiration, émetteur et audience. Sans JWT valide : 401 ; mauvaise audience : 403. Aucune identité n'est conservée.
- **dev** : tout est autorisé, sans login ni JWT. Ce mode ne peut pas être activé en `prod`.
- **Exception** : le chemin `/fin/**` (lien/QR de fin de partie) n'exige pas de JWT ; il est protégé par un token à usage unique. Voir `docs/fin-de-partie-qr.md` pour la règle Cloudflare à créer.
- Le CSRF est actif sur toutes les requêtes modifiantes.

## Lancer en local (dev)

```bash
C:\USINE_LOGICIELLE\apache-maven\bin\mvn spring-boot:run
```

L'application est servie sur <http://localhost:8181>.

## Lancer avec Docker (prod)

```bash
cp .env.example .env   # puis renseigner les valeurs
docker compose up --build
```

- La JVM est limitée par `-Xmx` (Raspberry Pi) et tourne en mode headless.
- Les images uploadées seront stockées sur le volume `/data/images`.
- Cloudflare doit publier l'application derrière Access, avec la règle Bypass limitée à `/fin/*`.

### Portainer / Arcane

`docker-compose.portainer.yml` est une stack prête à coller (Portainer : Stacks > Add stack ; Arcane : Projects) qui utilise l'image Docker Hub `<DOCKERHUB_USERNAME>/ludalex` (arm64). Les deux services ont un healthcheck : PostgreSQL via `pg_isready`, l'application via une requête HTTP sur son port 8181 (sans curl, avec 120 s de démarrage toléré), visible comme « healthy » dans Portainer ou Arcane. Variables à renseigner dans l'interface : `DOCKERHUB_USERNAME`, `DB_USERNAME`, `DB_PASSWORD`, `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD` ; facultatives : `LUDALEX_TAG` (défaut `latest`), `LUDALEX_PORT` (défaut `8181`), `POSTGRES_DB`, `JAVA_OPTS`.

## Tests

```bash
C:\USINE_LOGICIELLE\apache-maven\bin\mvn test
```

## Documentation

- `docs/database.puml` : modèle de données (PlantUML)
- `docs/fin-de-partie-qr.md` : fin de partie par QR code et configuration Cloudflare
- `docs/exemple-my-ludo/` : exports MyLudo anonymisés servant de jeu d'essai
- `docs/plan-des-changes.md` : plan et décisions du projet
- `CHANGELOG.md` : journal des modifications
