## 1. Ludothèque

- [x] 1.1 Créer le SVG de coupe (`static/decor/coupe.svg`) et le style du parchemin champion dans `ludalex.css`
- [x] 1.2 Remplacer le badge champion de `jeux/liste.html` par le parchemin sous le titre (noms à la ligne, hauteur réservée)
- [x] 1.3 Reprendre le parchemin champion sur `jeux/detail.html` si un champion y est affiché
- [x] 1.4 Grille 2 colonnes puis 3 dès 40 rem, centrée, largeur limitée

## 2. Déclaration d'une partie

- [x] 2.1 `PartieController#resultat` expose le jeu sélectionné au modèle
- [x] 2.2 `parties/formulaire.html` : zone `#apercu-jeu` ; `parties/resultat.html` : aperçu en `hx-swap-oob`
- [x] 2.3 Style de l'aperçu (taille raisonnable sur mobile)

## 3. Menu burger

- [x] 3.1 SVG burger en trois lianes (`static/decor/burger.svg` ou en ligne, `currentColor`)
- [x] 3.2 `fragments/base.html` : bouton `aria-expanded`/`aria-controls`, navigation avec identifiant
- [x] 3.3 `app.js` : classe `js`, bascule, fermeture au clic extérieur et sur Échap
- [x] 3.4 CSS : panneau parchemin sous l'en-tête sur petit écran, navigation en ligne dès 40 rem, repli sans JS

## 4. Tests

- [x] 4.1 Tests MVC de la liste (parchemin champion, égalité, sans champion, grille) et du formulaire/fragment (aperçu du jeu)
- [x] 4.2 Tests des gabarits (bouton `aria-expanded`, liens de navigation toujours présents)
- [x] 4.3 `CharteGraphiqueTest` : nouveaux décors présents et légers, aucune couleur en dur, aucune ressource externe

## 5. Documentation et livraison

- [x] 5.1 Cache du service worker incrémenté et nouveaux décors dans `/decor/`
- [x] 5.2 README, CHANGELOG `[0.12.0]` et pom en `0.12.0` identiques
- [x] 5.3 `mvn test` complet vert, `openspec validate --all`
