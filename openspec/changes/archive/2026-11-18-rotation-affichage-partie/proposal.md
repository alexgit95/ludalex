## Why

Pendant une partie, l'écran d'affichage reste aujourd'hui limité à la confrontation en cours. Une rotation enrichira cet écran avec les souvenirs des parties et des images adaptées à son orientation.

## What Changes

- Sélectionner les images du diaporama selon l'orientation de la fenêtre : noms commençant par `portrait_` en portrait, autres noms en paysage ; utiliser le fond LUDALEX en l'absence d'image adaptée.
- **BREAKING** : pendant une partie en cours, remplacer l'affichage permanent de la confrontation par une rotation de vues de 10 secondes, avec une image du diaporama entre chaque vue : confrontation, dernières parties du jeu et champion, dernières parties tous jeux confondus.
- Ajouter à la vue du jeu un podium des trois premiers rangs, révélés de la troisième à la première place en cinq secondes maximum, avec places partagées en cas d'égalité ; afficher au plus trois joueurs par rang et compter les autres ex æquo.
- Conserver un QR de fin de partie valide et visible sur toutes les vues de la rotation ; ignorer les vues d'historique sans partie terminée.
- Conserver les comportements prioritaires de pause et de victoire ainsi que la veille hors partie.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `diaporama-veille` : sélectionner les images selon l'orientation et réutiliser le diaporama pendant la partie.
- `ecran-affichage` : alterner confrontation, images et historiques avec champion et QR permanent pendant une partie.

## Impact

- Service, contrôleur, vues et scripts de l'écran d'affichage ; requêtes d'historique et statistiques du jeu.
- Documentation utilisateur, README du diaporama et tests serveur et navigateur.
- Changement fonctionnel incompatible avec l'affichage VS permanent : documenter la migration et incrémenter la version majeure selon les règles du projet.