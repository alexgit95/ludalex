## Context

Fonction de la **v2**. Le format est maintenant connu grâce aux exemples anonymisés de `docs/exemple-my-ludo/` : deux fichiers JSON, tableaux d'objets, `Titre` en clé de jeu.

- **collection** : `ID`, `Titre`, `Type` (`basegame` / `extension`), `Prix indicatif` (0 = inconnu), `Date d'acquisition`, mécanismes, thèmes, notes, `Dernière partie`...
- **plays** : `Titre`, `Date`, `Lieu` (vide possible), `Joueurs[{Nom, Score, Victoire, Équipe?}]`, `Vainqueurs[]`, `Coop`, `Coop Victoire`, `Exclue`, `Incomplète`, `Durée`, `Commentaire`...

Entités cibles existantes : `Jeu`, `Joueur`, `Partie`, `Participation`, `Equipe`. Contraintes à respecter : un joueur n'apparaît qu'une fois par partie, une partie non coopérative exige au moins un gagnant, 4 équipes au plus, date de partie et d'achat pas dans le futur, `Partie.lieu` ≤ 100 caractères.

## Goals / Non-Goals

**Goals:**
- Importer jeux possédés, joueurs et parties (date, lieu, scores, équipes, résultat coopératif) depuis les deux fichiers.
- N'ajouter que le delta : jamais d'écrasement ni de doublon ; import rejouable avec un export plus récent.
- Suivi visible de l'avancement et rapport de réussite ou d'erreur exploitable pour diagnostiquer.

**Non-Goals:**
- Synchronisation continue avec MyLudo, export vers MyLudo.
- Import de durée, commentaires, notes, images, métadonnées de jeu (éditeurs, thèmes...).
- Modification ou suppression de données existantes.

## Decisions

