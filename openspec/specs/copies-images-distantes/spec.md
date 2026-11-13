# copies-images-distantes Specification

## Purpose
Collecte automatique des copies locales de secours des images distantes, suivi administratif et coherence des references sur le volume persistant.

## Requirements

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

### Requirement: Telechargement de secours securise et borne
Le systeme SHALL telecharger au plus une image a la fois, en flux, avec protection SSRF a la connexion et a chaque redirection, delais bornes, limite de 2 Mo et verification du contenu reel JPEG, PNG, GIF ou WebP. Les fichiers SHALL etre publies sur le volume persistant sous des noms generes seulement apres validation complete.

#### Scenario: Copie valide
- **WHEN** une URL publique renvoie une image autorisee de moins de 2 Mo
- **THEN** une copie complete est publiee sur le volume et son etat devient disponible

#### Scenario: Destination interne
- **WHEN** une URL ou sa redirection vise une adresse privee, locale, de lien local ou de metadonnees cloud, y compris apres changement DNS
- **THEN** le telechargement est refuse sans acces a la destination interdite et aucune copie n'est publiee

#### Scenario: Fichier invalide ou trop volumineux
- **WHEN** une reponse depasse 2 Mo ou contient un document non image, meme annonce comme image
- **THEN** le travail passe en echec et le fichier temporaire est supprime

#### Scenario: Erreur reseau ou disque
- **WHEN** le reseau expire ou l'ecriture du fichier echoue
- **THEN** aucune copie partielle n'est restituee et l'erreur est visible dans le suivi sans empecher l'utilisation des fiches

### Requirement: Progression durable et tentatives limitees
Le systeme SHALL conserver les etats en attente, en cours, disponible ou en echec, les dates, le nombre de tentatives et la derniere erreur. Il SHALL reprendre les travaux interrompus apres redemarrage, sans retelecharger les copies disponibles. Les erreurs transitoires SHALL donner lieu a trois tentatives au maximum, espacees d'au moins une minute puis quinze minutes ; les refus de securite ou de format SHALL echouer sans relance automatique.

#### Scenario: Reprise apres arret
- **WHEN** l'application redemarre apres interruption d'un travail
- **THEN** le travail redevient eligible, les copies disponibles sont conservees et les fichiers temporaires abandonnes sont nettoyes

#### Scenario: Echec definitif
- **WHEN** la troisieme tentative transitoire echoue
- **THEN** le travail reste en echec, y compris lors des reconciliations suivantes, jusqu'a une relance admin explicite

### Requirement: Suivi et relance depuis l'administration
L'administration SHALL afficher des compteurs par etat, l'espace occupe par les copies et une liste paginee des references concernees, avec dates, nombre de tentatives et erreurs. Elle SHALL actualiser le suivi sans rechargement complet et permettre la relance d'un echec ou des echecs par une action protegee CSRF. Les acces SHALL respecter les regles dev/prod existantes et ne SHALL pas etre exposes via les liens de fin de partie.

#### Scenario: Consultation du traitement automatique
- **WHEN** l'utilisateur ouvre la rubrique admin pendant la collecte
- **THEN** il voit la progression, les fiches concernees et les erreurs, avec actualisation de la page courante sans charger toute la collection

#### Scenario: Relance manuelle
- **WHEN** l'utilisateur relance un ou plusieurs echecs avec une requete autorisee et un jeton CSRF valide
- **THEN** les travaux concernes repassent en attente avec un nouveau budget de tentatives et les copies disponibles ne sont pas retelechargees

#### Scenario: Action non autorisee
- **WHEN** une action de relance arrive sans CSRF valide ou sans JWT valide en prod
- **THEN** elle est refusee sans modifier les travaux

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

### Requirement: Coherence des references et des fichiers
Le systeme SHALL conserver une copie tant qu'au moins une fiche reference son URL et nettoyer les copies devenues orphelines. Une modification, suppression ou restauration concurrente ne SHALL jamais associer la copie d'une ancienne URL a une nouvelle source. Une copie referencee dont le fichier disparait SHALL etre remise en attente lors de la reconciliation.

#### Scenario: URL modifiee pendant un telechargement
- **WHEN** une fiche change d'URL avant la fin de la copie de l'ancienne
- **THEN** la copie de l'ancienne URL n'est pas utilisee pour la nouvelle et elle est nettoyee si plus aucune fiche ne l'utilise

#### Scenario: Suppression d'une reference partagee
- **WHEN** une fiche utilisant une copie partagee est supprimee
- **THEN** la copie reste disponible pour les autres fiches qui referencent cette URL

#### Scenario: Fichier manquant
- **WHEN** la reconciliation constate qu'une copie disponible reference un fichier absent
- **THEN** elle planifie une nouvelle copie sans declencher de telechargement dans la requete d'affichage

#### Scenario: Restauration pendant un travail
- **WHEN** une restauration remplace les donnees pendant un telechargement
- **THEN** aucune publication du travail ancien ne contamine les donnees restaurees et la file est reconciliee apres restauration