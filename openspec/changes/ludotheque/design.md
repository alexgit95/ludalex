## Context

Le catalogue est la base des parties et des statistiques. La liste doit rester légère sur Raspberry Pi (pagination, vignettes sans chargement massif). La couleur de fond provient du change `images`.

## Goals / Non-Goals

**Goals:**
- CRUD des jeux, liste en vignettes paginée, fiche détail.
- Prêts avec historique, emprunteur en texte libre, un seul prêt en cours.

**Non-Goals:**
- Statistiques et symbole de champion (change `statistiques`), simplement un emplacement ici.
- Import depuis MyLudo (v2).

## Decisions

- **Entité `Jeu`** : `id`, `nom`, `image` (`ImageSource`), `couleurFond` (chaîne `#RRGGBB`), `prix` (`BigDecimal`, précision 10,2), `dateAchat` (`LocalDate`), `cooperatif` (booléen). Types standards pour SQLite et PostgreSQL.
- **Entité `Pret`** : `id`, `jeu` (`@ManyToOne` paresseux), `emprunteur` (texte), `datePret`, `dateRetour` (nullable). Un prêt en cours = `dateRetour` nul.
- **Un seul prêt en cours** : contrôle dans le service dans une transaction, avec verrou pessimiste sur le jeu ; une contrainte d'unicité filtrée n'étant pas portable entre SQLite et PostgreSQL, la garantie reste applicative. Alternative rejetée : champ `pretEnCours` dupliqué sur `Jeu` (risque d'incohérence).
- **Liste** : requête paginée (`Pageable`, 24 par page) ; l'emprunteur courant est chargé par une requête groupée sur la page affichée (pas de N+1).
- **Suppression d'un jeu avec parties refusée** : l'historique des parties a de la valeur ; le contrôle passe par une interface `PartiesJeu` (comme pour les joueurs) ; supprimer des parties relève du change `parties`.
- **Fiche détail** : fragment HTMX « statistiques » chargé à la demande, vide jusqu'au change `statistiques`.
- **MVC Thymeleaf** : pas d'API REST, pas d'annotations OpenAPI.

## Risks / Trade-offs

- [Concurrence sur les prêts] → verrou pessimiste + test ciblé.
- [Liste lente avec beaucoup de jeux] → pagination et requêtes groupées.
- [Image par URL qui disparaît] → image par défaut côté navigateur (`onerror`).
