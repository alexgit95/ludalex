## Why

La vue du jeu doit mieux utiliser les écrans portrait, et le récapitulatif des parties doit mettre les vainqueurs davantage en valeur. En veille, les résultats récents et un accès rapide au lancement d'une partie enrichissent le diaporama sans interrompre son usage principal.

## What Changes

- **BREAKING** : en portrait, la vue du jeu en cours empile le podium, les trois dernières parties du jeu et le QR de fin en bas ; en paysage, elle conserve ses cinq dernières parties et sa disposition actuelle.
- **BREAKING** : remplacer la liste globale par une composition de cinq médaillons représentant les cinq dernières parties terminées, y compris plusieurs parties d'un même jeu, avec les vainqueurs plutôt que l'ensemble des participants ; utiliser cette même vue en portrait et en paysage.
- En veille, insérer ce récapitulatif après un nombre aléatoire de deux à cinq images, ou laisser le diaporama seul si aucun historique n'existe.
- En veille, afficher sur une image sur deux un QR ouvrant la page de lancement de partie, sans déclencher le lancement ni contourner l'authentification ; l'alterner aussi sur le fond LUDALEX en l'absence d'image adaptée.
- Afficher les victoires du podium sous la forme `4V` et entourer le premier rang de lauriers, y compris en cas d'ex æquo.
- Préserver les vues de dix secondes, la priorité de la pause et de la victoire, le QR de fin de partie et le respect du mouvement réduit.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `ecran-affichage` : vue portrait, galerie des résultats, podium et QR de lancement en veille.
- `diaporama-veille` : insertion aléatoire du récapitulatif et alternance du QR pendant la veille.

## Impact

- Contrôleur, services, génération des QR, gabarits et ressources CSS/JavaScript de l'écran d'affichage ; réutilisation des requêtes d'historique bornées.
- Tests Java, sécurité Cloudflare/CSRF, vérifications navigateur portrait et paysage, documentation et cache PWA.
- Changements fonctionnels incompatibles avec l'affichage précédent : documenter la migration et incrémenter la version majeure selon les règles du projet. Aucun changement de schéma ni de mécanisme d'authentification n'est prévu.