## MODIFIED Requirements

### Requirement: Collecte automatique des images referencees par URL
Le systeme SHALL collecter automatiquement toutes les sources d'image HTTP(S) referencees par l'application, jeux et joueurs compris, existantes et futures, y compris apres import ou restauration. L'inscription des copies a faire SHALL etre automatique et ne SHALL dependre ni de l'etat de la recuperation ni du reseau ; le telechargement SHALL n'avoir lieu que lorsque la recuperation est active. Le traitement SHALL etre asynchrone, pagine et ne SHALL pas bloquer une sauvegarde de fiche sur le telechargement de sa copie.

#### Scenario: Rattrapage des images existantes
- **WHEN** l'application demarre avec des images par URL sans copie locale
- **THEN** les URL sont decouvertes par lots et leur copie est planifiee en attente sans action admin et sans attendre un affichage, et telechargee des que la recuperation est active

#### Scenario: Nouvelle reference et import
- **WHEN** une creation, une modification, un import ou une restauration valide une reference URL
- **THEN** une copie manquante est automatiquement planifiee en attente apres validation des donnees

#### Scenario: URL partagee
- **WHEN** plusieurs fiches referencent exactement la meme URL
- **THEN** une seule copie et un seul travail actif sont conserves pour cette URL

## ADDED Requirements

### Requirement: Recuperation en pause par defaut au demarrage
Le systeme SHALL demarrer avec la recuperation des copies en pause, sans persister l'etat : chaque demarrage repart en pause sauf si la propriete `ludalex.images.copies.demarrage-actif` vaut `true`. En pause, aucun nouveau telechargement ne SHALL etre demarre, aucune tentative ne SHALL etre consommee et les copies en attente SHALL rester en attente ; l'inscription des references, la purge des orphelins et le nettoyage local SHALL continuer.

#### Scenario: Demarrage par defaut
- **WHEN** l'application demarre sans la propriete `demarrage-actif`
- **THEN** la recuperation est en pause et aucune image n'est telechargee

#### Scenario: Demarrage actif par configuration
- **WHEN** l'application demarre avec `ludalex.images.copies.demarrage-actif=true`
- **THEN** la recuperation est active des le demarrage

#### Scenario: Etat non memorise
- **WHEN** l'utilisateur a active la recuperation puis l'application redemarre sans la propriete
- **THEN** la recuperation est de nouveau en pause

#### Scenario: Nouvelles images pendant la pause
- **WHEN** une fiche reference une nouvelle URL pendant la pause
- **THEN** sa copie apparait en attente dans le suivi, sans tentative ni acces reseau

#### Scenario: Reprise
- **WHEN** la recuperation est activee alors que des copies attendent, y compris avec des echeances passees
- **THEN** elles sont traitees une par une avec leur budget de tentatives intact

### Requirement: Activation et pause depuis l'administration
L'administration des images distantes SHALL afficher l'etat de la recuperation (active ou en pause) et proposer d'activer ou de mettre en pause par une action protegee CSRF, sans redemarrage. Une mise en pause SHALL laisser se terminer le telechargement deja commence sans en demarrer d'autre. Les acces SHALL respecter les regles dev/prod existantes et ne SHALL pas etre exposes via les liens de fin de partie. Si le traitement est desactive par la propriete `ludalex.images.copies.actif=false`, la page SHALL l'indiquer sans proposer de commande.

#### Scenario: Activer
- **WHEN** l'utilisateur active la recuperation avec une requete autorisee et un jeton CSRF valide
- **THEN** l'etat devient actif et le suivi affiche la progression des copies en attente

#### Scenario: Mettre en pause pendant un telechargement
- **WHEN** l'utilisateur met en pause alors qu'une copie est en cours
- **THEN** cette copie se termine, aucune autre n'est demarree et le bandeau indique la pause

#### Scenario: Commande repetee
- **WHEN** l'utilisateur active une recuperation deja active ou met en pause une recuperation deja en pause
- **THEN** l'etat est inchange et la page est actualisee sans erreur

#### Scenario: Action non autorisee
- **WHEN** une commande arrive sans CSRF valide ou sans JWT valide en prod
- **THEN** elle est refusee sans modifier l'etat

#### Scenario: Traitement desactive par configuration
- **WHEN** `ludalex.images.copies.actif` vaut `false`
- **THEN** la page indique que le traitement est desactive par la configuration et ne propose aucun bouton
