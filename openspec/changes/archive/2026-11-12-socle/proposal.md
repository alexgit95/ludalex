## Why

Le dépôt ne contient qu'un `pom.xml` (invalide : balise `<version>` mal fermée), un `Dockerfile` incomplet (pas de `-Xmx`, pas de mode headless, `mvnw` absent) et aucun code. Toutes les fonctionnalités suivantes (images, jeux, parties, affichage, sauvegarde) reposent sur une base commune : profils d'exécution, sécurité Cloudflare Access, interface Thymeleaf/HTMX et PWA.

## What Changes

- Corriger le `pom.xml` (version valide `0.1.0`) et poser l'application Spring Boot (`com.alexgit95.ludalex`).
- Deux profils Spring uniquement : `dev` (SQLite, HTTP, **sans authentification**) et `prod` (PostgreSQL, HTTPS via Cloudflare, cookies `Secure`, **Cloudflare Access obligatoire**).
- Sécurité : validation du JWT Cloudflare (`Cf-Access-Jwt-Assertion`) en `prod`, CSRF actif, rejet 401/403, exception unique pour `/fin/**` (le contrôleur lui-même sera livré par `partie-en-cours`).
- Interface : gabarit Thymeleaf commun, HTMX, CSS maison, PWA minimale (manifest + service worker).
- Conteneurisation : `Dockerfile` arm64-compatible avec `-Xmx`, headless, volume d'images ; `docker-compose.yml` (application + PostgreSQL).
- Documentation : `README.md`, `CHANGELOG.md`, `docs/database.puml` (squelette), tests de sécurité.

## Capabilities

### New Capabilities
- `acces-securise`: vérification de l'accès (Cloudflare Access en `prod`, libre en `dev`), CSRF, exception `/fin/**`.
- `profils-execution`: profils `dev` / `prod`, configuration, variables d'environnement, conteneurisation et limites de ressources.
- `interface-pwa`: gabarit Thymeleaf/HTMX, PWA installable, accueil.

### Modified Capabilities
<!-- Aucune : premier change -->

## Impact

- Code : projet Maven, configuration Spring (`application*.properties`), configuration de sécurité, gabarits, ressources statiques.
- Dépendances : celles déjà déclarées au `pom.xml` (Spring Web MVC, JPA, Security + oauth2-jose, Thymeleaf, SQLite, PostgreSQL) ; ajout de HTMX en fichier statique.
- Systèmes : Cloudflare Access (politique Allow sur l'application) ; la règle Bypass `/fin/*` est décrite dans `docs/fin-de-partie-qr.md`.
