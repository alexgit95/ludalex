## Why

L'écran Affichage est vu par toute la table : il montre le jeu en cours en grand, les joueurs, un QR code pour clore la partie, et célèbre le vainqueur. Quand rien ne se joue, il doit rester agréable à regarder. Il tourne sur un navigateur ouvert en permanence, derrière Cloudflare Access.

## What Changes

- Page `/affichage` : image du jeu en grand, fond de la couleur de fond stockée, avatars de chaque joueur, QR code vers `/fin/{token}`.
- Partie en pause : affichage en grand du joueur à qui c'est de jouer.
- Partie terminée : animation sur l'avatar (et l'image de victoire) des vainqueurs, puis retour à la veille.
- Aucune partie : **diaporama** d'images celtiques / village médiéval embarquées dans l'application.
- Mise à jour par **polling HTMX léger** (3 s), génération et renouvellement du token côté serveur, QR code généré côté serveur (bibliothèque ZXing core).
- Résilience : l'écran se recharge si la session Cloudflare (1 mois) expire.

## Capabilities

### New Capabilities
- `ecran-affichage`: partie en cours, pause, victoire animée, QR code, mise à jour automatique, reprise après expiration de session.
- `diaporama-veille`: diaporama d'images embarquées lorsqu'aucune partie n'est active.

### Modified Capabilities
<!-- Aucune -->

## Impact

- Code : package `affichage` (contrôleur, service d'état, générateur de QR), gabarits, CSS d'animation, images du diaporama dans `src/main/resources/static`.
- Dépendances : ZXing `core` (Java pur, compatible arm64).
- Dépend de `images` et `partie-en-cours`. Version : `0.8.0`.
