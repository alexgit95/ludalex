## Why

Toutes les données vivent sur un Raspberry Pi : une carte SD qui meurt ou un changement de moteur (SQLite en dev, PostgreSQL en prod) ne doit pas faire perdre la ludothèque. L'utilisateur veut un seul fichier, téléchargeable puis ré-uploadable tel quel, qui contienne les données et toutes les images uploadées.

## What Changes

- Page Admin « Sauvegarde / Restauration ».
- **Export** : un seul ZIP contenant un manifeste (version du format), les données en JSON indépendantes du moteur (joueurs, jeux, prêts, parties, participations, équipes) et les images uploadées (jeux, joueurs). Les images par URL ne sont que des URL dans les données.
- **Restauration** : upload du ZIP **sans modification** ; remplacement complet (wipe) des données et du volume d'images, après confirmation explicite.
- Traitement **en flux** (écriture et lecture), validation complète avant toute suppression, import atomique.
- Contrôles du ZIP : zip slip, taille, nombre d'entrées, types d'images, version du format.

## Capabilities

### New Capabilities
- `sauvegarde-restauration`: export ZIP, restauration complète par réupload, contrôles de sécurité et d'intégrité.

### Modified Capabilities
<!-- Aucune -->

## Impact

- Code : package `sauvegarde` (export en flux, validateur de ZIP, importateur transactionnel), page Admin.
- Données : lecture de toutes les entités ; écriture après suppression complète en restauration.
- Configuration : limite d'upload spécifique à la restauration (taille du ZIP).
- Dépend de tous les changes de données (`images` à `affichage`). Version : `0.9.0`.