- **Deux fichiers, lecteurs isolés** : `LecteurCollectionMyLudo` et `LecteurPartiesMyLudo` lisent le tableau JSON en flux (Jackson, comme `LectureSauvegarde`) vers un modèle intermédiaire léger (`JeuMyLudo`, `PartieMyLudo`). Le format reste isolé pour absorber une évolution. Fichier absent accepté (au moins un des deux), chaque fichier déposé dans sa zone (`collection` ou `parties`) et vérifié par ses champs : un fichier dans la mauvaise zone est refusé avec un message explicite. Upload multipart : la limite existante de 2 Mo par fichier suffit largement (les exemples pèsent quelques centaines de Ko) et protège déjà de l'abus.
- **Trois temps** : (1) **analyse à blanc** sans écriture, produisant un `PlanImport` (jeux à créer / complétés / inchangés, joueurs à rapprocher, parties à ajouter / déjà présentes / ignorées, avertissements) ; (2) **correspondance des joueurs** et confirmation ; (3) **import**. Les fichiers sont lus une seule fois, l'annulation ou une expiration (30 min), puis supprimés en toutes circonstances.
- **Progression** : une tâche en mémoire à la fois (verrou, comme la restauration) expose phase, compteur `fait/total`, élément courant et résultat. La page l'interroge toutes les secondes par HTMX (`hx-trigger="every 1s"` vers `/admin/import-myludo/progression`) et affiche un `<progress>` accessible, puis le rapport final. Les fichiers sont petits : la progression porte sur le traitement (analyse et import), pas sur l'envoi. Le fragment répond 204 tant que rien ne change, comme l'écran Affichage.
- **Normalisation des noms** : minuscules, sans accents, espaces et ponctuation d'apostrophe unifiés, pour jeux et joueurs (« Galèrapagos », « Château Combo », « Ghost Fightin' Treasure Hunters »). Aucune colonne `myludo_id` : pas de changement de schéma ni de format de sauvegarde ; un jeu renommé dans MyLudo sera vu comme un nouveau jeu (signalé dans le rapport).
- **Jeux** : issus de la collection (et des parties pour les jeux qui n'y figurent pas). Création avec `nom`, `prix` (0 ou absent = vide), `dateAchat` (une date future est laissée vide avec avertissement), `cooperatif` déduit : au moins une partie `Coop: Oui` dans `plays`, sinon mécanisme exact « Coopération » de la collection (« Semi-coopération » exclu). Un jeu existant n'est **pas modifié**, seuls `prix` et `dateAchat` vides sont complétés. Une extension est importée comme un jeu ordinaire. Pas d'image : couleur de fond neutre.
- **Joueurs** : rapprochement par nom normalisé avec un joueur actif existant, sinon création ; l'utilisateur peut à l'analyse associer un nom à un autre joueur ou forcer la création. Les doublons de nom dans une même partie (« Anonyme » ×2) reçoivent un suffixe de position (« Anonyme », « Anonyme 2 ») et ces fiches sont réutilisées entre parties.
- **Parties** : `statut TERMINEE`, `termineeLe` vide (l'affichage ne déclenche pas d'animation de victoire). `Score` : si tous les scores d'une partie valent 0, aucun score n'est enregistré, sinon ils sont conservés. `Victoire` donne `gagnant`. Jeu coopératif : `Coop Victoire` donne `resultatCooperatif` (VICTOIRE/DEFAITE) et la liste des gagnants est ignorée. `Équipe` crée les équipes de la partie (4 maximum, sinon avertissement et équipes ignorées). `Lieu` vide reste vide, au-delà de 100 caractères il est tronqué avec avertissement. **Ignorées avec raison dans le rapport** : `Exclue: Oui`, date dans le futur, partie non coopérative sans gagnant, aucun joueur, jeu introuvable. Les parties `Incomplète`, solo et en ligne sont importées.
- **Delta seul, parties déjà présentes** : une partie du fichier est un conflit s'il existe, pour le même jeu et le même jour, une partie en base qui partage **au moins un joueur** avec elle. Chaque partie en base ne peut absorber **qu'une** partie du fichier (appariement : correspondance exacte des joueurs d'abord, puis meilleur recouvrement) ; les occurrences restantes du fichier sont ajoutées. Plusieurs manches identiques le même jour restent donc possibles et un second import du même fichier n'ajoute rien. Les parties existantes ne sont jamais modifiées, même si lieu ou scores diffèrent.
- **Atomicité** : l'import écrit en une seule transaction (insertion par lots de 500, `flush`/`clear`) ; en cas d'erreur, rien n'est enregistré. La progression est mise à jour pendant la transaction ; les parties existantes sont chargées par requête agrégée (jeu, date, joueurs) et non entité par entité.
- **Rapport et diagnostic** : succès = compteurs (jeux créés / complétés / inchangés, joueurs créés / associés, parties ajoutées / déjà présentes / ignorées par raison, avertissements) ; erreur = fichier, numéro d'entrée, jeu et date, champ et cause, ligne et colonne pour un JSON mal formé, sans détail interne ni trace. Les listes détaillées sont plafonnées (200 lignes) avec leur total. Le rapport reste affiché jusqu'à fermeture ou nouvelle analyse.
- **Sécurité** : même protection d'accès que l'Admin (JWT Cloudflare en `prod`), CSRF sur les envois, aucun appel sortant (pas d'image, pas de SSRF), JSON lu en flux avec contraintes Jackson par défaut, valeurs bornées (longueurs, dates, nombres) comme pour la restauration, aucun contenu du fichier interprété comme du HTML (échappement Thymeleaf).
- **Interface** : page `/admin/import-myludo` liée depuis l'Admin ; étapes visibles (fichiers, analyse, correspondance des joueurs, import, rapport). MVC Thymeleaf : pas d'API REST, pas d'OpenAPI. Indépendante de la charte graphique en cours de refonte (`charte-heroic-fantasy`), dont elle reprendra les composants.

## Risks / Trade-offs

- [Parties légitimes identiques le même jour] → appariement à occurrences (pas de clé unique), vérifié par tests sur les cas réels (Toy Battle, MicroMacro).
- [Recouvrement partiel considéré comme conflit] → une partie du fichier avec un joueur de plus qu'en base n'est pas ajoutée ; documenté dans le rapport.
- [Données MyLudo incohérentes] (jeu coopératif marqué `Coop: Non` sans gagnant) → déduction du caractère coopératif par jeu, sinon partie ignorée avec raison, jamais d'erreur bloquante.
- [Faux rapprochements de noms] → correspondance validée par l'utilisateur à l'analyse, jeux renommés signalés.
- [Fichier modifié entre analyse et import] → les fichiers conservés sont ceux de l'analyse ; l'import ré-évalue le delta en base avant d'écrire.
- [Perte de durée, commentaire, notes] → assumée, listée dans le README.

## Open Questions

- Aucune bloquante ; les cas non prévus de futurs exports (nouveaux champs) sont absorbés par l'isolement des lecteurs.
