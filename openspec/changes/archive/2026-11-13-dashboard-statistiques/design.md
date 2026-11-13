## Context

Les statistiques actuelles (`statistiques/`) ne couvrent qu'un jeu à la fois, via des requêtes agrégées JPA. L'application est mono-utilisateur, déployée sur Raspberry Pi (`-Xmx384m`), avec SQLite en dev et PostgreSQL en prod, sans chaîne de build front. La sauvegarde (format v2) vérifie l'exhaustivité des entités exportées. Les dates de parties sont des `DATE` ; seule `terminee_le` porte une heure et seulement pour les parties en direct.

## Goals / Non-Goals

**Goals:**
- Tableau de bord global rapide à l'affichage, cohérent avec la charte, sans dépendance JavaScript.
- Calculs sobres (requêtes agrégées, nombre constant de requêtes) et cache de 10 minutes toujours cohérent avec les données.
- Seuils configurables, persistés et sauvegardés.

**Non-Goals:**
- Durée de partie, heure de jeu, retards de prêt (données absentes).
- Bouton d'actualisation, interactivité riche des graphiques, export des statistiques.

## Decisions

**Rendu SVG/CSS par Thymeleaf plutôt que Chart.js.** Pas de JS à télécharger ni à initialiser après chaque swap HTMX, cohérence totale avec les variables CSS, accessibilité simple, testabilité du markup. Les diagrammes sont des fragments réutilisables (`kpi`, `barres`, `barres-horizontales`, `donut`, `chaleur`) alimentés par des modèles de vue déjà normalisés (valeurs, libellés, pourcentages) ; les calculs d'échelle sont faits en Java, pas dans le gabarit. Alternative écartée : Chart.js (200 Ko, cache du service worker, canvas opaque).

**Un instantané par période, mis en cache en mémoire.** `CacheStatistiques` garde une entrée par période (4 au plus) contenant un objet immuable `InstantaneStatistiques` complet, avec l'horodatage du calcul pris sur le `Clock` injecté. Les blocs HTMX lisent le même instantané, ce qui garantit des totaux cohérents. TTL de 10 minutes. `invalider()` vide toutes les entrées. Un verrou par période évite deux calculs simultanés (le second attend le premier). Alternatives écartées : Spring Cache/Caffeine (dépendance pour 4 entrées), un cache par bloc (incohérences).

**Invalidation sur tous les événements d'écriture.** L'invalidation est déclenchée après validation de la transaction (`TransactionSynchronization.afterCommit`) pour qu'un recalcul concurrent ne lise pas d'état antérieur. Elle est branchée sur l'ensemble des chemins d'écriture sans toucher chaque service : un écouteur JPA (`ChangementDonnees`, `@PostPersist/@PostUpdate/@PostRemove`) posé sur les entités `Jeu`, `Joueur`, `Pret`, `Partie`, `Participation`, `Equipe` et `Reglage` publie un événement `DonneesModifiees` une seule fois par transaction, après le commit (immédiatement hors transaction) ; la restauration le signale aussi explicitement pour couvrir une archive vide. Cela couvre parties (y compris fin et pause en direct ou via token), jeux, prêts, joueurs (dont anonymisation), réglages, restauration et import MyLudo, y compris les chemins futurs. Le cache écoute `DonneesModifiees` et évite tout couplage entre modules. Le bouton d'actualisation n'a ainsi plus d'utilité et est supprimé. Limite connue : les requêtes JPQL en masse ne déclenchent pas l'écouteur (seule la restauration en utilise, et elle signale explicitement).

**Calculs par requêtes agrégées, un seul passage par bloc.** Un `StatistiquesGlobalesRepository` fournit des projections (records) groupées en base ; seules les dates agrégées sont renvoyées. Le regroupement par mois, par jour de semaine et par jour pour le calendrier de chaleur se fait en comptant par `date_partie` (`select date_partie, count(*) group by date_partie`) puis en agrégeant en Java : portable SQLite/PostgreSQL sans fonction de date propre au moteur, volume borné (au plus un enregistrement par jour de jeu). Les parties non terminées sont exclues partout. Un test vérifie que le nombre de requêtes ne dépend pas du volume.

