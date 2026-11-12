## Context

Les parties relient jeux et joueurs. Ce change gère les parties **terminées** déclarées après coup ; les parties en cours et en pause (statut, token) viennent avec `partie-en-cours`, qui réutilise le même modèle. Décisions projet : plusieurs gagnants possibles, scores facultatifs, victoire coopérative comptant pour chaque participant, lieu en texte libre avec autocomplétion sur les lieux déjà saisis, pas de géolocalisation ni de géocodage.

## Goals / Non-Goals

**Goals:**
- Modèle `Partie`/`Participation`/`Equipe` extensible aux statuts « en cours » et « en pause ».
- Validation métier complète et testée (compétitif, coopératif, équipes).
- Autocomplétion de lieux sobre.

**Non-Goals:**
- Statistiques (change `statistiques`).
- Lancement, pause et QR (change `partie-en-cours`).
- Géolocalisation du navigateur, géocodage inverse.

## Decisions

- **Entité `Partie`** : `id`, `jeu`, `datePartie` (`LocalDate`), `lieu` (nullable, 100), `statut` (`TERMINEE` | `EN_COURS` | `EN_PAUSE`, seul `TERMINEE` est utilisé ici), `resultatCooperatif` (`VICTOIRE`|`DEFAITE`|null), `joueurSuivant` (nullable, utilisé par la pause). Les champs de pause sont créés ici pour éviter une évolution de schéma sans migration.
- **`Participation`** : `id`, `partie`, `joueur`, `score` (`Integer`, nullable), `gagnant` (booléen), `equipe` (nullable). Unicité (`partie`, `joueur`).
- **`Equipe`** : `id`, `partie`, `nom`. Supprimée avec la partie (`orphanRemoval`).
- **Gagnant calculé à l'écriture** : le service positionne `gagnant` selon le type de jeu (compétitif : gagnants désignés, ou membres de l'équipe désignée ; coopératif : tous si victoire, aucun si défaite). Les statistiques n'ont donc qu'à compter `gagnant = true`.
- **Autocomplétion** : requête `SELECT DISTINCT lieu FROM partie WHERE lower(lieu) LIKE :préfixe ORDER BY lieu LIMIT 10`, déclenchée par HTMX (`hx-trigger="keyup changed delay:300ms"`) vers un fragment `datalist`. Aucun appel sortant, donc pas de SSRF. Alternative rejetée : géocodage inverse (dépendance externe, fuite de position).
- **Formulaire dynamique** : le choix du jeu recharge par HTMX la zone de résultat (gagnants ou victoire/défaite).
- **Liste** : `Pageable` (20), filtre par jeu, tri par date décroissante puis `id`.
- **Interfaces `ParticipationsJoueur` et `PartiesJeu`** implémentées par ce change (requêtes `exists`).
- **MVC Thymeleaf** : aucune API REST, pas d'OpenAPI.

## Risks / Trade-offs

- [Données de pause créées avant leur usage] → champs nullables, documentés dans `docs/database.puml`.
- [Incohérence `gagnant` si les règles changent] → centralisé dans un seul service, couvert par tests.
- [Autocomplétion lente si beaucoup de parties] → requête bornée à 10 résultats ; index sur `lieu` si nécessaire.
- [Modification d'une partie qui change les stats] → acceptable, statistiques toujours calculées à la volée.
