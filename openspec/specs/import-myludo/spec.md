# import-myludo Specification

## Purpose
Import des jeux, joueurs et parties depuis les exports MyLudo (collection et parties), avec analyse, ajout du delta seul, progression et rapport.

## Requirements

### Requirement: Import depuis les deux exports MyLudo
Le système SHALL permettre, depuis l'administration, d'uploader l'export MyLudo de la collection et/ou celui des parties (fichiers JSON), de les analyser sans rien enregistrer, puis d'importer jeux, joueurs et parties après confirmation.

#### Scenario: Analyse avant import
- **WHEN** l'utilisateur uploade un ou deux exports valides
- **THEN** le système affiche le nombre de jeux, de joueurs et de parties qui seront créés, déjà présents ou ignorés, sans rien enregistrer

#### Scenario: Import confirmé
- **WHEN** l'utilisateur confirme l'import après l'analyse
- **THEN** les jeux, joueurs, parties, équipes et participations manquants sont créés

#### Scenario: Aucun fichier reconnu
- **WHEN** aucun fichier fourni n'est un export de collection ou de parties MyLudo reconnu
- **THEN** le système refuse avec un message explicite et ne modifie rien

### Requirement: Delta seulement, sans écrasement
L'import SHALL n'ajouter que les parties absentes et ne jamais modifier ni supprimer de partie existante. Une partie du fichier est déjà présente s'il existe, pour le même jeu et le même jour, une partie en base partageant au moins un joueur avec elle ; chaque partie en base ne peut correspondre qu'à une seule partie du fichier.

#### Scenario: Partie déjà saisie
- **WHEN** une partie du fichier a le même jeu, le même jour et au moins un joueur en commun avec une partie existante
- **THEN** elle n'est pas ajoutée et la partie existante reste inchangée (lieu, scores, gagnants)

#### Scenario: Manches successives le même jour
- **WHEN** le fichier contient cinq parties identiques du même jeu le même jour et la base n'en contient qu'une
- **THEN** quatre sont ajoutées

#### Scenario: Second import du même fichier
- **WHEN** l'utilisateur importe deux fois le même export
- **THEN** aucune partie, jeu ni joueur n'est dupliqué et le rapport indique tout comme déjà présent

#### Scenario: Export plus récent
- **WHEN** l'utilisateur importe un export contenant en plus des parties récentes
- **THEN** seules les parties récentes sont ajoutées

