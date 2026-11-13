## 1. Page d'accueil

- [x] 1.1 Faire rediriger `AccueilController` de `/` vers `/jeux` et supprimer le template d'accueil devenu inutilisé, s'il n'a plus de référence.
- [x] 1.2 Ajouter ou adapter les tests pour vérifier la redirection vers la liste paginée et les réponses attendues en `dev` et en `prod` avec ou sans JWT valide.

## 2. Alignement de la ludothèque

- [x] 2.1 Regrouper le titre et l'action « Ajouter un jeu » dans un conteneur centré de largeur maximale identique à la grille, sans modifier le nombre de colonnes ni le centrage des vignettes.
- [x] 2.2 Vérifier l'alignement et la grille sur petits et grands écrans au moyen des tests de rendu/CSS existants ou de tests adaptés.

## 3. Documentation et version

- [x] 3.1 Mettre à jour le README, ajouter l'entrée `[Fixed]` au changelog, incrémenter à l'identique la version du `pom.xml` et du changelog, puis aligner la version du cache dans `sw.js`.
- [x] 3.2 Exécuter les tests ciblés puis la suite Maven avec le binaire `C:\USINE_LOGICIELLE\apache-maven\bin`.
