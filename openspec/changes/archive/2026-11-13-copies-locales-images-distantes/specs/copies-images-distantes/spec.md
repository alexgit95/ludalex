## ADDED Requirements

### Requirement: Collecte automatique des images referencees par URL
Le systeme SHALL collecter automatiquement toutes les sources d'image HTTP(S) referencees par l'application, jeux et joueurs compris, existantes et futures, y compris apres import ou restauration. Le traitement SHALL etre asynchrone, pagine et ne SHALL pas bloquer une sauvegarde de fiche sur le telechargement de sa copie.

#### Scenario: Rattrapage des images existantes
- **WHEN** l'application demarre avec des images par URL sans copie locale
- **THEN** les URL sont decouvertes par lots et leur copie est planifiee sans action admin et sans attendre un affichage

#### Scenario: Nouvelle reference et import
- **WHEN** une creation, une modification, un import ou une restauration valide une reference URL
- **THEN** une copie manquante est automatiquement planifiee apres validation des donnees

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