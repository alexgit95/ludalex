## Why

Sur la ludothèque, le champion d'un jeu est un petit badge peu lisible posé sur la couleur du jeu, la grille remplit tout l'écran avec des vignettes trop petites, la déclaration d'une partie ne permet pas de vérifier visuellement le jeu choisi, et la navigation occupe deux lignes sur mobile.

## What Changes

- Ludothèque : le champion actuel passe sous le titre du jeu, sur un petit parchemin, avec une coupe SVG dans le thème, son avatar et son nom ; en cas d'égalité tous les noms s'affichent avec retour à la ligne ; la hauteur de la ligne est réservée pour garder les vignettes alignées.
- Ludothèque : grille limitée à 2 colonnes (petit écran) puis 3 colonnes (grand écran), centrée et étroite.
- Déclaration d'une partie : l'image du jeu choisi s'affiche dès la sélection (fragment HTMX déjà déclenché au changement de jeu).
- Navigation : sur petit écran, un bouton burger (icône en trois lianes) ouvre un panneau parchemin sous l'en-tête ; bouton avec `aria-expanded`, petit JS dans `app.js`, fermeture au clic extérieur et sur Échap ; sans JavaScript la navigation reste affichée.
- Version `0.12.0`, cache du service worker incrémenté ; aucun changement de schéma, d'API ni de configuration.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `catalogue-jeux`: affichage du champion sous le titre sur un parchemin, grille 2/3 colonnes centrée.
- `declaration-parties`: aperçu de l'image du jeu sélectionné dans le formulaire.
- `charte-graphique`: menu burger sur petit écran, parchemin et coupe/lianes en SVG du thème.

## Impact

- Gabarits : `fragments/base.html`, `jeux/liste.html`, `jeux/detail.html` (parchemin champion), `parties/formulaire.html`, `parties/resultat.html`.
- Statiques : `ludalex.css`, `app.js`, nouveaux SVG dans `static/decor/` (coupe, burger), `sw.js`.
- Java : `PartieController#resultat` expose le jeu sélectionné ; tests associés (MVC, JS/markup, charte).
- Documentation : README, CHANGELOG, pom en `0.12.0`.
