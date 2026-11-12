## 1. Modèle

- [x] 1.1 Créer l'entité `Jeu` et son dépôt
- [x] 1.2 Créer l'entité `Pret` et son dépôt (prêt en cours, historique, emprunteurs courants groupés)
- [x] 1.3 Définir l'interface `PartiesJeu` (existence de parties) avec une implémentation provisoire
- [x] 1.4 Mettre à jour `docs/database.puml`

## 2. Services

- [x] 2.1 Implémenter création et modification d'un jeu (validation nom, prix, date) avec calcul de la couleur de fond
- [x] 2.2 Implémenter la suppression (refus s'il a des parties, suppression de l'image et des prêts sinon)
- [x] 2.3 Implémenter le prêt (un seul en cours, verrou pessimiste) et le retour (date cohérente)
- [x] 2.4 Implémenter la liste paginée avec emprunteur courant sans N+1
- [x] 2.5 Tests : création valide/invalide, couleur de fond, suppression avec/sans parties, prêt, prêt déjà en cours, retour antérieur, historique, concurrence

## 3. Interface

- [x] 3.1 Créer l'écran liste en vignettes (image, titre, emplacement champion, emprunteur)
- [x] 3.2 Créer le formulaire de création/modification avec fragment d'image et sélecteur de couleur
- [x] 3.3 Créer la fiche détail (achat, type, historique des prêts, zone statistiques)
- [x] 3.4 Créer les actions HTMX de prêt, de retour et de suppression avec confirmation
- [x] 3.5 Ajouter l'image par défaut des jeux

## 4. Documentation et livraison

- [x] 4.1 Mettre à jour `README.md` (ludothèque, prêts)
- [x] 4.2 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à `0.4.0`, identique dans les deux fichiers
- [x] 4.3 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
