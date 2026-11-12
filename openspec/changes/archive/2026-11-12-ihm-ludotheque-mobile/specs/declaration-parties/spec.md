## ADDED Requirements

### Requirement: Aperçu du jeu choisi
Le formulaire de déclaration d'une partie SHALL afficher l'image et le nom du jeu sélectionné dès le choix, et les retirer si aucun jeu n'est sélectionné.

#### Scenario: Choix d'un jeu
- **WHEN** l'utilisateur sélectionne un jeu
- **THEN** son image et son nom apparaissent dans le formulaire

#### Scenario: Aucun jeu
- **WHEN** aucun jeu n'est sélectionné
- **THEN** aucun aperçu n'est affiché
