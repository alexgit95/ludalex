## ADDED Requirements

### Requirement: Indiquer les variantes proposées par un jeu
Le formulaire de création et de modification d'un jeu SHALL permettre d'indiquer si le jeu propose plusieurs campagnes, enquêtes ou versions. Par défaut, cet indicateur SHALL être désactivé.

#### Scenario: Nouveau jeu sans variantes
- **WHEN** l'utilisateur crée un jeu sans activer l'indicateur de campagnes, enquêtes ou versions
- **THEN** le jeu est enregistré comme ne proposant pas de variantes

#### Scenario: Nouveau jeu avec variantes
- **WHEN** l'utilisateur crée un jeu en activant l'indicateur de campagnes, enquêtes ou versions
- **THEN** le jeu est enregistré comme proposant des variantes et la saisie d'une variante est disponible pour ses parties
