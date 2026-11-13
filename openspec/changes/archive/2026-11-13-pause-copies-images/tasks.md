## 1. Etat et planification

- [x] 1.1 Creer `EtatCopies` (singleton, `AtomicBoolean` initialise par `ludalex.images.copies.demarrage-actif`, defaut `false`, `actif()`, `activer()`, `mettreEnPause()`, indication de la desactivation par `ludalex.images.copies.actif`)
- [x] 1.2 Garder `PlanificationCopies.traiter()` : retour immediat en pause ; laisser `reconcilier()`, `initialiser()` et `traiterUne()` inchanges
- [x] 1.3 Tests : demarrage en pause par defaut, demarrage actif par propriete, aucun telechargement ni tentative en pause, inscription des nouvelles references en pause, reprise avec echeances passees, telechargement en cours termine apres la pause

## 2. Administration

- [x] 2.1 Ajouter `POST /admin/images/activer` et `POST /admin/images/pause` dans `CopiesImagesController` (CSRF, fragment `suivi` actualise, idempotents)
- [x] 2.2 Afficher dans `admin/images.html` un bandeau d'etat (active, en pause, pause avec travail en cours, desactive par configuration) et le bouton Activer / Mettre en pause dans le fragment `suivi`
- [x] 2.3 Tests : activer, pause, commandes repetees, CSRF manquant, JWT exige en prod (401/403), echappement, bandeau selon l'etat, absence de boutons si desactive par configuration

## 3. Non-regression

- [x] 3.1 Auditer les `@SpringBootTest` : le POM désactive déjà le planificateur en test (`ludalex.images.copies.actif=false`), aucun test ne dépend du scheduler ; les nouveaux tests d'intégration l'activent explicitement
- [x] 3.2 Executer la suite complete avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`

## 4. Documentation et livraison

- [x] 4.1 Mettre a jour `README.md` (pause par defaut, bouton admin, propriete `demarrage-actif`, comportement du suivi en pause), `docker-compose.yml`, `docker-compose.portainer.yml` et `.env.example` (variable `LUDALEX_IMAGES_COPIES_DEMARRAGE_ACTIF`)
- [x] 4.2 Passer la version a 4.0.0 dans `pom.xml` et `CHANGELOG.md` avec `[Changed]` et `[BREAKING CHANGE]` (migration : activer depuis l'administration ou definir `LUDALEX_IMAGES_COPIES_DEMARRAGE_ACTIF=true`) ; incrementer le cache du service worker si besoin
