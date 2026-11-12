# LUDALEX

Application web (PWA) **mono-utilisateur** de gestion de ludothèque personnelle : jeux, prêts, parties, statistiques et écran d'affichage. Les joueurs sont de simples fiches déclarées dans l'application, sans compte. Cible de déploiement : Raspberry Pi.

Les spécifications détaillées sont dans le dossier `openspec/` ; le plan des changes est dans `docs/plan-des-changes.md`.

## État d'avancement

Version courante : **0.3.0**. Disponible : profils, sécurité, interface de base, PWA, conteneurisation, gestion des images et des joueurs. Les fonctionnalités métier restantes (jeux, parties, affichage, sauvegarde) arrivent avec les changes suivants.

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
