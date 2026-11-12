## 1. Fondations

- [ ] 1.1 Définir les variables CSS (palette, rayons, ombres, espacements, tailles) dans `ludalex.css` et vérifier les contrastes (test automatisé sur les couples texte/fond)
- [ ] 1.2 Choisir la typographie après essais (pile système, ou police libre `woff2` ≤ 60 Ko auto-hébergée) et la déclarer
- [ ] 1.3 Créer la texture de parchemin (dégradés + grain SVG en data-URI) et les 2 à 3 SVG de lierre sous `static/decor/`

## 2. Composants et pages courantes

- [ ] 2.1 Restyler base, en-tête, navigation, pied de page, titres, liens et boutons avec focus visible
- [ ] 2.2 Restyler formulaires, saisie d'image, messages et erreurs, tableaux, pagination et confirmations
- [ ] 2.3 Restyler vignettes de jeux, fiche de jeu, parties, partie en direct, statistiques et classement
- [ ] 2.4 Restyler l'administration (joueurs, sauvegarde) et les pages d'erreur 403/404/500
- [ ] 2.5 Retirer les couleurs codées en dur hors variables et les styles en ligne inutiles (garder la couleur de fond dynamique des jeux)

## 3. Écran Affichage

- [ ] 3.1 Habiller l'Affichage (cartouches parchemin, lierres de bordure, tailles `clamp`) dans `affichage.css`
- [ ] 3.2 Vérifier les quatre états (veille, partie, pause, victoire) sur fonds clairs, foncés et saturés

## 4. PWA

- [ ] 4.1 Aligner `theme-color` et le manifeste sur la palette et incrémenter la version du cache du service worker
- [ ] 4.2 Vérifier que le service worker met bien en cache les nouveaux SVG et la police éventuelle

## 5. Vérification

- [ ] 5.1 Contrôle visuel dans le navigateur de chaque famille de pages et de l'Affichage (poste et grand écran), lisibilité de loin
- [ ] 5.2 Tests : contraste de la palette, absence de ressource externe dans les CSS et gabarits, rendu des pages existantes inchangé fonctionnellement

## 6. Documentation et livraison

- [ ] 6.1 Mettre à jour `README.md` (charte graphique, ajout d'un décor ou d'une police)
- [ ] 6.2 Mettre à jour `CHANGELOG.md` et incrémenter la version du `pom.xml` à la même valeur dans les deux fichiers
- [ ] 6.3 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
