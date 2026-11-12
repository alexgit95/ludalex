# LUDALEX

Application web (PWA) **mono-utilisateur** de gestion de ludothèque personnelle : jeux, prêts, parties, statistiques et écran d'affichage. Les joueurs sont de simples fiches déclarées dans l'application, sans compte. Cible de déploiement : Raspberry Pi.

Les spécifications détaillées sont dans le dossier `openspec/` ; le plan des changes est dans `docs/plan-des-changes.md`.

## État d'avancement

Version courante : **0.8.0**. Disponible : profils, sécurité, interface de base, PWA, conteneurisation, images, joueurs, ludothèque, parties, statistiques, parties en direct et écran Affichage. La sauvegarde arrive avec le change suivant.

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

- **Partie en cours** : image du jeu sur sa couleur de fond (ou titre seul sans image), avatars et noms des joueurs, QR code de fin de partie.
- **Partie en pause** : le joueur à qui c'est de jouer, en grand, sans QR code.
- **Victoire** : animation du ou des vainqueurs pendant 15 s après la fin de la partie (message de défaite pour une défaite coopérative), puis retour à la veille.
- **Veille** : diaporama en fondu d'illustrations (château, village, nœud celtique, menhirs). Pour en ajouter, déposer des images (`svg`, `png`, `jpg`, `webp`, 2 Mo maximum) dans `src/main/resources/static/diaporama/` et reconstruire ; sans image, l'écran affiche LUDALEX.

Le QR change à chaque ouverture ou rechargement de la page (un QR scanné avant le rechargement n'est plus valable). L'écran demande au navigateur de rester allumé lorsque c'est possible.

La session Cloudflare Access dure un mois : à l'expiration, après trois échecs de mise à jour, la page se recharge et Cloudflare redemande la connexion ; une intervention humaine est donc nécessaire une fois par mois.

Réglages : `ludalex.affichage.url-publique` (origine du QR, déduite de la requête par défaut ; utile en `dev` pour qu'un téléphone joigne le poste), `ludalex.affichage.duree-victoire` (`15s`), `ludalex.affichage.renouvellement-token` (`24h`).

## Statistiques

La fiche d'un jeu affiche ses statistiques, calculées sur les parties **terminées** :

- **Classement** : parties jouées, victoires et taux de victoire (victoires / parties) de chaque joueur, sans minimum de parties. Jeux coopératifs et compétitifs sont traités de la même façon : une victoire coopérative compte pour chaque participant.
- **Champion** : le joueur qui a le plus de victoires (au moins une). En cas d'égalité, tous les premiers sont champions. Un joueur anonymisé reste dans le classement mais ne peut pas être champion.
- **Liste des jeux** : chaque vignette porte l'avatar du ou des champions.
- **Dix dernières parties** : date, lieu, joueurs, scores et gagnants.

## Parties

L'écran **Parties** (`/parties`) liste les parties de la plus récente à la plus ancienne (filtre par jeu) et permet d'en déclarer après coup.

- **Déclaration** : choisir le jeu, cocher les joueurs, indiquer la date (aujourd'hui par défaut, jamais dans le futur), le lieu (facultatif) et les scores (facultatifs).
- **Jeu compétitif** : désigner au moins un gagnant parmi les participants ; plusieurs gagnants sont possibles.
- **Jeu coopératif** : choisir « Victoire » ou « Défaite » ; une victoire compte pour chaque participant.
- **Équipes** : jusqu'à 4 équipes propres à la partie (nom facultatif) ; désigner une équipe gagnante rend gagnants tous ses membres.
- **Lieu** : texte libre ; en tapant, les lieux déjà saisis sont proposés (10 au plus).
- **Modification et suppression** : une partie peut être corrigée ou supprimée (confirmation). Un joueur anonymisé reste dans ses anciennes parties mais ne peut pas être ajouté à une nouvelle.
- Un jeu ayant des parties ne peut pas être supprimé ; un joueur ayant des parties est anonymisé au lieu d'être supprimé.

## Ludothèque

L'écran **Ludothèque** (`/jeux`) liste les jeux en vignettes (image, titre dessous, fond de la couleur du jeu).

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

## Tests

```bash
C:\USINE_LOGICIELLE\apache-maven\bin\mvn test
```

## Documentation

- `docs/database.puml` : modèle de données (PlantUML)
- `docs/fin-de-partie-qr.md` : fin de partie par QR code et configuration Cloudflare
- `docs/plan-des-changes.md` : plan et décisions du projet
- `CHANGELOG.md` : journal des modifications
