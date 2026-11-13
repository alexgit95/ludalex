## ADDED Requirements

### Requirement: Variante libre d'une partie terminée
Pour un jeu marqué comme proposant des campagnes, enquêtes ou versions, le formulaire de déclaration ou de modification d'une partie SHALL permettre de saisir un libellé libre facultatif de 200 caractères maximum. Le libellé SHALL être conservé avec la partie et affiché dans l'historique. Désactiver ultérieurement l'indicateur du jeu SHALL conserver les libellés des parties déjà enregistrées.

#### Scenario: Déclarer une partie avec une variante
- **WHEN** l'utilisateur déclare une partie d'un jeu proposant des variantes et saisit un libellé
- **THEN** la partie est enregistrée avec ce libellé et celui-ci apparaît dans l'historique

#### Scenario: Déclarer une partie sans variante
- **WHEN** l'utilisateur déclare une partie d'un jeu proposant des variantes sans renseigner le libellé
- **THEN** la partie est enregistrée sans variante

#### Scenario: Jeu sans variantes
- **WHEN** l'utilisateur déclare une partie d'un jeu ne proposant pas de variantes
- **THEN** aucun champ de variante n'est présenté et la partie est enregistrée sans variante

#### Scenario: Désactiver les variantes du jeu
- **WHEN** l'utilisateur désactive l'indicateur d'un jeu qui possède des parties avec variantes
- **THEN** les variantes des parties existantes restent enregistrées et visibles dans l'historique

#### Scenario: Libellé trop long
- **WHEN** l'utilisateur saisit un libellé de plus de 200 caractères
- **THEN** la soumission est refusée
