## Why

L'utilisateur possède déjà un historique sur MyLudo (collection de jeux, parties jouées avec joueurs, scores, dates et lieux). Le ressaisir à la main est fastidieux : l'import reprend jeux possédés, personnes, parties et statistiques depuis les deux fichiers d'export MyLudo. Il doit pouvoir être rejoué avec un export plus récent sans rien écraser ni dupliquer, et suivre son avancement à l'écran. Ce change est prévu pour une **v2** : il n'est pas nécessaire au fonctionnement de la v1.

## What Changes

- Page Admin « Import MyLudo » : upload de **deux fichiers JSON** (`collection` et `plays`, l'un ou l'autre ou les deux), analyse à blanc, correspondance des joueurs, confirmation, puis import.
- **Barre de progression** par phases (lecture, analyse, import) avec compteur et élément en cours ; suivi par interrogation légère depuis la page.
- **Delta seulement** : les parties déjà présentes en base (même jeu, même jour, au moins un joueur en commun) ne sont jamais écrasées ni dupliquées ; seules les parties manquantes sont ajoutées. Plusieurs parties identiques le même jour (manches successives) restent possibles.
- Jeux : créés depuis la collection (nom, prix, date d'acquisition) ; un jeu déjà présent n'est pas modifié, seuls ses champs vides (prix, date d'achat) sont complétés.
- Joueurs : rapprochés par nom normalisé, créés si absents ; plusieurs « Anonyme » d'une même partie deviennent « Anonyme », « Anonyme 2 »…
- **Rapport** : message de réussite avec compteurs (créés, déjà présents, ignorés avec raison, avertissements) ou message d'erreur diagnostiquable (fichier, numéro d'entrée, jeu et date, champ, cause ; ligne et colonne pour un JSON mal formé).
- Durée, commentaire, notes, images et métadonnées MyLudo ne sont pas importés (aucun champ correspondant, aucun changement de schéma).

## Capabilities

### New Capabilities
- `import-myludo`: import des jeux et des parties depuis les exports MyLudo (collection et parties), avec analyse, correspondance des joueurs, ajout du delta seul, progression et rapport.

### Modified Capabilities
<!-- Aucune -->

## Impact

- Code : package `importmyludo` (lecteurs JSON en flux des deux exports, analyse, rapprochement, tâche d'import avec progression, import transactionnel), contrôleur MVC et pages Admin (analyse, progression par HTMX, rapport), lien depuis l'Admin.
- Données : création de jeux, joueurs, parties, équipes et participations ; aucune suppression ni modification d'enregistrement existant hors complément des champs vides d'un jeu.
- Aucun changement de schéma ni du format de sauvegarde (`docs/database.puml` inchangé).
- Dépend de `parties`, `ludotheque`, `joueurs` et `statistiques` (pour voir l'effet) ; s'appuie sur le suivi par interrogation de l'écran Affichage.
- Exemples réels et anonymisés : `docs/exemple-my-ludo/`. Version : prochaine version mineure disponible à la livraison.
