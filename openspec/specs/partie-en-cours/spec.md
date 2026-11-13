# partie-en-cours Specification

## Purpose
Lancement, pause, reprise et fin d'une partie en direct, avec une seule partie active à la fois.

## Requirements

### Requirement: Lancer une partie
Le système SHALL permettre de lancer une partie en sélectionnant un jeu et au moins un joueur actif ; la partie passe au statut « en cours » avec la date du jour.

#### Scenario: Lancement valide
- **WHEN** l'utilisateur lance « Catan » avec Alex, Marie et Paul et qu'aucune partie n'est active
- **THEN** une partie « en cours » est créée et un token de fin de partie est généré

#### Scenario: Joueur anonymisé
- **WHEN** l'utilisateur tente d'inclure un joueur anonymisé
- **THEN** le système refuse

#### Scenario: Aucun joueur
- **WHEN** l'utilisateur lance une partie sans joueur
- **THEN** le système refuse

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

### Requirement: Une seule partie active
Le système SHALL garantir qu'il existe au plus une partie « en cours » ou « en pause » à la fois.

#### Scenario: Lancement avec une partie en cours
- **WHEN** l'utilisateur veut lancer une partie alors qu'une partie est en cours
- **THEN** le système affiche le formulaire de fin de la partie précédente et ne lance la nouvelle qu'après validation de cette fin

#### Scenario: Lancement avec une partie en pause
- **WHEN** l'utilisateur veut lancer une partie alors qu'une partie est en pause
- **THEN** le système impose de terminer d'abord la partie en pause

#### Scenario: Fin validée puis lancement
- **WHEN** l'utilisateur renseigne la fin de la partie précédente et valide
- **THEN** la partie précédente est terminée et la nouvelle partie est lancée

#### Scenario: Fin abandonnée
- **WHEN** l'utilisateur abandonne le formulaire de fin
- **THEN** la partie précédente reste active et aucune nouvelle partie n'est créée

#### Scenario: Lancements simultanés
- **WHEN** deux lancements arrivent simultanément
- **THEN** une seule partie est créée

### Requirement: Mettre en pause
Le système SHALL permettre de mettre en pause une partie en cours en indiquant obligatoirement le joueur à qui c'était de jouer, parmi les joueurs de la partie.

#### Scenario: Pause valide
- **WHEN** l'utilisateur met la partie en pause en désignant Marie
- **THEN** la partie passe « en pause », Marie est enregistrée comme joueur suivant et les tokens de fin de partie actifs sont invalidés

#### Scenario: Joueur suivant manquant
- **WHEN** l'utilisateur met en pause sans désigner de joueur
- **THEN** le système refuse

#### Scenario: Joueur hors partie
- **WHEN** le joueur désigné ne participe pas à la partie
- **THEN** le système refuse

### Requirement: Reprendre une partie
Le système SHALL permettre de reprendre une partie en pause : elle repasse « en cours », le joueur suivant est effacé et un nouveau token est généré.

#### Scenario: Reprise
- **WHEN** l'utilisateur reprend la partie en pause
- **THEN** la partie est « en cours » et un nouveau token valide existe

### Requirement: Terminer une partie
Le système SHALL permettre de terminer une partie en cours ou en pause en désignant au moins un gagnant (jeu compétitif) ou un résultat victoire/défaite (jeu coopératif), avec des scores facultatifs ; la partie devient « terminée » avec l'horodatage de fin.

#### Scenario: Fin compétitive
- **WHEN** l'utilisateur termine la partie en désignant Alex et Marie gagnants, sans score
- **THEN** la partie est terminée, Alex et Marie sont gagnants, les scores restent vides

#### Scenario: Fin coopérative
- **WHEN** l'utilisateur termine une partie coopérative en victoire
- **THEN** tous les participants sont marqués gagnants

#### Scenario: Aucun gagnant
- **WHEN** l'utilisateur valide la fin d'une partie compétitive sans gagnant
- **THEN** le système refuse

#### Scenario: Tokens invalidés
- **WHEN** une partie est terminée
- **THEN** tous ses tokens sont invalidés

### Requirement: Partie active consultable
Le système SHALL exposer la partie active (jeu, joueurs, statut, joueur suivant) aux écrans de l'application.

#### Scenario: Consultation
- **WHEN** l'application demande la partie active
- **THEN** elle obtient la partie en cours ou en pause, ou aucune
