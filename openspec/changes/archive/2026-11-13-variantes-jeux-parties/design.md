## Context

`Jeu` contient les informations stables du jeu et `Partie` référence le jeu joué. Aucune campagne, enquête ou édition n'est actuellement enregistrée par partie. Le formulaire de partie passée actualise déjà un fragment HTMX quand le jeu change; le lancement direct crée la partie dès la sélection du jeu et les pages de sauvegarde exportent les entités vers des DTO JSON.

La base utilise `ddl-auto=update` sans outil de migration. SQLite et PostgreSQL doivent rester compatibles. Le ZIP de sauvegarde est actuellement au format v1, dont le validateur n'accepte explicitement que cette version.

## Goals / Non-Goals

**Goals:**
- Signaler sur un jeu s'il possède plusieurs campagnes, enquêtes ou versions.
- Stocker un libellé libre facultatif de 200 caractères maximum sur chaque partie concernée.
- Saisir la valeur sur une partie passée ou au lancement d'une partie en direct, puis l'afficher dans l'historique.
- Préserver les données dans les sauvegardes et restaurer les anciennes archives v1.

**Non-Goals:**
- Gérer un catalogue de variantes ou empêcher les différences d'orthographe.
- Ajouter un filtre ou des statistiques par campagne.
- Importer des variantes depuis MyLudo, qui ne fournit pas cette donnée.

## Decisions

- Ajouter un indicateur booléen à `Jeu`, initialisé à faux, et une chaîne nullable `variante` à `Partie`. Le libellé reste une valeur libre plutôt qu'une relation vers une nouvelle table : les termes peuvent désigner une campagne, une enquête ou une édition, et l'utilisateur veut pouvoir saisir directement le libellé réellement joué.
- Afficher le champ uniquement si le jeu sélectionné a l'indicateur activé. Le formulaire de partie passée réutilise le rafraîchissement HTMX du jeu sélectionné; le formulaire de lancement direct actualise également la disponibilité du champ lorsque le jeu change. Le lancement direct enregistre la variante dès la création de la partie, afin qu'elle survive aux pauses, reprises et terminaisons par QR.
- Garder les anciennes valeurs de partie si l'indicateur du jeu est ensuite désactivé. L'indicateur contrôle la saisie des nouvelles parties, pas l'affichage ni la conservation de l'historique.
- Étendre les DTO JSON du ZIP avec l'indicateur du jeu et le libellé de partie, et passer le format de sauvegarde à v2. Le nouveau code accepte v1 et v2; les données absentes d'un ZIP v1 deviennent faux/null. Les anciennes versions de l'application refusent les ZIP v2, donc elles ne doivent pas être utilisées pour restaurer une sauvegarde créée par cette version.
- Mettre à jour `docs/database.puml` et vérifier la mise à jour de schéma sur SQLite et PostgreSQL. Les parties MyLudo importées gardent une variante vide, car l'export source ne fournit pas cette information.

## Risks / Trade-offs

- [La saisie libre peut produire plusieurs orthographes pour la même campagne] → Accepté pour éviter un écran de gestion et permettre des libellés différents; le filtre par campagne n'est pas inclus.
- [La colonne booléenne ajoutée à `jeu` doit avoir une valeur sûre pour les lignes existantes] → Garantir le défaut faux avec la stratégie de schéma Hibernate compatible SQLite/PostgreSQL et tester une mise à jour de base contenant déjà des jeux.
- [Une ancienne version ne peut pas restaurer un ZIP v2] → Accepter explicitement les ZIP v1 dans la nouvelle version, incrémenter la version applicative majeure et indiquer dans la documentation de mettre l'application à jour avant de restaurer un ZIP v2.

## Migration Plan

Aucune migration manuelle n'est prévue : Hibernate ajoute le champ de jeu et le champ facultatif de partie, avec une valeur faux pour les jeux existants. L'export passe au format v2; la restauration accepte v1 (variante absente) et v2. Avant un retour à une ancienne image applicative, utiliser une sauvegarde v1 créée avant la mise à jour; une sauvegarde v2 n'est pas restaurable par ces anciennes versions.

## Open Questions

Aucune.
