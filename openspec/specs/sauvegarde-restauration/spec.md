# sauvegarde-restauration Specification

## Purpose
Export ZIP unique des données et images, et restauration complète sûre, atomique et indépendante du moteur de base.

## Requirements

### Requirement: Export en un seul ZIP
Le système SHALL permettre de télécharger un fichier ZIP unique contenant un manifeste, toutes les données de l'application dans un format JSON indépendant du moteur de base, toutes les images uploadées et toutes les copies locales disponibles d'images par URL encore referencees, avec leurs associations. Les copies partagees SHALL etre exportees une seule fois et l'export SHALL rester coherent avec le traitement d'arriere-plan.

#### Scenario: Téléchargement
- **WHEN** l'utilisateur clique sur « Télécharger la sauvegarde »
- **THEN** le navigateur reçoit `ludalex-sauvegarde-<date>.zip` contenant `manifest.json`, `donnees/*.json` et `images/*`

#### Scenario: Images par URL
- **WHEN** un jeu ou un joueur utilise une image par URL avec une copie disponible
- **THEN** l'URL, l'association avec sa copie et le fichier de copie figurent dans le ZIP

#### Scenario: Source sans copie et copies partagees
- **WHEN** des sources URL sont sans copie ou partagent la meme copie
- **THEN** les URL sans copie sont exportees sans fichier et chaque fichier partage disponible est inclus une seule fois

#### Scenario: Export concurrent a la collecte
- **WHEN** un travail publie ou nettoie une copie pendant l'export
- **THEN** chaque association exportee possede son fichier dans le ZIP et aucun fichier partiel n'est exporte

#### Scenario: Tokens et secrets exclus
- **WHEN** la sauvegarde est générée
- **THEN** elle ne contient aucun token de fin de partie, aucun secret de configuration et aucun historique d'erreur technique de la collecte

### Requirement: Manifeste versionné
Le ZIP SHALL contenir un manifeste indiquant la version du format de sauvegarde, la version de l'application et la date, et le nombre d'enregistrements par type pour contrôle.

#### Scenario: Manifeste présent
- **WHEN** une sauvegarde est exportée
- **THEN** `manifest.json` porte `formatVersion`, `appVersion`, la date et les compteurs

### Requirement: Sauvegarde des variantes de jeux et de parties
Le système SHALL exporter en format ZIP v3 l'indicateur de variantes de chaque jeu et le libellé associé à chaque partie. La restauration SHALL accepter les formats v1, v2 et v3; les données absentes d'un ZIP v1 SHALL être restaurées avec l'indicateur désactivé et sans variante.

#### Scenario: Aller-retour d'une sauvegarde v3
- **WHEN** l'utilisateur sauvegarde puis restaure des jeux et parties avec variantes au format v3
- **THEN** l'indicateur des jeux et les libellés des parties sont conservés sans modification

#### Scenario: Aller-retour d'une sauvegarde v2
- **WHEN** l'utilisateur sauvegarde puis restaure des jeux et parties avec variantes
- **THEN** l'indicateur des jeux et les libellés des parties sont conservés sans modification

#### Scenario: Restaurer une sauvegarde v1
- **WHEN** l'utilisateur restaure une sauvegarde au format v1 sans champs de variantes
- **THEN** les jeux sont restaurés sans indicateur de variantes et les parties sans libellé

#### Scenario: Restauration entre SQLite et PostgreSQL
- **WHEN** une sauvegarde v2 est restaurée sur l'autre moteur de base pris en charge
- **THEN** les indicateurs et libellés sont conservés avec les autres données de jeu et de partie

### Requirement: Export sobre en mémoire
L'export SHALL être écrit en flux directement dans la réponse HTTP, en lisant les données par lots et sans charger l'ensemble en mémoire.

#### Scenario: Grande ludothèque
- **WHEN** la base contient 10 000 parties
- **THEN** l'export lit par lots et la mémoire reste bornée

### Requirement: Restauration depuis le ZIP exporté
Le système SHALL accepter en restauration le ZIP téléchargé, sans aucune modification préalable, y compris d'une base SQLite vers PostgreSQL et inversement. Il SHALL restaurer les copies presentes avec leurs associations et continuer a accepter les anciens ZIP ne contenant que les sources URL ; les copies manquantes SHALL etre planifiees automatiquement apres restauration validee.

#### Scenario: Aller-retour
- **WHEN** l'utilisateur télécharge une sauvegarde puis la ré-uploade telle quelle
- **THEN** la restauration réussit et les données, images uploadees et copies disponibles sont identiques à celles exportées

#### Scenario: Changement de moteur
- **WHEN** une sauvegarde faite en `dev` (SQLite) est restaurée en `prod` (PostgreSQL)
- **THEN** les données sont retrouvées avec leurs relations (parties, participations, équipes, prêts) et les associations URL/copie

