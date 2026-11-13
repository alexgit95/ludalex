## Why

Afficher l'identifiant du build dans l'administration permet de confirmer rapidement quelle version est déployée et de relier une anomalie au commit correspondant. Les métadonnées Maven et le SHA injectés en CI existent déjà; ils ne sont simplement pas visibles dans l'application.

## What Changes

- Afficher sur la page d'administration la version issue du POM suivie des 7 premiers caractères du SHA lorsque celui-ci est disponible, au format `<version> - <sha-court>`.
- En l'absence de SHA (exécution locale où `git.commit` vaut `unknown`), afficher la version seule.
- Réutiliser les métadonnées de build existantes, sans nouvelle dépendance ni appel Git à l'exécution.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `interface-pwa`: la page d'administration affiche l'identifiant de version du build.

## Impact

- `AdminController` et `templates/admin/index.html` pour exposer et afficher l'information.
- Métadonnées `BuildProperties` déjà générées par le plugin Spring Boot; le build Docker reçoit déjà `GIT_COMMIT` depuis la CI.
- Tests du rendu de la page d'administration.
- README, changelog, version du `pom.xml` et cache du service worker, selon les règles du projet.
- Aucun changement d'API, de schéma de base ou de configuration requis.