### Requirement: Jeux de la collection
Le système SHALL créer les jeux de la collection (nom, prix, date d'acquisition) et rapprocher chaque jeu d'un jeu existant par nom normalisé (casse, accents, espaces), sans jamais modifier un jeu existant hormis le complément de son prix et de sa date d'achat lorsqu'ils sont vides.

#### Scenario: Jeu déjà présent
- **WHEN** « Catan » existe déjà avec un prix
- **THEN** il n'est pas dupliqué et son prix n'est pas modifié

#### Scenario: Champ vide complété
- **WHEN** un jeu existant n'a pas de date d'achat et la collection en fournit une
- **THEN** la date d'achat est renseignée

#### Scenario: Prix inconnu
- **WHEN** la collection indique un prix indicatif de 0
- **THEN** le jeu est créé sans prix

#### Scenario: Jeu absent de la collection
- **WHEN** une partie référence un jeu qui n'est pas dans la collection importée ni en base
- **THEN** le jeu est créé à partir de la partie, sans prix ni date d'achat

#### Scenario: Jeu coopératif
- **WHEN** au moins une partie d'un jeu est marquée coopérative
- **THEN** le jeu est créé comme coopératif

### Requirement: Correspondance des joueurs
Le système SHALL faire correspondre chaque nom de joueur de l'export à un joueur existant (nom normalisé) ou en créer un, et permettre à l'utilisateur de modifier cette correspondance avant l'import. Un même nom répété dans une partie SHALL donner des fiches distinctes (« Anonyme », « Anonyme 2 »).

#### Scenario: Joueur existant
- **WHEN** un joueur de l'export porte le même nom qu'un joueur existant
- **THEN** il leur est associé par défaut

#### Scenario: Nouveau joueur
- **WHEN** un joueur de l'export n'existe pas dans l'application
- **THEN** il est proposé à la création et créé à l'import

#### Scenario: Changement de correspondance
- **WHEN** l'utilisateur associe un nom de l'export à un autre joueur existant
- **THEN** les participations de ce nom sont rattachées à ce joueur

#### Scenario: Doublon de nom dans une partie
- **WHEN** une partie contient deux joueurs « Anonyme »
- **THEN** deux fiches distinctes sont utilisées et réutilisées pour les autres parties concernées

### Requirement: Conversion des parties
Le système SHALL créer chaque partie ajoutée comme terminée avec sa date, son lieu, ses participations (scores, gagnants), ses équipes et, pour un jeu coopératif, son résultat victoire ou défaite, en respectant les règles de validation de l'application.

#### Scenario: Partie avec scores
- **WHEN** une partie a des scores non tous nuls
- **THEN** les scores sont enregistrés par joueur

#### Scenario: Scores tous nuls
- **WHEN** tous les scores d'une partie valent 0
- **THEN** aucun score n'est enregistré

#### Scenario: Partie coopérative perdue
- **WHEN** une partie d'un jeu coopératif a pour résultat une défaite
- **THEN** la partie est enregistrée avec le résultat défaite et sans vainqueur

#### Scenario: Équipes
- **WHEN** les joueurs d'une partie portent des équipes
- **THEN** les équipes sont créées pour cette partie et rattachées aux joueurs

#### Scenario: Lieu vide ou trop long
- **WHEN** le lieu est vide ou dépasse 100 caractères
- **THEN** il reste vide, ou est tronqué avec un avertissement

#### Scenario: Partie ignorée
- **WHEN** une partie est exclue, datée dans le futur, sans joueur, ou non coopérative sans gagnant
- **THEN** elle n'est pas importée et le rapport en indique la raison

### Requirement: Progression de l'import
L'interface SHALL afficher, pendant l'analyse et l'import, une barre de progression indiquant la phase, le nombre d'éléments traités sur le total et l'élément en cours, mise à jour automatiquement.

#### Scenario: Import en cours
- **WHEN** un import de 140 parties est en cours
- **THEN** la page affiche une barre de progression et un compteur qui avancent sans recharger la page

#### Scenario: Une seule opération à la fois
- **WHEN** une analyse ou un import est déjà en cours
- **THEN** une seconde demande est refusée avec un message explicite

### Requirement: Rapport de réussite ou d'erreur
Le système SHALL afficher à la fin un message de réussite avec les compteurs (jeux créés, complétés, inchangés ; joueurs créés, associés ; parties ajoutées, déjà présentes, ignorées par raison ; avertissements) ou, en cas d'échec, un message d'erreur permettant le diagnostic : fichier, numéro d'entrée, jeu et date concernés, champ et cause, ligne et colonne pour un JSON mal formé.

#### Scenario: Import réussi
- **WHEN** l'import se termine sans erreur
- **THEN** le message indique que l'import a réussi avec les compteurs et la liste des parties ignorées et de leurs raisons

#### Scenario: JSON mal formé
- **WHEN** un fichier n'est pas un JSON valide
- **THEN** le message indique le fichier, la ligne et la colonne de l'erreur et rien n'est enregistré

#### Scenario: Entrée invalide
- **WHEN** une entrée contient une valeur inutilisable (date illisible, nom absent)
- **THEN** le message indique le fichier, le numéro de l'entrée, le jeu et la date, le champ et la cause

#### Scenario: Aucun détail interne
- **WHEN** une erreur inattendue survient
- **THEN** le message reste générique, la cause détaillée est journalisée côté serveur et rien n'est enregistré

### Requirement: Import atomique
L'import SHALL être atomique : en cas d'erreur pendant l'écriture, aucune donnée n'est créée ou modifiée.

#### Scenario: Erreur au milieu
- **WHEN** une erreur survient après l'ajout de plusieurs parties
- **THEN** la transaction est annulée et la base reste dans son état précédent

### Requirement: Sécurité et limites du fichier
Le système SHALL réserver l'import à l'administration (JWT Cloudflare en `prod`) avec protection CSRF, refuser les fichiers dépassant la taille autorisée, lire les fichiers en flux, ne faire aucun appel sortant et ne conserver les données des fichiers en mémoire que le temps de la session.

#### Scenario: Sans JWT en prod
- **WHEN** une requête vers l'import arrive sans JWT en `prod`
- **THEN** le système répond 401

#### Scenario: CSRF manquant
- **WHEN** l'envoi ou la confirmation n'a pas de jeton CSRF valide
- **THEN** le système répond 403 et ne modifie rien

#### Scenario: Fichier trop volumineux
- **WHEN** un fichier dépasse la taille maximale autorisée
- **THEN** le système refuse l'upload avec un message explicite

#### Scenario: Données de la session
- **WHEN** l'import se termine, échoue, est annulé ou expire
- **THEN** les données lues des fichiers sont oubliées

### Requirement: Données non importées
L'import SHALL ne reprendre ni la durée, ni les commentaires, ni les notes, ni les images, ni les métadonnées des jeux (éditeurs, thèmes, mécanismes), faute de champ correspondant, sans modifier le schéma de la base.

#### Scenario: Champs ignorés
- **WHEN** un export contient durée, commentaire et notes
- **THEN** ils sont ignorés sans erreur
