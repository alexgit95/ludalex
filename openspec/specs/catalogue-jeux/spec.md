# catalogue-jeux Specification

## Purpose
Gestion des jeux de la ludothèque, liste en vignettes et fiche détail.

## Requirements

### Requirement: Déclaration d'un jeu
Le système SHALL permettre de créer un jeu avec un nom obligatoire, une image (upload ou URL), un prix, une date d'achat et un indicateur coopératif.

#### Scenario: Création valide
- **WHEN** l'utilisateur saisit « Catan », uploade une image, indique 45,00 €, le 2025-03-12, non coopératif
- **THEN** le jeu est créé et la couleur de fond est calculée et stockée

#### Scenario: Nom manquant
- **WHEN** l'utilisateur valide sans nom
- **THEN** le système refuse avec une erreur sur le champ

#### Scenario: Prix négatif
- **WHEN** l'utilisateur saisit un prix négatif
- **THEN** le système refuse

#### Scenario: Date d'achat future
- **WHEN** l'utilisateur saisit une date d'achat postérieure à aujourd'hui
- **THEN** le système refuse

### Requirement: Modification et suppression
Le système SHALL permettre de modifier un jeu et de le supprimer après confirmation.

#### Scenario: Suppression d'un jeu sans partie
- **WHEN** l'utilisateur confirme la suppression d'un jeu sans partie
- **THEN** le jeu, son historique de prêts et son image uploadée sont supprimés

#### Scenario: Suppression d'un jeu avec parties
- **WHEN** l'utilisateur tente de supprimer un jeu qui a des parties
- **THEN** le système refuse et explique qu'il faut d'abord supprimer ou conserver l'historique des parties

### Requirement: Liste en vignettes
Le système SHALL afficher tous les jeux sous forme de vignettes paginées : image, titre dessous, et emplacement du symbole de champion.

#### Scenario: Liste des jeux
- **WHEN** l'utilisateur ouvre la ludothèque
- **THEN** il voit les jeux par page avec leur image et leur titre

#### Scenario: Jeu sans image
- **WHEN** un jeu n'a pas d'image
- **THEN** une image par défaut est affichée

#### Scenario: Jeu prêté
- **WHEN** un jeu est actuellement prêté
- **THEN** la vignette indique le nom de l'emprunteur

### Requirement: Fiche détail
Le système SHALL afficher pour un jeu la date d'achat, le prix, le caractère coopératif et la zone des statistiques de parties.

#### Scenario: Ouverture de la fiche
- **WHEN** l'utilisateur clique sur un jeu de la liste
- **THEN** la fiche affiche la date d'achat, le prix, le type du jeu et l'historique des prêts

### Requirement: Couleur de fond modifiable
Le système SHALL permettre de corriger la couleur de fond d'un jeu.

#### Scenario: Correction manuelle
- **WHEN** l'utilisateur choisit une couleur dans le formulaire du jeu
- **THEN** cette couleur remplace celle qui avait été calculée

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
