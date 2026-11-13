## ADDED Requirements

### Requirement: Variante choisie au lancement d'une partie en direct
Lorsqu'un jeu propose des campagnes, enquêtes ou versions, le formulaire de lancement en direct SHALL permettre de saisir un libellé libre facultatif de 200 caractères maximum. La variante SHALL être enregistrée dès la création de la partie et conservée pendant les pauses, reprises et terminaisons.

#### Scenario: Lancement avec une variante
- **WHEN** l'utilisateur lance une partie en direct d'un jeu proposant des variantes et saisit un libellé
- **THEN** la partie en cours conserve et expose ce libellé jusqu'à sa fin

#### Scenario: Lancement sans variante
- **WHEN** l'utilisateur lance une partie en direct d'un jeu proposant des variantes sans renseigner le libellé
- **THEN** la partie est créée sans variante

#### Scenario: Lancement d'un jeu sans variantes
- **WHEN** l'utilisateur lance une partie d'un jeu ne proposant pas de variantes
- **THEN** aucun champ de variante n'est présenté et la partie est créée sans variante