#### Scenario: Ancien format sans copies
- **WHEN** une sauvegarde de l'ancien format contenant des sources URL sans copie est restauree
- **THEN** les sources sont conservees et leurs copies sont planifiees automatiquement sans exiger de modification du ZIP

### Requirement: Remplacement complet protégé
La restauration SHALL remplacer entièrement les données et les images existantes, uniquement après une confirmation explicite de l'utilisateur et avec une protection CSRF valide.

#### Scenario: Confirmation requise
- **WHEN** l'utilisateur envoie un ZIP sans avoir saisi la confirmation demandée
- **THEN** le système refuse et ne modifie rien

#### Scenario: CSRF manquant
- **WHEN** la requête de restauration n'a pas de jeton CSRF valide
- **THEN** le système répond 403 et ne modifie rien

#### Scenario: Remplacement
- **WHEN** l'utilisateur confirme la restauration d'un ZIP valide
- **THEN** toutes les données et images existantes sont remplacées par celles du ZIP

#### Scenario: Partie active et tokens
- **WHEN** une restauration réussit
- **THEN** tous les tokens de fin de partie existants sont supprimés

### Requirement: Validation complète avant suppression
Le système SHALL valider entièrement le ZIP (structure, manifeste, limites, cohérence des données) avant de supprimer quoi que ce soit.

#### Scenario: ZIP invalide
- **WHEN** le ZIP est corrompu ou incomplet
- **THEN** le système refuse avec un message explicite et les données existantes restent intactes

#### Scenario: Version de format inconnue
- **WHEN** le manifeste porte une version de format non prise en charge
- **THEN** le système refuse et indique les versions acceptées

#### Scenario: Références incohérentes
- **WHEN** une participation référence un joueur absent des données
- **THEN** le système refuse la restauration

### Requirement: Contrôle du contenu du ZIP
Le système SHALL refuser tout ZIP contenant une entrée au chemin dangereux (zip slip), hors des dossiers autorisés, un nombre d'entrées ou une taille décompressée excessifs, un ratio de compression anormal, ou une image de type non autorisé ou de plus de 2 Mo.

#### Scenario: Zip slip
- **WHEN** une entrée a pour nom `../../etc/cron.d/x`
- **THEN** le système refuse le ZIP sans rien extraire

#### Scenario: Bombe de décompression
- **WHEN** le ZIP se décompresse au-delà de la taille maximale autorisée
- **THEN** le système refuse le ZIP

#### Scenario: Image non conforme
- **WHEN** une entrée `images/x.png` n'est pas une image PNG, JPEG, GIF ou WebP réelle
- **THEN** le système refuse le ZIP

#### Scenario: Entrée inattendue
- **WHEN** le ZIP contient un fichier hors de `manifest.json`, `donnees/` et `images/`
- **THEN** le système refuse le ZIP

### Requirement: Restauration atomique
La restauration SHALL être atomique : en cas d'échec pendant l'import, les données et les images précédentes restent intactes.

#### Scenario: Échec en cours d'import
- **WHEN** une erreur survient pendant l'insertion des données
- **THEN** la transaction est annulée, les images extraites sont supprimées et l'état précédent est conservé

### Requirement: Restauration sobre en mémoire
Le ZIP reçu SHALL être écrit en fichier temporaire puis lu en flux, sans être chargé en mémoire ; le fichier temporaire SHALL être supprimé en fin de traitement.

#### Scenario: Gros ZIP
- **WHEN** un ZIP de 200 Mo est restauré
- **THEN** la mémoire utilisée reste bornée et le fichier temporaire est supprimé à la fin

### Requirement: Accès protégé
Les opérations d'export et de restauration SHALL suivre les mêmes exigences d'accès que le reste de l'application (JWT Cloudflare en `prod`) et ne pas être accessibles via `/fin/**`.

#### Scenario: Sans JWT en prod
- **WHEN** une requête vers l'export arrive sans JWT en `prod`
- **THEN** le système répond 401

### Requirement: Validation des associations de copies
Le systeme SHALL valider les fichiers de copies et leurs associations avant remplacement, avec les memes controles de chemin, taille, format et limites globales du ZIP que pour les uploads. Une association vers un fichier manquant ou une source inexistante SHALL etre refusee et la restauration SHALL conserver son atomicite, y compris vis-a-vis du worker de copies.

#### Scenario: Association invalide
- **WHEN** un ZIP declare une copie pour une source inexistante ou sans le fichier correspondant
- **THEN** la restauration est refusee sans modifier les donnees et fichiers existants

#### Scenario: Copie malveillante
- **WHEN** une copie dans le ZIP viole les controles de chemin, format reel ou taille
- **THEN** la restauration est refusee avant tout remplacement

#### Scenario: Echec de restauration avec collecte active
- **WHEN** une restauration echoue alors que la collecte est active
- **THEN** les donnees et images anterieures sont conservees et la collecte reprend sur cet etat coherent
