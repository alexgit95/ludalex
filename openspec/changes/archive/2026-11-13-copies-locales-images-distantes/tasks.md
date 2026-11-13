## 1. Persistance et stockage

- [x] 1.1 Ajouter la persistance des copies par URL exacte, contrainte d'unicite, etats, tentatives, echeances et erreurs bornees ; tester la compatibilite SQLite/PostgreSQL et mettre a jour `docs/database.puml`.
- [x] 1.2 Ajouter le telechargement en flux vers un temporaire du volume, controles SSRF/redirections/DNS, delais, limite 2 Mo et validation reelle des formats ; couvrir chaque regle par des tests unitaires.
- [x] 1.3 Publier atomiquement les fichiers valides et restituer les copies via le stockage protege existant ; tester echec disque, contenu partiel, type MIME, chemins dangereux et fichiers absents.

## 2. Collecte automatique et cycle de vie

- [x] 2.1 Decouvrir par lots les references jeux/joueurs au demarrage et periodiquement ; enregistrer les nouvelles references apres commit, imports/restaurations compris, avec deduplication des URL et tests d'idempotence.
- [x] 2.2 Implementer le worker unique et la progression durable, trois tentatives transitoires maximum avec attentes d'une puis quinze minutes, refus definitifs et reprise apres arret ; tester avec horloge controlee sans attentes reelles.
- [x] 2.3 Gerer changement/suppression de reference, copies partagees, temporaires/orphelins et fichiers disparus ; tester les courses entre edition et publication et l'absence de requetes reseau dans les formulaires ou l'affichage.

## 3. Affichage avec repli

- [x] 3.1 Exposer aux vues l'URL prioritaire et la copie disponible avec resolution par lots, sans requete JPA par image ; conserver les sources URL/upload et les couleurs existantes.
- [x] 3.2 Appliquer la chaine distante puis locale puis secours terminal aux fragments, fiches, listes, apercus, ecran d'affichage et diaporama, y compris apres mises a jour HTMX et pour les images decoratives.
- [x] 3.3 Tester dans le navigateur les images distantes valides, 404, refus, erreurs reseau et contenu indecodable, copies absentes ou invalides et secours terminal invalide ; verifier absence de boucle et absence d'effet sur les requetes non images.

## 4. Administration

- [x] 4.1 Ajouter au sein de l'administration existante les compteurs agreges, espace occupe, liste paginee, fiches concernees, dates, tentatives et erreurs, avec actualisation HTMX bornee.
- [x] 4.2 Ajouter relance individuelle et relance des echecs par POST avec CSRF ; tester les transitions, le budget de tentatives, l'echappement des donnees et les protections dev/prod sans nouvel acces via `/fin/**`.

## 5. Sauvegarde et restauration

- [x] 5.1 Versionner le nouveau ZIP et exporter en flux les associations et copies disponibles dedupliquees, sans historique technique ni fichiers partiels ; tester la coherence lors de publication/nettoyage concurrent.
- [x] 5.2 Restaurer et valider les associations/fichiers, maintenir la lecture des anciens ZIP et planifier les copies manquantes ; coordonner le worker avec la restauration et tester le retour a l'etat precedent sur echec.
- [x] 5.3 Tester les aller-retours avec et sans copies, les references partagees, le format historique, les associations manquantes, types/tailles/chemins invalides et l'independance SQLite/PostgreSQL.

## 6. Documentation et verification finale

- [x] 6.1 Mettre a jour README et la regle d'images de `.github/copilot-instructions.md` ; documenter traitement auto, suivi admin, limites du repli, volume persistant, anciens ZIP et procedure de retour arriere.
- [x] 6.2 Ajouter une entree Keep a Changelog avec rubrique `[BREAKING CHANGE]` et marche a suivre ; incrementer la version majeure dans le POM et le changelog de facon identique, sans modifier les pipelines.
- [x] 6.3 Executer les tests Java avec le binaire Maven de `C:/USINE_LOGICIELLE/apache-maven/bin`, puis la suite de regression ; verifier la collecte existante/nouvelle, la reprise et la sobriete des requetes avec un jeu de donnees pagine.
- [x] 6.4 Verifier l'administration et le repli en navigateur sur desktop/mobile, notamment apres HTMX, puis valider le change avec `openspec validate copies-locales-images-distantes --strict` avant de cocher les dernieres taches.

## Bilan de verification

- Suite Maven complete : 618 tests sans echec ; ajout ensuite d'un test de coordination export/worker et execution ciblee reussie (619 tests au total dans les rapports).
- SQLite reel : persistance des copies et echeances, instance navigateur avec une seule connexion, restauration v3 et collecte automatique d'une nouvelle URL avec refus SSRF.
- PostgreSQL : requetes et schema verifies avec son dialecte sur H2 en mode PostgreSQL/UTC ; aucun serveur PostgreSQL reel n'a ete utilise. Cette limite vaut aussi pour les aller-retours entre moteurs.
- Navigateur : distant valide prioritaire ; 404, 403, reseau et contenu indecodable vers la copie ; copie absente/invalide vers le defaut ; dernier secours en erreur sans handler actif ; erreur non image sans effet.
- HTMX : apercu de jeu insere dynamiquement avec repli, suivi admin remplace sans rechargement, relance HTTP 200 vers EN_ATTENTE avec zero tentative.
- Responsivite : controles desktop et viewport mobile 390 x 844, aucune largeur excessive de page et defilement horizontal limite au tableau. Le navigateur integre masque l'onglet : images lazy forcees en eager uniquement dans le test et attentes avec polling temporel.
- OpenSpec : validation stricte reussie avant cloture des cases.