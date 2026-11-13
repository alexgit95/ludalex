## 1. Modèle de présentation

- [x] 1.1 Définir le mode de présentation (`COOPERATIF`, `SOLO`, `EQUIPES`, `DUEL`, `MULTI`) et sa règle de priorité dans `VuePartie` ou un type dédié
- [x] 1.2 Regrouper les participations par équipe (ordre des équipes, puis groupe sans équipe) pour le mode `EQUIPES`
- [x] 1.3 Exposer le mode et les groupes dans l'état d'affichage (`AffichageService`) sans requête supplémentaire

## 2. Gabarit et CSS

- [x] 2.1 Ajouter la couche de fond `fond-jeu` (image via `--fond-image`, flou, voile, repli sur la couleur) pour `EN_COURS` et `EN_PAUSE`
- [x] 2.2 Refaire la section `partie-en-cours` : grille `minmax(0, 1fr) auto`, titre du jeu sans image au premier plan, avatars en `vh`
- [x] 2.3 Rendre les variantes duel, multi, équipes (nom d'équipe, groupe sans équipe), coopératif (« Ensemble ») et solo
- [x] 2.4 Créer le cartouche VS (SVG d'épées inline, style parchemin) et l'animation `pulsation-vs` avec `prefers-reduced-motion`
- [x] 2.5 Vérifier le rendu sur tablette réelle (rognage, flou, lisibilité) et ajuster

## 3. Tests

- [x] 3.1 Tests unitaires du mode de présentation : solo, duel, multi, équipes, joueur sans équipe, coopératif
- [x] 3.2 Tests unitaires du regroupement par équipe (ordre, groupe sans équipe)
- [x] 3.3 Mettre à jour les tests de `AffichageService` et `AffichageController` (fond, VS, équipes, coopératif, image absente)
- [x] 3.4 Exécuter la suite avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`

## 4. Documentation et version

- [x] 4.1 Mettre à jour `README.md` (écran Affichage : fond, VS, équipes)
- [x] 4.2 Mettre à jour `CHANGELOG.md` (`[Added]`, `[Changed]`, `[Fixed]`) et incrémenter la version mineure dans `pom.xml` et le changelog (identiques)
