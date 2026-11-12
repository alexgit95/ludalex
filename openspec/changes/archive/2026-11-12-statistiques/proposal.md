## Why

La ludothèque doit valoriser les parties : qui est le champion d'un jeu, quel est le taux de victoire de chaque joueur, quelles sont les dernières parties. La liste des jeux affiche un symbole de champion, et la fiche détail une synthèse.

## What Changes

- Statistiques par jeu : classement des joueurs par victoires, champion(s), taux de victoire par joueur, dix dernières parties.
- Symbole de champion sur chaque vignette de la liste : en cas d'égalité, **tous les avatars** des premiers.
- Fiche détail : remplissage de la zone « statistiques » prévue par `ludotheque`.
- Calculs par **requêtes agrégées** (aucune collection de parties chargée en mémoire).

## Capabilities

### New Capabilities
- `statistiques-jeux`: champion(s), taux de victoire, meilleurs joueurs et dernières parties par jeu.

### Modified Capabilities
<!-- Aucune : la zone statistiques et l'emplacement du champion sont de simples emplacements dans `ludotheque`, non encore archivé -->

## Impact

- Code : package `statistiques` (requêtes agrégées, service, fragments Thymeleaf).
- Base : aucune nouvelle table ; éventuels index sur `participation`.
- Dépend de `parties`. Version : `0.6.0`.
