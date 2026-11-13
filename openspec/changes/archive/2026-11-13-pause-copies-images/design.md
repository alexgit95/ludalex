## Context

`PlanificationCopies` exécute `traiterUne()` chaque seconde (téléchargement d'une copie) et `reconcilier()` chaque minute (inscription des URL en base, purge des orphelins, remise en attente des fichiers disparus). `initialiser()` fait un nettoyage local puis une réconciliation au démarrage. Seul `traiterUne()` accède au réseau.

## Goals / Non-Goals

**Goals:**
- Ne télécharger aucune image tant que l'utilisateur n'a pas activé la récupération, après chaque démarrage.
- Piloter l'état depuis l'administration, sans redémarrage.
- Garder le suivi informatif : les copies à faire restent visibles en attente.

**Non-Goals:**
- Persister l'état (choix A : retour en pause à chaque redémarrage).
- Interrompre un téléchargement en cours.
- Modifier l'affichage, la sauvegarde ou le schéma.

## Decisions

**Interrupteur en mémoire (`EtatCopies`).** Bean singleton avec un `AtomicBoolean` initialisé par `ludalex.images.copies.demarrage-actif` (défaut `false`). Pas de table ni de réglage persisté : l'état volatil est ce qui est demandé, et la sauvegarde v4 reste inchangée. Alternative écartée : table `reglage` (mémorise l'état, mais ne répond pas à « en pause à chaque démarrage » et restaurerait l'état d'une autre machine).

**Garde dans le planificateur, pas dans `CopiesImages`.** `PlanificationCopies.traiter()` retourne immédiatement si l'état est en pause ; `traiterUne()` reste inchangé. Les tests unitaires du worker, qui l'appellent directement, ne sont pas touchés, et la pause évite aussi la requête en base chaque seconde. `reconcilier()` et `initialiser()` ne sont pas gardés : ils n'utilisent pas le réseau et alimentent les compteurs « en attente ».

**Pause coopérative.** Un téléchargement en cours se termine (il est déjà borné par les délais et 2 Mo) ; aucun nouveau n'est pris. L'interface affiche « En pause » et, si le compteur « en cours » est non nul, que ce travail se termine. Une copie en attente ne consomme aucune tentative pendant la pause et reste éligible à la reprise (échéances passées incluses).

**Commandes POST idempotentes.** `POST /admin/images/activer` et `POST /admin/images/pause` avec CSRF, via HTMX sur le fragment `suivi` (même schéma que « Relancer »), renvoient le fragment actualisé. Mêmes règles d'accès dev/prod, aucune exposition par `/fin/**`.

**Relation avec `ludalex.images.copies.actif`.** Cette propriété reste l'arrêt complet par configuration (le planificateur n'existe pas). Si elle est à `false`, le bandeau indique « Traitement désactivé par la configuration » et les boutons sont absents.

## Risks / Trade-offs

- [Les installations existantes cessent de télécharger après la mise à jour] → `[BREAKING CHANGE]` documenté, version 4.0.0, propriété `demarrage-actif` pour reproduire l'ancien comportement.
- [Un redémarrage remet silencieusement en pause] → bandeau d'état toujours visible sur la page, et mention dans le README.
- [Les tests d'intégration comptent sur le scheduler] → les démarrer avec `ludalex.images.copies.demarrage-actif=true`, vérifié par un audit des `@SpringBootTest` concernés.
- [Les images nouvelles n'ont que leur URL distante pendant la pause] → comportement attendu : la chaîne d'affichage reste distante puis copie disponible puis secours.
