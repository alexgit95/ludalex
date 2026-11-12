# gestion-joueurs Specification

## Purpose
Création, modification, liste et anonymisation des joueurs, avec avatar et image de victoire.

## Requirements

### Requirement: Déclaration d'un joueur
Le système SHALL permettre de créer un joueur avec un nom obligatoire, une image d'avatar et une image de victoire, chacune étant un fichier uploadé ou une URL.

#### Scenario: Création complète
- **WHEN** l'utilisateur saisit un nom, uploade un avatar et fournit une URL pour l'image de victoire
- **THEN** le joueur est créé et apparaît dans la liste

#### Scenario: Nom manquant
- **WHEN** l'utilisateur valide le formulaire sans nom
- **THEN** le système refuse et affiche une erreur sur le champ

#### Scenario: Images facultatives
- **WHEN** l'utilisateur crée un joueur sans avatar ni image de victoire
- **THEN** le joueur est créé et une image par défaut est affichée à sa place

### Requirement: Unicité du nom
Le système SHALL refuser deux joueurs actifs de même nom, sans tenir compte de la casse.

#### Scenario: Nom en doublon
- **WHEN** l'utilisateur crée « Alex » alors qu'« alex » existe
- **THEN** le système refuse la création

### Requirement: Modification d'un joueur
Le système SHALL permettre de modifier le nom et les images d'un joueur.

#### Scenario: Remplacement d'un avatar
- **WHEN** l'utilisateur remplace l'avatar uploadé par un autre fichier
- **THEN** l'ancien fichier est supprimé du volume et le nouveau est utilisé

### Requirement: Liste des joueurs
Le système SHALL afficher la liste paginée des joueurs avec leur avatar et leur nom.

#### Scenario: Liste paginée
- **WHEN** l'utilisateur ouvre l'écran Admin
- **THEN** les joueurs sont listés par page, sans chargement massif

### Requirement: Suppression par anonymisation
La suppression d'un joueur SHALL l'anonymiser : son nom est remplacé par une mention générique, ses images sont retirées et leurs fichiers supprimés du volume, et ses participations aux parties sont conservées.

#### Scenario: Joueur ayant des parties
- **WHEN** l'utilisateur supprime un joueur qui a participé à 12 parties
- **THEN** le joueur est anonymisé, les 12 parties et leurs résultats restent consultables avec la mention générique

#### Scenario: Joueur sans partie
- **WHEN** l'utilisateur supprime un joueur sans participation
- **THEN** le joueur est supprimé définitivement

#### Scenario: Confirmation
- **WHEN** l'utilisateur demande la suppression
- **THEN** le système demande une confirmation avant d'agir

### Requirement: Joueur anonymisé non sélectionnable
Un joueur anonymisé SHALL NOT être proposé pour une nouvelle partie ni modifiable.

#### Scenario: Sélection pour une partie
- **WHEN** l'utilisateur ouvre la sélection des joueurs d'une nouvelle partie
- **THEN** les joueurs anonymisés n'apparaissent pas
