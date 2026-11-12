# declaration-parties Specification

## Purpose
Déclaration, modification, suppression et liste des parties terminées, scores, gagnants et lieux avec autocomplétion.

## Requirements

### Requirement: Déclaration d'une partie terminée
Le système SHALL permettre de déclarer une partie terminée en sélectionnant un jeu, au moins un joueur actif, une date, un lieu facultatif, au moins un gagnant (jeu compétitif) et des scores facultatifs.

#### Scenario: Partie compétitive valide
- **WHEN** l'utilisateur déclare une partie de « Catan » avec Alex, Marie et Paul, gagnant Marie, scores 8, 10 et 6
- **THEN** la partie est enregistrée avec les trois participations, Marie marquée gagnante

#### Scenario: Date par défaut
- **WHEN** l'utilisateur ouvre le formulaire de déclaration
- **THEN** la date proposée est celle du jour

#### Scenario: Date future
- **WHEN** l'utilisateur saisit une date postérieure à aujourd'hui
- **THEN** le système refuse

#### Scenario: Aucun gagnant
- **WHEN** l'utilisateur valide une partie compétitive sans gagnant
- **THEN** le système refuse

#### Scenario: Plusieurs gagnants
- **WHEN** l'utilisateur désigne deux gagnants
- **THEN** les deux participations sont marquées gagnantes

#### Scenario: Gagnant hors participants
- **WHEN** un gagnant ne figure pas parmi les joueurs de la partie
- **THEN** le système refuse

#### Scenario: Joueur en double
- **WHEN** le même joueur est ajouté deux fois à la partie
- **THEN** le système refuse

#### Scenario: Joueur anonymisé
- **WHEN** l'utilisateur cherche à ajouter un joueur anonymisé
- **THEN** il n'est pas proposé et la soumission le contenant est refusée

### Requirement: Scores facultatifs
Le système SHALL accepter des scores numériques entiers, saisis pour tout ou partie des joueurs, ou aucun.

#### Scenario: Aucun score
- **WHEN** l'utilisateur déclare une partie sans score
- **THEN** la partie est enregistrée avec des scores vides

#### Scenario: Score non numérique
- **WHEN** l'utilisateur saisit « abc » comme score
- **THEN** le système refuse

### Requirement: Lieu en texte libre avec autocomplétion
Le système SHALL accepter un lieu facultatif en texte libre (100 caractères maximum) et proposer, pendant la saisie, les lieux déjà utilisés dans des parties précédentes.

#### Scenario: Suggestions
- **WHEN** l'utilisateur tape « Mai » et qu'une partie existe au lieu « Maison de Marie »
- **THEN** « Maison de Marie » est proposé

#### Scenario: Aucune suggestion
- **WHEN** aucun lieu connu ne correspond à la saisie
- **THEN** aucune suggestion n'est affichée et la saisie libre reste possible

#### Scenario: Nombre limité de suggestions
- **WHEN** plus de dix lieux correspondent
- **THEN** au plus dix suggestions sont renvoyées, sans doublon

#### Scenario: Lieu trop long
- **WHEN** l'utilisateur saisit un lieu de plus de 100 caractères
- **THEN** le système refuse

### Requirement: Liste des parties
Le système SHALL afficher la liste paginée des parties, de la plus récente à la plus ancienne, avec filtre par jeu.

#### Scenario: Liste filtrée
- **WHEN** l'utilisateur filtre sur « Catan »
- **THEN** seules les parties de ce jeu sont listées, par page

### Requirement: Modification et suppression d'une partie
Le système SHALL permettre de modifier une partie terminée avec les mêmes règles de validité qu'à la création, et de la supprimer après confirmation.

#### Scenario: Correction du gagnant
- **WHEN** l'utilisateur change le gagnant d'une partie terminée
- **THEN** la modification est enregistrée

#### Scenario: Suppression confirmée
- **WHEN** l'utilisateur confirme la suppression d'une partie
- **THEN** la partie, ses participations et ses équipes sont supprimées

### Requirement: Vérification des participations d'un joueur
Le système SHALL indiquer si un joueur a participé à au moins une partie, pour permettre l'anonymisation ou la suppression physique du joueur.

#### Scenario: Joueur avec participation
- **WHEN** le service des joueurs demande si un joueur a des participations
- **THEN** la réponse est affirmative s'il figure dans au moins une partie

### Requirement: Aperçu du jeu choisi
Le formulaire de déclaration d'une partie SHALL afficher l'image et le nom du jeu sélectionné dès le choix, et les retirer si aucun jeu n'est sélectionné.

#### Scenario: Choix d'un jeu
- **WHEN** l'utilisateur sélectionne un jeu
- **THEN** son image et son nom apparaissent dans le formulaire

#### Scenario: Aucun jeu
- **WHEN** aucun jeu n'est sélectionné
- **THEN** aucun aperçu n'est affiché
