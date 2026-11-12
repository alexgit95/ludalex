## ADDED Requirements

### Requirement: Champion visible sous le titre du jeu
La liste des jeux SHALL afficher, sous le titre de chaque jeu, le ou les champions actuels sur un petit parchemin, avec une coupe aux couleurs du thème, l'avatar et le nom de chacun, en retournant à la ligne si nécessaire, et en réservant la même hauteur pour les jeux sans champion.

#### Scenario: Un champion
- **WHEN** un jeu a un champion
- **THEN** son avatar et son nom s'affichent avec la coupe sous le titre, sur un fond parchemin lisible

#### Scenario: Égalité
- **WHEN** plusieurs joueurs sont champions à égalité
- **THEN** tous les noms s'affichent, à la ligne si l'espace manque

#### Scenario: Sans champion
- **WHEN** un jeu n'a pas de champion
- **THEN** l'espace est réservé et les vignettes restent alignées

### Requirement: Grille aérée
La grille des jeux SHALL afficher 2 jeux par ligne sur petit écran et 3 sur grand écran, centrée et de largeur limitée.

#### Scenario: Petit écran
- **WHEN** la largeur est inférieure à 40 rem
- **THEN** deux jeux par ligne au plus

#### Scenario: Grand écran
- **WHEN** la largeur est supérieure ou égale à 40 rem
- **THEN** trois jeux par ligne au plus, grille centrée
