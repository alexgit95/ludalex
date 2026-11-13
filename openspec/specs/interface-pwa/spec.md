# interface-pwa Specification

## Purpose
Interface Thymeleaf/HTMX et application installable (PWA).

## Requirements

### Requirement: Interface Thymeleaf et HTMX
Le système SHALL rendre ses pages côté serveur avec Thymeleaf, utiliser HTMX pour les mises à jour partielles, une CSS maison, et ne dépendre d'aucune chaîne de build front. La route `/` SHALL rediriger vers la liste de la ludothèque sur `/jeux`.

#### Scenario: Page d'accueil
- **WHEN** l'utilisateur ouvre `/`
- **THEN** le système le redirige vers `/jeux`, qui affiche la liste paginée de la ludothèque avec la navigation principale

#### Scenario: Lancement de l'application installée
- **WHEN** l'utilisateur lance l'application PWA dont le `start_url` est `/`
- **THEN** il arrive sur la liste paginée de la ludothèque après la redirection vers `/jeux`

### Requirement: Application installable
Le système SHALL exposer un manifeste PWA et un service worker minimal rendant l'application installable.

#### Scenario: Manifeste
- **WHEN** le navigateur demande `/manifest.webmanifest`
- **THEN** le système répond avec un manifeste valide (nom, icônes, `display: standalone`)

#### Scenario: Ressources PWA sans JWT
- **WHEN** le navigateur demande le manifeste ou le service worker en `prod`
- **THEN** il est servi avec les mêmes exigences d'accès que le reste de l'application

### Requirement: Page d'erreur sobre
Le système SHALL afficher des pages d'erreur sans détail technique (pas de trace d'exécution).

#### Scenario: Erreur 404
- **WHEN** l'utilisateur ouvre une URL inconnue
- **THEN** une page 404 lisible est affichée sans trace d'exécution
