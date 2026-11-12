## 1. Projet Maven

- [x] 1.1 Corriger le `pom.xml` (balise `<version>` valide, version `0.1.0`)
- [x] 1.2 Créer la classe `LudalexApplication` dans `com.alexgit95.ludalex`
- [x] 1.3 Vérifier que le build passe avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
- [x] 1.4 Régler la question du `mvnw`/`.mvn` référencé par le `Dockerfile` (génération ou build alternatif)

## 2. Profils et configuration

- [x] 2.1 Créer `application.properties`, `application-dev.properties` et `application-prod.properties`
- [x] 2.2 Configurer SQLite en `dev` (dialecte communautaire) et PostgreSQL en `prod` avec `ddl-auto`
- [x] 2.3 Activer les headers `X-Forwarded-*` et les cookies `Secure`/`HttpOnly` en `prod`
- [x] 2.4 Valider au démarrage les variables obligatoires de `prod` et refuser un mode d'accès autre que `cloudflare`
- [x] 2.5 Écrire les tests de configuration par profil

## 3. Sécurité

- [x] 3.1 Implémenter le décodeur JWT (JWK du `CF_ACCESS_TEAM_DOMAIN`, émetteur, expiration, audience)
- [x] 3.2 Créer la chaîne de sécurité `prod` (JWT exigé, `/fin/**` exempté, CSRF actif)
- [x] 3.3 Créer la chaîne de sécurité `dev` (accès libre, CSRF actif)
- [x] 3.4 Renvoyer 401/403 sans détail technique
- [x] 3.5 Tests : JWT valide, expiré, mauvaise audience, signature invalide, absent, header d'identité forgé, accès libre en `dev`, exception limitée à `/fin/**`, CSRF

## 4. Interface

- [x] 4.1 Créer le gabarit Thymeleaf commun (en-tête, navigation, pied) et la page d'accueil
- [x] 4.2 Ajouter HTMX en fichier statique et le jeton CSRF dans les requêtes HTMX
- [x] 4.3 Écrire la CSS maison de base (mobile d'abord)
- [x] 4.4 Ajouter le manifeste PWA, les icônes et le service worker minimal
- [x] 4.5 Ajouter les pages d'erreur sobres (404, 403, 500)

## 5. Conteneurisation

- [x] 5.1 Mettre à jour le `Dockerfile` : `-Xmx`, `-XX:+UseSerialGC`, `-Djava.awt.headless=true`, volume `/data/images`
- [x] 5.2 Créer `docker-compose.yml` (application + PostgreSQL, volumes, variables via `.env`)
- [x] 5.3 Ajouter `.env.example` sans secret et ignorer `.env` dans `.gitignore`

## 6. Documentation et livraison

- [x] 6.1 Créer `docs/database.puml` (squelette vide)
- [x] 6.2 Créer `README.md` (présentation, profils, variables, lancement local et Docker, Cloudflare)
- [x] 6.3 Créer `CHANGELOG.md` (format Keep a Changelog) avec la version `0.1.0`, identique à celle du `pom.xml`
- [x] 6.4 Lancer `mvn test` et corriger jusqu'au vert
