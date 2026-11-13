## 1. Modèle et saisie

- [x] 1.1 Ajouter l'indicateur de variantes à `Jeu` (défaut faux pour les jeux existants) et le libellé nullable de 200 caractères à `Partie`; mettre à jour `docs/database.puml` et vérifier la compatibilité SQLite/PostgreSQL.
- [x] 1.2 Ajouter l'indicateur aux formulaires de création/modification des jeux et le champ facultatif aux déclarations/modifications de parties; n'effacer aucune variante historique si l'indicateur est désactivé.
- [x] 1.3 Ajouter la saisie conditionnelle au lancement direct et conserver la variante sur la partie active, après pause/reprise et jusqu'à la fin.
- [x] 1.4 Afficher la variante dans l'historique des parties et couvrir les parcours de saisie, modification, jeu sans variantes et libellé trop long par des tests.

## 2. Sauvegarde et restauration

- [x] 2.1 Étendre les DTO JSON et exporter les variantes au format ZIP v2; accepter les ZIP v1 et restaurer leurs champs absents avec les valeurs par défaut.
- [x] 2.2 Tester les aller-retours v2 avec variantes, la restauration d'archives v1 sans variantes, le changement de moteur et l'import MyLudo sans variante.

## 3. Documentation et version

- [x] 3.1 Mettre à jour le README, incrémenter à `1.0.0` le `pom.xml` et le changelog, documenter explicitement la rupture de compatibilité des ZIP v2 dans `[BREAKING CHANGE]` avec la consigne de mise à jour, et aligner le cache du service worker.
- [x] 3.2 Exécuter les tests ciblés puis toute la suite Maven avec le binaire `C:\USINE_LOGICIELLE\apache-maven\bin`.
