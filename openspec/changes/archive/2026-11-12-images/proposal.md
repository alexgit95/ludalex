## Why

Jeux, joueurs et victoires s'illustrent tous par une image, fournie soit par upload soit par URL. L'écran Affichage a aussi besoin d'une couleur de fond tirée de l'image du jeu. Ce besoin transverse doit être résolu une fois, sobrement pour un Raspberry Pi, avant les entités qui l'utilisent.

## What Changes

- Composant d'image réutilisable : une image est soit un **fichier uploadé** (stocké sur un volume, 2 Mo maximum, formats JPEG/PNG/GIF/WebP), soit une **URL** (seule l'URL est conservée, l'image n'est jamais copiée).
- Service de restitution des fichiers uploadés (lecture en flux, `Cache-Control`).
- Protection SSRF pour toute URL saisie et pour tout téléchargement ponctuel.
- Bibliothèque tierce pour lire le **WebP** (ImageIO ne le gère pas).
- Calcul de la **couleur de fond** à l'enregistrement : décodage avec sous-échantillonnage, réduction à environ 32×32 pixels, classe de couleur la plus fréquente ; stockage ; modification manuelle ; couleur neutre en cas d'échec.

## Capabilities

### New Capabilities
- `stockage-images`: upload sur volume, images par URL, restitution, limites de taille et de format, protection SSRF.
- `couleur-fond`: calcul unique de la couleur dominante, stockage, modification manuelle, repli neutre.

### Modified Capabilities
<!-- Aucune -->

## Impact

- Code : package `images` (embeddable `ImageSource`, services de stockage, de validation d'URL et de calcul de couleur, contrôleur de restitution).
- Dépendances : bibliothèque WebP (par exemple `webp-imageio`/décodeur Java pur compatible arm64 ; choix final à valider sur Raspberry Pi).
- Systèmes : volume Docker `/data/images` déjà prévu par `socle`.
- Version : `0.2.0`.
