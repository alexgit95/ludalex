## MODIFIED Requirements

### Requirement: Interface Thymeleaf et HTMX
Le système SHALL rendre ses pages côté serveur avec Thymeleaf, utiliser HTMX pour les mises à jour partielles, une CSS maison, et ne dépendre d'aucune chaîne de build front. La route `/` SHALL rediriger vers la liste de la ludothèque sur `/jeux`.

#### Scenario: Page d'accueil
- **WHEN** l'utilisateur ouvre `/`
- **THEN** le système le redirige vers `/jeux`, qui affiche la liste paginée de la ludothèque avec la navigation principale

#### Scenario: Lancement de l'application installée
- **WHEN** l'utilisateur lance l'application PWA dont le `start_url` est `/`
- **THEN** il arrive sur la liste paginée de la ludothèque après la redirection vers `/jeux`
