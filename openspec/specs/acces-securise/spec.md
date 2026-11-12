# acces-securise Specification

## Purpose
Vérification de l'accès (Cloudflare Access en prod, libre en dev), CSRF et exception /fin/**.

## Requirements

### Requirement: Validation du JWT Cloudflare en prod
En profil `prod`, le système SHALL exiger sur toute requête un JWT Cloudflare Access valide dans le header `Cf-Access-Jwt-Assertion` : signature vérifiée avec les clés publiques du `CF_ACCESS_TEAM_DOMAIN`, date d'expiration, émetteur et audience égale à `CF_ACCESS_AUD`.

#### Scenario: JWT valide
- **WHEN** une requête en `prod` porte un JWT correctement signé, non expiré, avec le bon émetteur et la bonne audience
- **THEN** la requête est traitée normalement

#### Scenario: JWT absent
- **WHEN** une requête en `prod` ne porte pas le header `Cf-Access-Jwt-Assertion`
- **THEN** le système répond 401 et ne traite pas la requête

#### Scenario: JWT expiré
- **WHEN** une requête en `prod` porte un JWT dont la date d'expiration est dépassée
- **THEN** le système répond 401

#### Scenario: Mauvaise audience
- **WHEN** une requête en `prod` porte un JWT valide dont l'audience diffère de `CF_ACCESS_AUD`
- **THEN** le système répond 403

#### Scenario: Signature invalide
- **WHEN** une requête en `prod` porte un JWT signé par une clé qui n'appartient pas au `CF_ACCESS_TEAM_DOMAIN`
- **THEN** le système répond 401

### Requirement: Aucune décision sur donnée non signée
Le système MUST NOT prendre de décision d'accès à partir d'un header, d'un cookie ou d'un paramètre non signé, et MUST NOT stocker d'identité (email) issue du JWT.

#### Scenario: Header d'identité forgé
- **WHEN** une requête en `prod` porte un header `Cf-Access-Authenticated-User-Email` mais pas de JWT valide
- **THEN** le système répond 401

### Requirement: Aucune authentification en dev
En profil `dev`, le système SHALL autoriser tout accès sans login ni JWT.

#### Scenario: Accès libre en dev
- **WHEN** une requête arrive en profil `dev` sans aucun header d'authentification
- **THEN** la requête est traitée normalement

### Requirement: Vérification Cloudflare non désactivable en prod
Le système MUST refuser de démarrer en profil `prod` si `CF_ACCESS_TEAM_DOMAIN` ou `CF_ACCESS_AUD` est absent, et MUST NOT permettre de désactiver la vérification Cloudflare par configuration en `prod`.

#### Scenario: Variable manquante
- **WHEN** l'application démarre en `prod` sans `CF_ACCESS_AUD`
- **THEN** le démarrage échoue avec un message explicite

#### Scenario: Tentative de désactivation
- **WHEN** l'application démarre en `prod` avec un mode d'accès différent de `cloudflare`
- **THEN** le démarrage échoue

### Requirement: Exception pour la fin de partie
En profil `prod`, le système SHALL exempter de la validation du JWT uniquement les chemins `/fin/**`, et exiger un JWT valide sur tous les autres chemins.

#### Scenario: Chemin de fin de partie sans JWT
- **WHEN** une requête sans JWT vise `/fin/<token>` en `prod`
- **THEN** le filtre de validation du JWT ne la rejette pas (l'autorisation relève du token)

#### Scenario: Autre chemin sans JWT
- **WHEN** une requête sans JWT vise `/` ou `/jeux` en `prod`
- **THEN** le système répond 401

### Requirement: Protection CSRF
Le système SHALL activer la protection CSRF sur toutes les requêtes modifiantes (POST, PUT, PATCH, DELETE), y compris sur les formulaires HTMX.

#### Scenario: Requête modifiante sans jeton CSRF
- **WHEN** un POST arrive sans jeton CSRF valide
- **THEN** le système répond 403

#### Scenario: Requête HTMX avec jeton
- **WHEN** une requête HTMX modifiante porte le jeton CSRF en header
- **THEN** elle est acceptée