**Joueurs anonymisés.** Les totaux de parties (comptage par partie) les incluent ; tout classement nominatif joint sur `joueur.anonymise = false`, comme déjà fait pour les champions.

**Réglages : table générique `reglage(cle, valeur)`.** Un catalogue en code (`ReglageCle`) décrit chaque réglage (type, défaut, bornes, libellé) ; `ReglagesService` lit avec repli sur le défaut, valide à l'écriture, publie `DonneesModifiees`. Une table générique permet d'ajouter des réglages futurs sans changement de schéma. Clé unique, `valeur` en `VARCHAR`, types standards. La page d'administration `/admin/reglages` est protégée par le contrôle d'accès et CSRF.

**Sauvegarde v4 incluant les réglages.** Le format v3 (copies d'images) et la version 2.0.0 sont déjà livrés ; ce change les fait passer en v4 et 3.0.0. Ajout de `donnees/reglages.json` (liste de `{cle, valeur}`) exportée par lecture directe de la table, donc **tout** réglage présent, y compris futur, sans modifier le format. Le manifeste ajoute le compteur `reglages`. À la restauration, le fichier est validé avant toute suppression (clé présente au catalogue, valeur valide, doublons, compteur) puis la table est vidée et réinsérée dans la même transaction. Pour v1, v2 et v3, la table est simplement vidée. L'exhaustivité des entités exportées inclut la nouvelle entité. Conséquence : **breaking change**, version majeure 3.0.0 ; un ZIP v4 est refusé par les versions antérieures qui n'acceptent que v1 à v3. Alternative écartée : ne pas sauvegarder les réglages (rejetée par la décision produit).

**Période.** Enum `PeriodeStatistiques` (30 jours, 12 mois, année civile, tout) ; bornes calculées avec le `Clock` injecté ; la période précédente de même durée sert au calcul d'évolution. Paramètre inconnu : retour à 12 mois.

**Chargement par blocs HTMX.** `/statistiques` rend la page avec le sélecteur et des conteneurs `hx-get="/statistiques/bloc/{nom}?periode=..." hx-trigger="load"` ; chaque bloc lit l'instantané du cache. Les chiffres clés arrivent en premier. Le premier appel après expiration calcule et peuple le cache, les suivants le réutilisent.

**Index.** Index sur `partie(date_partie)`, `partie(jeu_id)` et `participation(joueur_id)` déclarés par les annotations JPA ; compatible SQLite et PostgreSQL.

## Risks / Trade-offs

- [Premier accès lent après expiration sur Raspberry Pi] → requêtes agrégées, index, calcul à un seul passage, pas de bloc à calcul superflu ; mesure par test de volume.
- [Cache non invalidé par un nouveau chemin d'écriture] → événement unique `DonneesModifiees` et test qui vérifie, pour chaque service d'écriture, que l'invalidation a lieu.
- [Invalidation avant commit puis recalcul sur l'ancien état] → invalidation en `afterCommit`.
- [Breaking change de la sauvegarde] → documenté en `[BREAKING CHANGE]`, version 3.0.0, restauration v1 à v3 conservée, conseil de garder un ZIP v1 à v3 pour un retour arrière.
- [Réglage inconnu dans un ZIP issu d'une version plus récente] → refus explicite avant toute suppression plutôt qu'ignorance silencieuse.
- [Lisibilité du SVG sur petit écran] → `viewBox` responsive, libellés tronqués, tableaux de données alternatifs, `prefers-reduced-motion` respecté.
- [Statistiques partielles (prix, dates, scores facultatifs)] → comptage des lignes exclues affiché dans le bloc concerné.

## Migration Plan

1. Déployer 3.0.0 : Hibernate crée la table `reglage` (`ddl-auto`) ; aucune migration de données.
2. Les réglages valent les défauts jusqu'à modification.
3. Retour arrière : restaurer une sauvegarde v1 à v3 prise avant la mise à jour ; un ZIP v4 ne peut pas être restauré par l'ancienne version.
