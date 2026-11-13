## ADDED Requirements

### Requirement: Version du build visible dans l'administration
La page d'administration SHALL afficher la version de l'application issue du POM. Si un SHA de commit connu est disponible dans les métadonnées du build, la page SHALL afficher aussi ses sept premiers caractères après la version, au format `<version> - <sha-court>`. Si le SHA est absent, vide ou égal à `unknown`, la page SHALL afficher la version seule.

#### Scenario: SHA de commit disponible
- **WHEN** l'utilisateur ouvre `/admin` et que les métadonnées du build contiennent un SHA connu
- **THEN** la page affiche la version du POM suivie d'un tiret et des sept premiers caractères du SHA

#### Scenario: SHA de commit indisponible
- **WHEN** l'utilisateur ouvre `/admin` et que le SHA vaut `unknown`, est vide ou absent
- **THEN** la page affiche la version du POM sans suffixe de commit
