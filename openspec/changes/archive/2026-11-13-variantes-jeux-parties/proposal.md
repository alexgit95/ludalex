## Why

Pour les jeux qui proposent plusieurs campagnes, enquêtes ou éditions, l'historique actuel ne permet pas de distinguer les parties jouées. L'utilisateur veut pouvoir signaler cette particularité sur la fiche du jeu puis noter librement la variante choisie pour chaque partie.

## What Changes

- Ajouter au jeu un indicateur précisant qu'il propose plusieurs campagnes, enquêtes ou versions.
- Pour un jeu ainsi marqué, permettre de saisir un libellé libre facultatif sur chaque partie, à la déclaration ou au lancement d'une partie en direct.
- Afficher le libellé dans l'historique des parties et le conserver lors de la modification.
- Inclure l'indicateur et le libellé dans la sauvegarde/restauration; la nouvelle version restaurera les ZIP v1 en attribuant les valeurs par défaut.
- **BREAKING** : versionner les nouveaux ZIP en v2. Les ZIP v2 ne seront pas restaurables par les versions antérieures; il faudra mettre l'application à jour avant restauration. Les ZIP v1 resteront acceptés.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `catalogue-jeux`: déclarer si un jeu propose des campagnes, enquêtes ou versions.
- `declaration-parties`: saisir, modifier et consulter le libellé libre d'une partie.
- `partie-en-cours`: saisir et conserver le libellé choisi au lancement d'une partie en direct.
- `sauvegarde-restauration`: exporter et restaurer l'indicateur du jeu et le libellé de la partie.

## Impact

- Entités `Jeu` et `Partie`, formulaires et vues de jeu, déclaration de partie, lancement direct et historique.
- Export/import JSON du ZIP de sauvegarde et tests d'aller-retour, en préservant la compatibilité des archives sans ces valeurs.
- `docs/database.puml`, tests Java, README, changelog, version majeure du `pom.xml` et cache du service worker.
- Aucun endpoint REST, aucune nouvelle dépendance et aucun catalogue séparé de campagnes.
