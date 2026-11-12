## 1. Lecture des exports

- [x] 1.1 Créer les modèles intermédiaires (`JeuMyLudo`, `PartieMyLudo`, `JoueurPartieMyLudo`) et les lecteurs en flux des deux exports JSON, isolés du reste (`LecteurCollectionMyLudo`, `LecteurPartiesMyLudo`)
- [x] 1.2 Reconnaître le type de chaque fichier par ses champs, accepter un seul des deux fichiers, refuser un fichier non reconnu
- [x] 1.3 Reporter les erreurs de lecture avec fichier, numéro d'entrée, jeu, date, champ, ligne et colonne pour un JSON mal formé
- [x] 1.4 Utiliser les exemples anonymisés de `docs/exemple-my-ludo/` comme jeu d'essai (copie réduite sous `src/test/resources`)
- [x] 1.5 Tests : fichier valide, fichier d'un autre type, JSON mal formé, champ invalide, tableau vide, caractères échappés (`\u0027`)

## 2. Analyse et rapprochement

- [x] 2.1 Normaliser les noms (casse, accents, espaces, apostrophes) pour jeux et joueurs
- [x] 2.2 Rapprocher les jeux de la collection et des parties avec l'existant ; déduire `cooperatif` (partie `Coop: Oui`, sinon mécanisme « Coopération ») ; prix 0 vide ; date d'achat future vide avec avertissement
- [x] 2.3 Rapprocher les joueurs, numéroter les doublons de nom d'une partie (« Anonyme », « Anonyme 2 ») et réutiliser ces fiches
- [x] 2.4 Convertir les parties : scores tous nuls vides, résultat coopératif, équipes (4 maximum), lieu tronqué à 100, règles d'ignorance avec raison (exclue, date future, sans joueur, non coopérative sans gagnant)
- [x] 2.5 Détecter les parties déjà présentes par appariement à occurrences (même jeu, même jour, joueur commun, une partie en base absorbe une seule partie du fichier) avec chargement agrégé des parties existantes
- [x] 2.6 Produire le `PlanImport` (compteurs, parties à ajouter, déjà présentes, ignorées par raison, avertissements, correspondance des joueurs)
- [x] 2.7 Tests : jeu existant, joueur existant, nouveau joueur, doublon « Anonyme », manches identiques du même jour (Toy Battle, MicroMacro), recouvrement partiel, partie coopérative perdue, non coopérative sans gagnant, équipes, deuxième import, export plus récent

## 3. Import et progression

- [x] 3.1 Implémenter l'import atomique (une transaction, lots de 500) : création des jeux, joueurs, parties, équipes, participations ; complément des seuls prix et dates d'achat vides ; aucune modification ni suppression existante
- [x] 3.2 Implémenter la tâche en mémoire à une seule instance (phase, `fait/total`, élément courant, résultat) avec verrou et expiration
- [x] 3.3 Lire les fichiers une seule fois et conserver les enregistrements en mémoire entre analyse et import, les oublier à la fin, à l'annulation ou à l'expiration
- [x] 3.4 Construire le rapport de réussite (compteurs, ignorées avec raison, avertissements plafonnés à 200 lignes) et d'erreur diagnostiquable sans détail interne
- [x] 3.5 Tests : atomicité (erreur au milieu), progression, refus concurrent, expiration de la session, ré-évaluation du delta avant écriture, idempotence

## 4. Interface Admin

- [x] 4.1 Créer la page `/admin/import-myludo` (envoi des deux fichiers) et le lien depuis l'Admin
- [x] 4.2 Afficher l'analyse (compteurs, avertissements) et l'écran de correspondance des joueurs avec confirmation
- [x] 4.3 Afficher la barre de progression (`<progress>`, phase, compteur, élément en cours) mise à jour par HTMX toutes les secondes (204 si inchangé)
- [x] 4.4 Afficher le message de réussite avec compteurs ou le message d'erreur diagnostiquable
- [x] 4.5 Protéger par CSRF et JWT (401 en `prod` sans JWT), refuser fichiers trop volumineux ; tests du contrôleur (CSRF, 401, fichier trop gros, fichier invalide, parcours complet)

## 5. Documentation et livraison

- [x] 5.1 Documenter dans `README.md` l'import MyLudo (fichiers attendus, règle du delta, champs non importés, progression et rapport)
- [x] 5.2 Mettre à jour `CHANGELOG.md` et incrémenter la version du `pom.xml` à la même valeur dans les deux fichiers (`docs/database.puml` inchangé)
- [x] 5.3 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
