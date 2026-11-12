## 1. Modèle

- [ ] 1.1 Créer les entités `Partie`, `Participation` et `Equipe` avec statut, résultat coopératif et champs de pause (nullables)
- [ ] 1.2 Créer les dépôts (liste paginée filtrée par jeu, lieux distincts, existence de participations et de parties)
- [ ] 1.3 Implémenter `ParticipationsJoueur` et `PartiesJeu`
- [ ] 1.4 Mettre à jour `docs/database.puml`

## 2. Règles métier

- [ ] 2.1 Valider jeu, joueurs actifs sans doublon, date non future, lieu ≤ 100 caractères
- [ ] 2.2 Implémenter la détermination des gagnants (désignés, équipe gagnante, coopératif victoire/défaite)
- [ ] 2.3 Valider les équipes (membres, un joueur par équipe, joueurs participants)
- [ ] 2.4 Valider les scores (entiers, facultatifs)
- [ ] 2.5 Implémenter création, modification et suppression avec confirmation
- [ ] 2.6 Implémenter la requête d'autocomplétion (10 résultats, sans doublon, insensible à la casse)
- [ ] 2.7 Tests : partie valide, sans gagnant, plusieurs gagnants, gagnant hors participants, joueur en double, anonymisé, date future, score invalide, équipes, coopératif gagné/perdu, résultat manquant, autocomplétion

## 3. Interface

- [ ] 3.1 Créer le formulaire de déclaration (jeu, joueurs, date, lieu, résultat, scores, équipes)
- [ ] 3.2 Recharger par HTMX la zone de résultat selon le type du jeu
- [ ] 3.3 Ajouter l'autocomplétion du lieu (fragment `datalist`, délai 300 ms)
- [ ] 3.4 Créer la liste paginée des parties avec filtre par jeu
- [ ] 3.5 Créer les actions de modification et de suppression avec confirmation

## 4. Documentation et livraison

- [ ] 4.1 Mettre à jour `README.md` (déclaration de parties, équipes, coopératif, lieux)
- [ ] 4.2 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à `0.5.0`, identique dans les deux fichiers
- [ ] 4.3 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
