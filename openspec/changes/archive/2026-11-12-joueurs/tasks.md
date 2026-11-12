## 1. Modèle

- [x] 1.1 Créer l'entité `Joueur` (nom, avatar, image de victoire, anonymisé) et son dépôt
- [x] 1.2 Définir l'interface `ParticipationsJoueur` (existence de participations) avec une implémentation provisoire
- [x] 1.3 Mettre à jour `docs/database.puml`

## 2. Service

- [x] 2.1 Implémenter création et modification avec validation du nom et unicité insensible à la casse
- [x] 2.2 Implémenter la gestion des images (remplacement, suppression des fichiers après commit)
- [x] 2.3 Implémenter l'anonymisation et la suppression physique sans participation
- [x] 2.4 Implémenter la liste paginée et la requête des joueurs sélectionnables (non anonymisés)
- [x] 2.5 Tests : création, nom manquant, doublon, remplacement d'image, anonymisation avec parties, suppression sans partie, exclusion des anonymisés

## 3. Interface Admin

- [x] 3.1 Créer l'écran Admin des joueurs (liste paginée, avatar, nom)
- [x] 3.2 Créer le formulaire de création/modification avec le fragment de saisie d'image
- [x] 3.3 Créer la confirmation de suppression (HTMX)
- [x] 3.4 Ajouter l'image par défaut pour avatar et image de victoire absents

## 4. Documentation et livraison

- [x] 4.1 Mettre à jour `README.md` (gestion des joueurs, anonymisation)
- [x] 4.2 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à `0.3.0`, identique dans les deux fichiers
- [x] 4.3 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
