## Why

Une ludothèque se vit par les parties jouées. L'utilisateur doit pouvoir déclarer, après coup, une partie terminée : jeu, joueurs, date, lieu, gagnants, scores, équipes, et gérer les jeux coopératifs. Ces données alimentent ensuite les statistiques.

## What Changes

- Entités `Partie`, `Participation` (joueur, score facultatif, gagnant) et `Equipe` (propre à une partie).
- Formulaire de déclaration : jeu, joueurs, date (par défaut aujourd'hui), lieu en texte libre avec **autocomplétion des lieux déjà saisis**, gagnants (au moins un, plusieurs possibles), scores facultatifs, équipes facultatives.
- Jeux coopératifs : seul résultat « victoire / défaite » ; une victoire compte pour chaque participant.
- Liste paginée des parties, modification et suppression d'une partie.
- Branchement des interfaces `ParticipationsJoueur` et `PartiesJeu` prévues par `joueurs` et `ludotheque`.
- Mise à jour de `docs/database.puml`.

## Capabilities

### New Capabilities
- `declaration-parties`: déclaration, modification, suppression et liste des parties terminées, scores, gagnants, lieux avec autocomplétion.
- `equipes-parties`: équipes propres à une partie et résultat coopératif.

### Modified Capabilities
<!-- Aucune -->

## Impact

- Code : package `parties`, gabarits de saisie et de liste.
- Base : tables `partie`, `participation`, `equipe`.
- Dépend de `joueurs` et `ludotheque`. Version : `0.5.0`.
