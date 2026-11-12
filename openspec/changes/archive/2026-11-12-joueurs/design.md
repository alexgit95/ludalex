## Context

Application mono-utilisateur : un joueur n'est qu'une fiche, sans lien avec Cloudflare ni compte. Il est référencé par les participations des parties (change `parties`) ; la suppression ne doit donc pas casser l'historique. Précondition : `images`.

## Goals / Non-Goals

**Goals:**
- CRUD d'administration des joueurs, images par upload ou URL.
- Suppression par anonymisation sans perte d'historique.

**Non-Goals:**
- Comptes, rôles, authentification des joueurs.
- Réactivation d'un joueur anonymisé.

## Decisions

- **Entité `Joueur`** : `id`, `nom`, `avatar` (`ImageSource`), `imageVictoire` (`ImageSource`), `anonymise` (booléen). Types standards, compatibles SQLite et PostgreSQL.
- **Anonymisation plutôt que suppression en cascade ou interdiction** (décision projet) : `nom` = « Joueur supprimé », images à `null`, fichiers supprimés, `anonymise = true`. Les clés étrangères des participations restent valides. Alternative rejetée : suppression physique avec `ON DELETE SET NULL`, qui perdrait l'attribution des victoires dans les statistiques.
- **Suppression physique uniquement sans participation** : vérifiée par une requête `existsBy…` (le dépôt des participations n'existant pas encore, la vérification est portée par une interface `ParticipationsJoueur` implémentée par `parties`; avant ce change elle renvoie « aucune »).
- **Unicité du nom** insensible à la casse, appliquée dans le service (la base SQLite/PostgreSQL ne partage pas le même comportement de collation) ; les joueurs anonymisés sont exclus de la règle.
- **Pagination** JPA (`Pageable`, taille 20) pour la liste.
- **Écran** : MVC Thymeleaf avec fragments HTMX pour le formulaire et la confirmation (pas d'API REST, donc pas d'annotations OpenAPI).

## Risks / Trade-offs

- [Collision de nom après anonymisation] → les anonymisés sont exclus de la règle d'unicité.
- [Fichier supprimé mais transaction annulée] → suppression des fichiers après commit.
- [Jeu de données volumineux] → pagination obligatoire.
