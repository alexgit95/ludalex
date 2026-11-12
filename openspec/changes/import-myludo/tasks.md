## 1. Préparation

- [ ] 1.1 Obtenir un export MyLudo réel et documenter son format (champs, encodage, types)
- [ ] 1.2 Confirmer le périmètre importable (jeux, parties, scores, lieux, joueurs) et ajuster `design.md` et les specs si besoin

## 2. Lecture et aperçu

- [ ] 2.1 Implémenter le lecteur d'export en flux vers un modèle intermédiaire (`ImportPlan`)
- [ ] 2.2 Sécuriser la lecture (taille maximale, XML sans entités externes ou protection d'archive selon le format)
- [ ] 2.3 Implémenter l'aperçu (compteurs de jeux, parties et joueurs)
- [ ] 2.4 Tests : fichier valide, fichier invalide, fichier trop volumineux, entité externe XML

## 3. Rapprochement et import

- [ ] 3.1 Implémenter le rapprochement des joueurs (existant par nom, ou création) avec choix de l'utilisateur
- [ ] 3.2 Implémenter le rapprochement des jeux par nom normalisé
- [ ] 3.3 Implémenter l'import atomique des jeux, parties et participations avec idempotence
- [ ] 3.4 Reprendre l'URL d'image éventuelle selon les règles de `images` (SSRF, couleur de fond, repli neutre)
- [ ] 3.5 Produire le rapport (créés, ignorés, erreurs)
- [ ] 3.6 Tests : joueur existant, nouveau joueur, jeu déjà présent, double import, erreur au milieu, rapport

## 4. Interface Admin

- [ ] 4.1 Créer la page Import MyLudo (upload, aperçu, correspondance des joueurs, confirmation, rapport)

## 5. Documentation et livraison

- [ ] 5.1 Mettre à jour `docs/database.puml` si le modèle évolue
- [ ] 5.2 Mettre à jour `README.md` (import MyLudo)
- [ ] 5.3 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à `0.10.0`, identique dans les deux fichiers
- [ ] 5.4 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
