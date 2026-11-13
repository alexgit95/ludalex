# Instructions Générales du Projet

## Contexte
- **Application :** PWA **mono-utilisateur** de gestion de ludothèque personnelle (jeux, prêts, parties, statistiques, écran d'affichage). Les joueurs sont de simples fiches déclarées dans l'application, sans compte. Cible de déploiement : Raspberry Pi (ressources limitées). Cadre d'entreprise : respecter l'architecture en place et ne pas régresser sur le déploiement.
- **Backend :** Java 25, Spring Boot 4.x, Spring Security (session cookie + CSRF), Spring Data JPA.
- **Front :** Thymeleaf + HTMX, CSS maison, PWA (manifest + service worker minimal). Pas de SPA, pas de chaîne de build front.
- **Conteneurisation :** Docker (Dockerfile + docker-compose.yml).
- **Spécifications :** le dossier `openspec/` fait foi (changes, specs, design, tasks).

## Profils Spring (deux modes uniquement)
- `dev` (défaut) : développement local, **SQLite**, HTTP, sans Docker, **sans authentification**.
- `prod` : Docker + **PostgreSQL**, HTTPS terminé par Cloudflare (`X-Forwarded-*`), cookies de session `Secure`, authentification par **Cloudflare Access** (`ludalex.acces.mode=cloudflare`). Variables : `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD` (obligatoires).
- Configuration : `application.properties` (commun), `application-dev.properties`, `application-prod.properties`.

## Authentification
- L'authentification est **entièrement déléguée à Cloudflare Access** : l'application ne gère ni mot de passe, ni formulaire de login, ni compte utilisateur, ni rôle (application mono-utilisateur).
- Le rôle de l'application se limite à vérifier que la requête provient d'un utilisateur authentifié par Cloudflare et habilité à accéder à cette application :
  - validation du JWT Cloudflare (header `Cf-Access-Jwt-Assertion`) : signature (clés publiques du `CF_ACCESS_TEAM_DOMAIN`), expiration, émetteur et audience (`CF_ACCESS_AUD`) ;
  - rejet (401/403) de toute requête sans JWT valide ou dont l'audience ne correspond pas à l'application ;
  - aucune décision ne repose sur un header ou paramètre non signé ; aucune identité (email) n'est stockée.
- **Seule exception en `prod` : le lien/QR de fin de partie** (`/fin/{token}`), accessible sans JWT depuis n'importe quel appareil. Le token est l'unique autorisation : aléatoire (≥ 128 bits), stocké haché, rattaché à une seule partie, **à usage unique**, expirant, limité en débit, et ne permet que de désigner le gagnant ou de mettre la partie en pause. Il ne doit exposer aucune autre donnée. Une règle Cloudflare Access de type Bypass limitée à ce chemin est requise : voir `docs/fin-de-partie-qr.md`.
- **`prod` = Cloudflare Access ; `dev` (local) = aucune authentification** : tout accès est autorisé sans login ni JWT. Ce mode ne doit jamais être actif en `prod` (la vérification Cloudflare est obligatoire et ne peut pas être désactivée par configuration).
- Toute modification de ce mécanisme doit être couverte par des tests (JWT valide, expiré, mauvaise audience, absent en `prod` ; accès libre en `dev` ; token de fin de partie valide, expiré, déjà utilisé, d'une autre partie).

## Images et sauvegarde
- Images uploadées : stockées sur un **volume** (jamais en base ni en mémoire complète). Images par URL : URL prioritaire et copie locale de secours sur ce volume, téléchargée automatiquement en arrière-plan avec suivi admin, SSRF, format et taille contrôlés ; repli sur toute erreur de chargement d'une ressource image uniquement.
- Toute URL d'image saisie est protégée contre la SSRF (y compris le téléchargement ponctuel servant au calcul de la couleur de fond).
- La couleur de fond d'un jeu est calculée **une seule fois à l'enregistrement** (image réduite par sous-échantillonnage, classe de couleur la plus fréquente) et stockée ; modifiable manuellement ; couleur neutre en cas d'échec. Une bibliothèque tierce gère le format WebP.
- Sauvegarde/restauration : un seul ZIP (données indépendantes du moteur + images uploadées) téléchargeable puis ré-uploadable **sans modification** ; la restauration remplace tout l'existant, protégée par confirmation et CSRF, traitée en flux, avec contrôle du contenu (zip slip, taille, types).

## CI/CD
- Le déploiement est automatisé par la GitHub Action de `.github/workflows/`. Ne propose aucune modification des pipelines sans demande explicite.

## Base de données
- Aucun outil de migration (pas de Liquibase/Flyway) : le schéma est généré par Hibernate (`ddl-auto`).
- Les entités `@Entity` doivent rester 100 % compatibles **SQLite** et **PostgreSQL** (types et SQL standards uniquement).
- **Modélisation (PlantUML) :** à chaque modification de la structure (entités, colonnes, relations, contraintes), mettre à jour `docs/database.puml`.

## Règles de codage
### 1. Sécurité
- CSRF actif. Les données non autorisées ne doivent jamais être envoyées au client (le token de fin de partie n'expose que sa partie).
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
- Cible Raspberry Pi (arm64) : limiter le tas JVM avec `-Xmx` dans le Dockerfile, JVM en mode headless, statistiques par requêtes agrégées, décodage d'image avec sous-échantillonnage.
