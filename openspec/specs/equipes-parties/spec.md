# equipes-parties Specification

## Purpose
Équipes propres à une partie, victoire d'équipe et résultat des jeux coopératifs.

## Requirements

### Requirement: Équipes propres à une partie
Le système SHALL permettre de regrouper des joueurs d'une partie en équipes ; une équipe n'existe que pour la partie qui la définit.

#### Scenario: Deux équipes
- **WHEN** l'utilisateur crée « Les Rouges » (Alex, Marie) et « Les Bleus » (Paul, Julie) pour une partie
- **THEN** les deux équipes sont rattachées à cette partie uniquement

#### Scenario: Joueur dans deux équipes
- **WHEN** un joueur est affecté à deux équipes de la même partie
- **THEN** le système refuse

#### Scenario: Équipe sans membre
- **WHEN** une équipe est créée sans aucun membre
- **THEN** le système refuse

#### Scenario: Équipe hors partie
- **WHEN** une équipe contient un joueur qui ne participe pas à la partie
- **THEN** le système refuse

### Requirement: Victoire d'équipe
Lorsqu'une équipe est désignée gagnante, le système SHALL marquer comme gagnants tous ses membres.

#### Scenario: Équipe gagnante
- **WHEN** l'utilisateur désigne « Les Rouges » gagnante
- **THEN** Alex et Marie sont marqués gagnants, Paul et Julie ne le sont pas

### Requirement: Jeu coopératif
Pour un jeu coopératif, le système SHALL demander uniquement les joueurs et le résultat « victoire » ou « défaite », sans désignation de gagnant individuel.

#### Scenario: Victoire coopérative
- **WHEN** l'utilisateur déclare une partie coopérative gagnée avec trois joueurs
- **THEN** les trois participations sont marquées gagnantes

#### Scenario: Défaite coopérative
- **WHEN** l'utilisateur déclare une partie coopérative perdue
- **THEN** aucune participation n'est marquée gagnante

#### Scenario: Résultat manquant
- **WHEN** l'utilisateur valide une partie coopérative sans résultat
- **THEN** le système refuse

#### Scenario: Formulaire adapté au type du jeu
- **WHEN** l'utilisateur sélectionne un jeu coopératif
- **THEN** le formulaire remplace la sélection des gagnants par le choix victoire/défaite
