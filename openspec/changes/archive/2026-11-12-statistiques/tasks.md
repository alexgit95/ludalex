## 1. Requêtes agrégées

- [x] 1.1 Créer les projections (records) : ligne de classement, champion par jeu
- [x] 1.2 Écrire la requête de classement par jeu (parties, victoires, parties terminées uniquement)
- [x] 1.3 Écrire la requête des champions pour une liste d'identifiants de jeux
- [x] 1.4 Écrire la requête des dix dernières parties et le chargement groupé de leurs participations
- [x] 1.5 Déclarer les index (`participation`, `partie`) et mettre à jour `docs/database.puml`

## 2. Service

- [x] 2.1 Implémenter taux de victoire (non défini sans partie) et classement trié
- [x] 2.2 Implémenter la désignation des champions (égalité, maximum > 0, anonymisés exclus)
- [x] 2.3 Implémenter les champions d'une page de jeux sans N+1
- [x] 2.4 Tests : classement, parties en cours ignorées, coopératif, taux, champion unique, égalité, aucune victoire, anonymisé en tête, dix dernières, nombre constant de requêtes

## 3. Interface

- [x] 3.1 Créer le fragment statistiques de la fiche (classement, taux, dernières parties)
- [x] 3.2 Créer le fragment champion (un ou plusieurs avatars) et l'inclure dans la vignette de la liste
- [x] 3.3 Charger la zone statistiques de la fiche par HTMX

## 4. Documentation et livraison

- [x] 4.1 Mettre à jour `README.md` (statistiques, règle d'égalité, taux de victoire)
- [x] 4.2 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à `0.6.0`, identique dans les deux fichiers
- [x] 4.3 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
