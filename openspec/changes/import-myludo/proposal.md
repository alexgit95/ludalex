## Why

L'utilisateur possède déjà un historique sur l'application MyLudo (jeux, parties). Le ressaisir à la main serait fastidieux ; une importation reprend son historique dans LUDALEX. Ce change est prévu pour une **v2** : il n'est pas nécessaire au fonctionnement de la v1.

## What Changes

- Page Admin « Import MyLudo » : upload d'un export MyLudo, aperçu de ce qui sera importé, correspondance des joueurs, import des jeux et des parties.
- Rapprochement avec l'existant (jeux et joueurs déjà présents) pour éviter les doublons ; import rejouable sans doublon.
- Rapport d'import (créés, ignorés, erreurs).

## Capabilities

### New Capabilities
- `import-myludo`: import des jeux et des parties depuis un export MyLudo, avec aperçu, rapprochement, idempotence et rapport.

### Modified Capabilities
<!-- Aucune -->

## Impact

- Code : package `importmyludo` (lecteur de l'export, rapprochement, import transactionnel), page Admin.
- Données : création de jeux, joueurs, parties et participations.
- Dépend de `parties` (et de `statistiques` pour en voir l'effet). Version : `0.10.0`.
- **Prérequis** : le format exact de l'export MyLudo doit être confirmé sur un export réel avant la conception détaillée.
