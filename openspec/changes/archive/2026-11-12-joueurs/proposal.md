## Why

Les parties, statistiques et l'écran Affichage manipulent des joueurs : de simples fiches déclarées dans l'application (nom, avatar, image de victoire), sans compte. Il faut un écran d'administration pour les gérer, y compris leur suppression sans casser l'historique des parties.

## What Changes

- Entité `Joueur` (nom, avatar, image de victoire, indicateur d'anonymisation).
- Écran Admin : liste, création, modification des joueurs ; images par upload ou URL (composant du change `images`).
- Suppression = **anonymisation** : nom remplacé, images retirées (fichiers supprimés), parties conservées.
- Mise à jour de `docs/database.puml`.

## Capabilities

### New Capabilities
- `gestion-joueurs`: création, modification, liste et anonymisation des joueurs, avec avatar et image de victoire.

### Modified Capabilities
<!-- Aucune -->

## Impact

- Code : package `joueurs` (entité, dépôt, service, contrôleur MVC, gabarits).
- Base : nouvelle table `joueur`.
- Dépend de `images`. Version : `0.3.0`.
