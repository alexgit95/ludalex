## MODIFIED Requirements

### Requirement: Grille aérée
La grille des jeux SHALL afficher 2 jeux par ligne sur petit écran et 3 sur grand écran, centrée et de largeur limitée. Le titre de la page et l'action « Ajouter un jeu » SHALL être alignés sur le bord gauche de cette grille.

#### Scenario: Petit écran
- **WHEN** la largeur est inférieure à 40 rem
- **THEN** deux jeux par ligne au plus et le titre ainsi que l'action principale sont alignés sur le bord gauche de la grille centrée

#### Scenario: Grand écran
- **WHEN** la largeur est supérieure ou égale à 40 rem
- **THEN** trois jeux par ligne au plus, la grille est centrée et le titre ainsi que l'action principale sont alignés sur son bord gauche
