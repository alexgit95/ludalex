## Why

La route `/` affiche actuellement une page d'accueil intermédiaire alors que l'usage principal est la consultation de la ludothèque. Sur cette page, le titre et le bouton d'ajout sont alignés à gauche du contenu tandis que la grille est centrée, ce qui crée le décalage visible dans la capture.

## What Changes

- Faire de la ludothèque la destination de la route `/`, en réutilisant la liste paginée existante.
- Aligner le titre et l'action « Ajouter un jeu » sur le bord gauche de la grille, tout en conservant son centrage, sa largeur limitée et son comportement responsive.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `interface-pwa`: la route d'accueil `/` ouvre directement la ludothèque.
- `catalogue-jeux`: le titre et l'action principale de la liste s'alignent sur la grille centrée.

## Impact

- Route MVC `/` dans `AccueilController` et éventuel template d'accueil devenu inutile.
- Gabarit de liste des jeux et styles de la ludothèque.
- Tests de contrôleur et tests de rendu/CSS associés.
- Specs OpenSpec `interface-pwa` et `catalogue-jeux`.
- Aucun changement de base de données, API, dépendance ou configuration PWA requis; le `start_url` reste `/`.
