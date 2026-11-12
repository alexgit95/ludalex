# statistiques-jeux Specification

## Purpose
Classement, champion(s), taux de victoire et dernières parties par jeu.

## Requirements

### Requirement: Classement des joueurs par jeu
Le système SHALL calculer, pour un jeu, le nombre de parties jouées et de victoires de chaque joueur ayant joué à ce jeu, en ne comptant que les parties terminées, sans nombre minimum de parties, et en regroupant jeux compétitifs et coopératifs.

#### Scenario: Classement
- **WHEN** Alex a gagné 5 parties sur 8 et Marie 3 sur 8 à « Catan »
- **THEN** le classement place Alex avant Marie

#### Scenario: Parties en cours ignorées
- **WHEN** une partie du jeu est en cours ou en pause
- **THEN** elle n'est comptée ni dans les parties jouées ni dans les victoires

#### Scenario: Victoire coopérative
- **WHEN** une partie coopérative est gagnée par trois joueurs
- **THEN** une victoire est comptée pour chacun d'eux

### Requirement: Taux de victoire
Le système SHALL calculer le taux de victoire d'un joueur pour un jeu comme le nombre de victoires divisé par le nombre de parties jouées.

#### Scenario: Calcul
- **WHEN** un joueur a gagné 3 parties sur 4
- **THEN** son taux de victoire est de 75 %

#### Scenario: Aucune partie
- **WHEN** un jeu n'a aucune partie
- **THEN** aucun taux n'est affiché

### Requirement: Champion(s) d'un jeu
Le système SHALL désigner comme champion le ou les joueurs ayant le plus de victoires pour un jeu ; en cas d'égalité, tous les joueurs à égalité sont désignés. Les joueurs anonymisés ne sont pas éligibles.

#### Scenario: Champion unique
- **WHEN** Alex a 5 victoires et Marie 3
- **THEN** Alex est le champion

#### Scenario: Égalité
- **WHEN** Alex et Marie ont chacun 4 victoires, plus que les autres
- **THEN** les deux sont champions

#### Scenario: Aucune victoire
- **WHEN** aucun joueur n'a de victoire pour ce jeu
- **THEN** aucun champion n'est désigné

#### Scenario: Joueur anonymisé en tête
- **WHEN** le joueur ayant le plus de victoires est anonymisé
- **THEN** le champion est le meilleur des joueurs non anonymisés

### Requirement: Symbole de champion sur la liste
Le système SHALL afficher sur la vignette de chaque jeu un symbole de champion avec l'avatar du champion, ou les avatars de tous les champions en cas d'égalité.

#### Scenario: Égalité sur la vignette
- **WHEN** deux joueurs sont champions d'un jeu
- **THEN** la vignette affiche leurs deux avatars

#### Scenario: Jeu sans champion
- **WHEN** un jeu n'a pas de champion
- **THEN** aucun symbole n'est affiché

#### Scenario: Requête unique pour la page
- **WHEN** la liste affiche une page de 24 jeux
- **THEN** les champions de la page sont obtenus par un nombre constant de requêtes

### Requirement: Dernières parties
Le système SHALL afficher sur la fiche d'un jeu les dix dernières parties terminées : date, lieu, joueurs, gagnants.

#### Scenario: Dix dernières
- **WHEN** un jeu compte 25 parties
- **THEN** les 10 plus récentes sont affichées

### Requirement: Statistiques sans chargement massif
Les statistiques SHALL être calculées par des requêtes agrégées et ne jamais charger en mémoire l'ensemble des parties.

#### Scenario: Jeu avec de nombreuses parties
- **WHEN** un jeu compte 5 000 parties
- **THEN** le classement est obtenu par agrégation côté base, sans instancier les 5 000 parties
