## Context

La charte heroic fantasy (0.11.0) fournit variables CSS, parchemin, lierres et cartouches. Le champion est rendu dans `jeux/liste.html` par un `<span class="champion">` en position absolue ; la grille utilise `auto-fill, minmax(9rem, 1fr)` ; `PartieController#resultat` renvoie le fragment de résultat sur changement du `<select>` de jeu ; la navigation est un flex wrap dans `fragments/base.html`.

## Goals / Non-Goals

**Goals:**
- Champion lisible quelle que soit la couleur du jeu, vignettes alignées.
- Grille plus aérée (2 puis 3 colonnes), centrée.
- Confirmation visuelle du jeu choisi à la déclaration.
- Navigation mobile en un panneau, utilisable sans JS.

**Non-Goals:**
- Aucun changement de calcul des champions, de schéma ou d'API ; pas de bibliothèque front.

## Decisions

- **Champion** : `<p class="champions-vignette">` sous le titre, parchemin `--surface` (bord `--or`), coupe SVG en fond ou `<svg>` en ligne, avatars et noms en `inline-flex` avec `flex-wrap` ; `min-height` de deux lignes ; élément vide présent pour réserver la hauteur (`aria-hidden`) quand il n'y a pas de champion. Le contraste encre/parchemin est déjà couvert par `CharteGraphiqueTest`.
- **Grille** : `grid-template-columns: repeat(2, 1fr)`, puis `repeat(3, 1fr)` dès 40 rem, `max-width: 40rem` et `margin-inline: auto`.
- **Aperçu du jeu** : option côté serveur ; `resultat.html` renvoie en plus un `<div id="apercu-jeu" hx-swap-oob="true">` avec le fragment `image` du jeu et son nom ; le formulaire contient un `#apercu-jeu` vide. Alternative écartée : `data-image` sur chaque `<option>` (expose toutes les URL et ajoute du JS).
- **Burger** : `<button class="burger" aria-expanded="false" aria-controls="navigation">` ajouté par le gabarit, masqué au-delà de 40 rem ; `app.js` ajoute la classe `js` à `<html>` puis bascule `aria-expanded` et la classe `ouvert` ; sans classe `js`, la navigation reste visible (amélioration progressive). Panneau parchemin positionné sous l'en-tête, fermeture au clic extérieur et sur Échap, focus visible conservé.
- **Icône** : SVG « trois lianes » en `currentColor`, couleur `--encre`, focus doré ; dans `static/decor/` pour la coupe et le burger, ajoutés au cache du service worker (cache incrémenté).

## Risks / Trade-offs

- Assertions existantes sur le HTML de la liste et de la navigation → les liens restent dans le DOM, le markup du champion est mis à jour dans les tests.
- Hauteur réservée → un peu d'espace vide sur les jeux sans champion, assumé pour l'alignement.
- Panneau qui recouvre le contenu → il se ferme à la navigation et au clic extérieur.
