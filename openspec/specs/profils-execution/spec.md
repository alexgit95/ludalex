# profils-execution Specification

## Purpose
Profils dev/prod, configuration, variables d'environnement et conteneurisation sobre pour Raspberry Pi.

## Requirements

### Requirement: Deux profils d'exécution
Le système SHALL proposer uniquement les profils `dev` (défaut) et `prod`, configurés par `application.properties` (commun), `application-dev.properties` et `application-prod.properties`.

#### Scenario: Démarrage sans profil
- **WHEN** l'application démarre sans profil explicite
- **THEN** le profil `dev` est actif

#### Scenario: Profil prod
- **WHEN** l'application démarre avec le profil `prod`
- **THEN** elle utilise PostgreSQL via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

### Requirement: Base SQLite en dev et PostgreSQL en prod
Le système SHALL utiliser SQLite en `dev` et PostgreSQL en `prod`, avec un schéma généré par Hibernate (`ddl-auto`) et sans outil de migration.

#### Scenario: Schéma créé en dev
- **WHEN** l'application démarre en `dev` sur une base vide
- **THEN** le schéma est créé par Hibernate sans Liquibase ni Flyway

### Requirement: Cookies et HTTPS en prod
En `prod`, le système SHALL tenir compte des headers `X-Forwarded-*` (terminaison HTTPS par Cloudflare) et émettre les cookies de session avec l'attribut `Secure`.

#### Scenario: Cookie Secure
- **WHEN** une session est créée en `prod`
- **THEN** le cookie de session porte les attributs `Secure` et `HttpOnly`

### Requirement: Variables d'environnement obligatoires en prod
En `prod`, le système SHALL exiger `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CF_ACCESS_TEAM_DOMAIN` et `CF_ACCESS_AUD`.

#### Scenario: Variable absente
- **WHEN** `DB_URL` est absente en `prod`
- **THEN** le démarrage échoue avec un message nommant la variable manquante

### Requirement: Image Docker sobre pour Raspberry Pi
L'image Docker SHALL cibler `arm64`, limiter le tas JVM avec `-Xmx`, exécuter la JVM en mode headless, tourner avec un utilisateur non privilégié et monter un volume pour les images uploadées.

#### Scenario: Limite mémoire
- **WHEN** le conteneur démarre
- **THEN** la JVM est lancée avec `-Xmx` et `-Djava.awt.headless=true`

#### Scenario: Orchestration locale
- **WHEN** on lance `docker-compose up`
- **THEN** l'application démarre en `prod` avec un conteneur PostgreSQL et un volume pour les images
