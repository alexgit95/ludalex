## Context

Le champ `gagnant` de `Participation` est déjà positionné à l'écriture (change `parties`), ce qui permet des statistiques par simple comptage. Contraintes : Raspberry Pi, requêtes agrégées, compatibilité SQLite/PostgreSQL (JPQL standard ou SQL simple).

## Goals / Non-Goals

**Goals:**
- Classement, taux de victoire, champion(s) et dernières parties par jeu.
- Symboles de champion de toute une page de liste en un nombre constant de requêtes.

**Non-Goals:**
- Statistiques globales tous jeux confondus, par période ou par lieu.
- Mise en cache applicative (requêtes assez légères).

## Decisions

- **Requête de classement** (JPQL) : `SELECT p.joueur.id, COUNT(p), SUM(CASE WHEN p.gagnant THEN 1 ELSE 0 END) FROM Participation p WHERE p.partie.jeu.id = :jeu AND p.partie.statut = TERMINEE GROUP BY p.joueur.id`, avec projection vers un record (pas d'entités chargées).
- **Champion(s)** : calculés en mémoire sur le résultat agrégé (au plus le nombre de joueurs, petit) : maximum de victoires > 0, tous les joueurs à égalité, hors anonymisés (filtre en base).
- **Liste des champions pour une page** : une requête `GROUP BY jeu, joueur` restreinte à `jeu IN (:ids)` (ids de la page), puis calcul des maxima en mémoire par jeu ; évite le N+1.
- **Dernières parties** : requête paginée `Pageable` (10), puis chargement des participations de ces seules parties en une requête (`IN`).
- **Taux** : `victoires / parties` avec arrondi à l'entier le plus proche en affichage ; non défini si aucune partie.
- **Pas de cache** : les parties changent peu mais l'écran doit être exact après chaque déclaration ; les requêtes sont indexées (`participation(partie_id)`, `participation(joueur_id)`, `partie(jeu_id, statut)`).
- **Interface** : fragment HTMX `statistiques` chargé dans la fiche, fragment `champion` inclus dans les vignettes.

## Risks / Trade-offs

- [Égalité à 0 victoire] → aucun champion si le maximum est 0.
- [Index absents sur SQLite] → déclarés via `@Table(indexes = …)`, standard JPA.
- [Joueur anonymisé en tête] → exclu du titre de champion mais ses parties restent comptées pour les autres statistiques.
