## Why

Le cœur de l'application est le catalogue des jeux possédés : nom, image, prix, date d'achat, caractère coopératif, et suivi des prêts. L'écran de liste doit afficher les jeux sous forme de vignettes, et la fiche détaillée accueillera ensuite les statistiques.

## What Changes

- Entité `Jeu` (nom, image, prix, date d'achat, coopératif, couleur de fond).
- Écran liste : vignettes (image, titre dessous) ; l'emplacement du symbole de champion est prévu mais alimenté par le change `statistiques`.
- Écran détail : informations d'achat ; zone réservée aux statistiques.
- Création, modification et suppression d'un jeu ; couleur de fond calculée à l'enregistrement (composant du change `images`).
- Prêts : prêter un jeu à une personne (texte libre), enregistrer le retour, conserver l'**historique** des prêts, afficher l'emprunteur courant sur la liste.
- Mise à jour de `docs/database.puml`.

## Capabilities

### New Capabilities
- `catalogue-jeux`: gestion des jeux, liste en vignettes, fiche détail.
- `prets-jeux`: prêt, retour, historique, emprunteur courant.

### Modified Capabilities
<!-- Aucune -->

## Impact

- Code : packages `jeux` et `prets`, gabarits liste et détail.
- Base : tables `jeu` et `pret`.
- Dépend de `images`. Version : `0.4.0`.
