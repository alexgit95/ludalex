# Instructions Générales du Projet

## Contexte
- **Application :** PWA de gestion de ludothèque (catalogue de jeux, prêts/emprunts, utilisateurs). Cadre d'entreprise : respecter l'architecture en place et ne pas régresser sur le déploiement.
- **Backend :** Java 25, Spring Boot 4.x, Spring Security (session cookie + CSRF), Spring Data JPA.
- **Front :** Thymeleaf + HTMX, CSS maison, PWA (manifest + service worker minimal). Pas de SPA, pas de chaîne de build front.
- **Conteneurisation :** Docker (Dockerfile + docker-compose.yml).
- **Spécifications :** le dossier `openspec/` fait foi (changes, specs, design, tasks).

## Profils Spring (deux modes uniquement)
- `dev` (défaut) : développement local, **SQLite**, HTTP, sans Docker, **sans authentification**.
- `prod` : Docker + **PostgreSQL**, HTTPS terminé par Cloudflare (`X-Forwarded-*`), cookies de session `Secure`, authentification par **Cloudflare Access** (`ludalex.acces.mode=cloudflare`). Variables : `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD`, `EMAIL_HMAC_SECRET` (obligatoires) .
- Configuration : `application.properties` (commun), `application-dev.properties`, `application-prod.properties`.

## Authentification
- L'authentification est **entièrement déléguée à Cloudflare Access** : l'application ne gère ni mot de passe, ni formulaire de login, ni création de compte local en `prod`.
- Le rôle de l'application se limite à vérifier que la requête provient d'un utilisateur authentifié par Cloudflare et habilité à accéder à cette application :
  - validation du JWT Cloudflare (header `Cf-Access-Jwt-Assertion`) : signature (clés publiques du `CF_ACCESS_TEAM_DOMAIN`), expiration, émetteur et audience (`CF_ACCESS_AUD`) ;
  - rejet (401/403) de toute requête sans JWT valide ou dont l'audience ne correspond pas à l'application ;
  - identité de l'utilisateur déduite uniquement des claims du JWT validé (email), jamais d'un header ou paramètre non signé.
- L'email est stocké sous forme hachée (HMAC avec `EMAIL_HMAC_SECRET`) afin d'identifier l'utilisateur de façon stable sans conserver l'adresse en clair.
- Les droits fonctionnels (rôles, ex. admin / utilisateur) restent gérés côté serveur, en complément de cette vérification.
- **`prod` = Cloudflare Access ; `dev` (local) = aucune authentification** : tout accès est autorisé sans login ni JWT. Ce mode ne doit jamais être actif en `prod` (la vérification Cloudflare est obligatoire et ne peut pas être désactivée par configuration).
- Toute modification de ce mécanisme doit être couverte par des tests (JWT valide, expiré, mauvaise audience, absent en `prod` ; accès libre en `dev`).

## CI/CD
- Le déploiement est automatisé par la GitHub Action de `.github/workflows/`. Ne propose aucune modification des pipelines sans demande explicite.

## Base de données
- Aucun outil de migration (pas de Liquibase/Flyway) : le schéma est généré par Hibernate (`ddl-auto`).
- Les entités `@Entity` doivent rester 100 % compatibles **SQLite** et **PostgreSQL** (types et SQL standards uniquement).
- **Modélisation (PlantUML) :** à chaque modification de la structure (entités, colonnes, relations, contraintes), mettre à jour `docs/database.puml`.

## Règles de codage
### 1. Sécurité
- CSRF actif, droits vérifiés côté serveur (rôles). Les données non autorisées ne doivent jamais être envoyées au client.
- Toute requête sortante vers une URL saisie par l'utilisateur est protégée contre la SSRF.

### 2. Documentation API
- Si un contrôleur REST est créé ou modifié, ajouter les annotations OpenAPI 3 (`@Tag`, `@Operation`, `@ApiResponse`) avec codes HTTP et DTO. Les contrôleurs MVC rendant du HTML n'y sont pas soumis.

### 3. Tests unitaires obligatoires
- À chaque modification ou ajout de code Java, écrire ou mettre à jour les tests (JUnit 5 + Mockito), avec une couverture complète des nouvelles règles métier.
- Exécuter Maven exclusivement avec le binaire de **`C:\USINE_LOGICIELLE\apache-maven\bin`**.

### 4. Documentation fonctionnelle
- À chaque modification de fonctionnalité, mettre à jour `README.md` à la racine, de façon claire et structurée.

### 5. Journal des modifications
- À chaque tâche de code terminée, mettre à jour `CHANGELOG.md` (format Keep a Changelog : `[Added]`, `[Changed]`, `[Fixed]`) en décrivant l'impact technique et fonctionnel, et incrémenter la version dans le `pom.xml` et dans le changelog à **chaque modification** (les deux versions doivent rester identiques).
- Signaler explicitement tout **breaking change** (API, schéma de base, configuration, variables d'environnement, comportement fonctionnel) dans une rubrique `[BREAKING CHANGE]` du changelog, avec la marche à suivre pour migrer ; il implique une incrémentation de version majeure.

### 6. Ressources
- Code sobre en mémoire : pagination JPA, pas de chargement massif de collections, pas de fuite mémoire.
