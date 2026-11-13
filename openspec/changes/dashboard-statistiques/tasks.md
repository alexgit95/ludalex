## 1. Réglages persistés (lot 1)

- [x] 1.1 Créer l'entité `Reglage` (`cle` unique, `valeur`) et son dépôt ; mettre à jour `docs/database.puml`
- [x] 1.2 Créer le catalogue `ReglageCle` (type, défaut, bornes, libellé) avec les trois seuils : jeu dormant 6 mois, classement 5 parties, prêt long 30 jours
- [x] 1.3 Créer `ReglagesService` (lecture avec défaut, écriture validée, rétablissement, publication de `DonneesModifiees`)
- [x] 1.4 Créer la page d'administration `/admin/reglages` (formulaire, validation par champ, CSRF, lien depuis `/admin`)
- [x] 1.5 Tests : défauts, clé inconnue, bornes, rétablissement, contrôleur (CSRF, valeur invalide), JWT exigé en `prod`

## 2. Sauvegarde et restauration v4 (lot 1)

- [x] 2.1 Exporter `donnees/reglages.json` et le compteur `reglages` du manifeste, format `formatVersion` 4
- [x] 2.2 Valider avant toute suppression : clé au catalogue, valeur valide, doublons, compteur
- [x] 2.3 Restaurer les réglages dans la transaction existante ; vider la table pour les ZIP v1 à v3
- [x] 2.4 Étendre le test d'exhaustivité des entités exportées à `Reglage`
- [x] 2.5 Tests : aller-retour v4, restauration v1 à v3 (défauts), clé inconnue, valeur invalide, compteur incohérent

## 3. Cache et invalidation (lot 1)

- [x] 3.1 Créer l'événement `DonneesModifiees` et `CacheStatistiques` (une entrée par période, TTL 10 min, `Clock` injecté, verrou par période, `invalider()` en `afterCommit`)
- [x] 3.2 Publier l'événement depuis `PartieService`, `PartieEnCoursService` (lancement, pause, reprise, fin, token), `JeuService`, `PretService`, `JoueurService`, `ReglagesService`, la restauration et l'import MyLudo
- [x] 3.3 Tests : cache valide, expiration, invalidation par chaque service, calcul unique en accès concurrent, invalidation après commit uniquement

## 4. Socle du tableau de bord (lot 1)

- [x] 4.1 Créer `PeriodeStatistiques` (30 jours, 12 mois, année civile, tout ; bornes et période précédente) avec repli sur 12 mois
- [x] 4.2 Créer `StatistiquesGlobalesRepository` (requêtes agrégées, parties terminées uniquement, anonymisés exclus des classements) et `InstantaneStatistiques`
- [x] 4.3 Créer `TableauDeBordController` (`/statistiques`, blocs `/statistiques/bloc/{nom}`), ajouter l'onglet « Statistiques » à la navigation et au menu burger
- [x] 4.4 Créer les fragments Thymeleaf SVG/CSS réutilisables (`barres`, `barresH`, `anneau`, `calendrier`, `courbe`, `lignes`), styles dans `ludalex.css` aux variables de la charte, `<title>` et tableau alternatif, état vide
- [x] 4.5 Ajouter l'index `partie(date_partie, statut)` ; `partie(jeu_id, statut)` et `participation(joueur_id)` existent déjà
- [x] 4.6 Tests : période et repli, rendu des fragments (markup, vide, aucune ressource externe), contrôleur, JWT exigé en `prod`, nombre de requêtes constant

## 5. Chiffres clés, jeux et classement joueurs (lot 1)

- [x] 5.1 Chiffres clés : parties et évolution, jeux joués, joueurs actifs, jeux et coopératifs, valeur et prix moyen avec comptage des jeux sans prix, prêts, série de semaines
- [x] 5.2 Parties par mois, par jour de semaine ; top 10 jeux joués ; jeux jamais joués ; répartition coopératif/compétitif ; classement joueurs avec minimum de parties
- [x] 5.3 Tests sur base réelle : parties en cours ignorées, anonymisés, coopératif, évolution, valeurs partielles

## 6. Activité et joueurs (lot 2)

- [x] 6.1 Calendrier de chaleur, cumul de parties, répartition par lieu (« Non renseigné »)
- [x] 6.2 Parties par joueur, taux de victoire, records de score, duos, polyvalence
- [x] 6.3 Tests sur base réelle : agrégation par date portable SQLite/PostgreSQL, scores facultatifs, duos, égalités

## 7. Jeux avancés, ludothèque et prêts (lot 3)

- [x] 7.1 Jeux plus gagnés, dormants (seuil configuré), taux de victoire coopératif et jeu le plus difficile, variantes, coût par partie
- [x] 7.2 Achats par mois et par année, investissement cumulé, jeux les plus chers, jeux sans prix ou sans date d'achat
- [x] 7.3 Prêts par mois, emprunteurs, durée moyenne, prêts en cours et longs (seuil configuré)
- [x] 7.4 Tests sur base réelle : seuils modifiés, jeux sans prix ou date, prêts longs

## 8. Finitions et livraison

- [x] 8.1 Test de volume : nombre de requêtes constant lorsque les données augmentent
- [x] 8.2 Mettre à jour `README.md` (onglet, périodes, cache, réglages, sauvegarde v4) et `docs/database.puml`
- [x] 8.3 Passer la version à 3.0.0 dans `pom.xml` et `CHANGELOG.md` avec `[Added]`, `[Changed]` et `[BREAKING CHANGE]` (sauvegarde v4, migration : conserver un ZIP v1 à v3 pour revenir en arrière) ; incrémenter le cache du service worker si les ressources statiques changent
- [x] 8.4 Exécuter la suite de tests avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
