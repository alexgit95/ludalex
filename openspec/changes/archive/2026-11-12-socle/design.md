## Context

Dépôt quasi vide. Contraintes : Java 25, Spring Boot 4.1.x, Raspberry Pi (arm64, mémoire limitée), authentification déléguée à Cloudflare Access, application mono-utilisateur, `dev` sans authentification, schéma Hibernate sans migration, SQLite et PostgreSQL à compatibilité totale. Le `pom.xml` actuel contient déjà les dépendances principales mais est invalide (`<version>0.0.1/version>`) ; le `Dockerfile` référence un `mvnw` absent.

## Goals / Non-Goals

**Goals:**
- Socle exécutable en `dev` et `prod`, sécurisé, packagé en image Docker arm64 sobre.
- Filtre Cloudflare testable isolément (JWT valide / expiré / mauvaise audience / absent).
- Gabarit Thymeleaf + HTMX + PWA réutilisé par tous les changes suivants.

**Non-Goals:**
- Aucune entité métier (jeux, joueurs, parties) : elles viennent avec leurs changes.
- Le contrôleur `/fin/{token}` : seule l'exception de sécurité est posée ici.
- Aucune modification des pipelines GitHub Actions.

## Decisions

- **Package racine `com.alexgit95.ludalex`**, découpage par fonctionnalité (`acces`, `images`, `joueurs`, `jeux`, `parties`, …) plutôt que par couche technique : chaque change ajoute son package sans toucher aux autres.
- **Validation du JWT avec `spring-security-oauth2-jose` (`NimbusJwtDecoder` + `JwkSetUri`)** sur `https://<CF_ACCESS_TEAM_DOMAIN>/cdn-cgi/access/certs`, avec validateurs d'émetteur, d'expiration et d'audience. Alternative rejetée : parser le JWT à la main (risque d'erreur cryptographique). Le jeu de clés est mis en cache par Nimbus (pas d'appel par requête).
- **Un filtre, deux chaînes de sécurité** : `SecurityFilterChain` conditionnée par profil. En `prod`, la chaîne `/fin/**` est `permitAll` (l'autorisation est le token) et la chaîne par défaut exige le JWT. En `dev`, tout est `permitAll`. Un `@ConfigurationProperties` `ludalex.acces.mode` (`cloudflare` | `aucun`) pilote le choix, mais un validateur au démarrage refuse `aucun` quand le profil `prod` est actif.
- **Pas de session applicative authentifiée** : le JWT est revérifié à chaque requête (stateless côté authentification) ; la session ne sert qu'au jeton CSRF. Cela évite tout stockage d'identité.
- **CSRF** : jeton Spring Security transmis aux requêtes HTMX via un `hx-headers` posé sur `<body>`. `/fin/**` conserve le CSRF (la page affichée porte son jeton).
- **Profils** : `dev` par défaut via `spring.profiles.default=dev`. SQLite avec le dialecte `hibernate-community-dialects`, fichier `./data/ludalex.db`. `ddl-auto=update` en `dev` et `prod` (aucun outil de migration, décision projet).
- **HTMX** servi en fichier statique local (pas de CDN : fonctionne hors ligne et évite une requête tierce).
- **PWA** : `manifest.webmanifest` + service worker qui ne met en cache que les ressources statiques (`/css`, `/js`, icônes) ; aucune page dynamique n'est cachée, pour ne jamais servir de données obsolètes.
- **Docker** : build multi-étapes existant conservé ; ajout de `-Xmx` (valeur initiale 384m, ajustable par `JAVA_OPTS`), `-XX:+UseSerialGC`, `-Djava.awt.headless=true`, volume `/data/images`. Le `mvnw`/`.mvn` est généré ou le build passe par l'image Maven ; à trancher à l'implémentation (le binaire local reste `C:\USINE_LOGICIELLE\apache-maven\bin`).
- **docker-compose.yml** : services `app` et `postgres`, volumes nommés pour la base et les images, variables lues depuis un `.env` non versionné.

## Risks / Trade-offs

- [Clés Cloudflare indisponibles au démarrage] → chargement paresseux et mis en cache ; échec de validation = 401, pas de crash.
- [`-Xmx` trop bas, OOM lors d'une restauration ou d'un décodage d'image] → traitements en flux et sous-échantillonnage (changes suivants) ; valeur surchargeable.
- [Service worker servant une version obsolète] → cache limité aux statiques, versionné par un nom de cache incrémenté à chaque release.
- [Exception `/fin/**` mal bornée] → test dédié vérifiant que seul ce préfixe est exempté, et fiche `docs/fin-de-partie-qr.md`.
- [Même schéma SQLite/PostgreSQL] → types JPA standards uniquement, tests sur H2 (déjà au `pom.xml`) et revue manuelle en `dev`.

## Migration Plan

Premier déploiement : aucune donnée existante. Cloudflare : créer l'application Access (politique Allow) puis la règle Bypass `/fin/*` (voir `docs/fin-de-partie-qr.md`). Retour arrière : retirer l'image ; aucun schéma à défaire.
