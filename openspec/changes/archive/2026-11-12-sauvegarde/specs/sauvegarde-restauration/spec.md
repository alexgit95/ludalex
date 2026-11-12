## ADDED Requirements

### Requirement: Export en un seul ZIP
Le système SHALL permettre de télécharger un fichier ZIP unique contenant un manifeste, toutes les données de l'application dans un format JSON indépendant du moteur de base, et toutes les images uploadées.

#### Scenario: Téléchargement
- **WHEN** l'utilisateur clique sur « Télécharger la sauvegarde »
- **THEN** le navigateur reçoit `ludalex-sauvegarde-<date>.zip` contenant `manifest.json`, `donnees/*.json` et `images/*`

#### Scenario: Images par URL
- **WHEN** un jeu utilise une image par URL
- **THEN** seule l'URL figure dans les données, sans fichier dans `images/`

#### Scenario: Tokens et secrets exclus
- **WHEN** la sauvegarde est générée
- **THEN** elle ne contient aucun token de fin de partie

### Requirement: Manifeste versionné
Le ZIP SHALL contenir un manifeste indiquant la version du format de sauvegarde, la version de l'application et la date, et le nombre d'enregistrements par type pour contrôle.

#### Scenario: Manifeste présent
- **WHEN** une sauvegarde est exportée
- **THEN** `manifest.json` porte `formatVersion`, `appVersion`, la date et les compteurs

### Requirement: Export sobre en mémoire
L'export SHALL être écrit en flux directement dans la réponse HTTP, en lisant les données par lots et sans charger l'ensemble en mémoire.

#### Scenario: Grande ludothèque
- **WHEN** la base contient 10 000 parties
- **THEN** l'export lit par lots et la mémoire reste bornée

### Requirement: Restauration depuis le ZIP exporté
Le système SHALL accepter en restauration le ZIP téléchargé, sans aucune modification préalable, y compris d'une base SQLite vers PostgreSQL et inversement.

#### Scenario: Aller-retour
- **WHEN** l'utilisateur télécharge une sauvegarde puis la ré-uploade telle quelle
- **THEN** la restauration réussit et les données et images sont identiques à celles exportées

#### Scenario: Changement de moteur
- **WHEN** une sauvegarde faite en `dev` (SQLite) est restaurée en `prod` (PostgreSQL)
- **THEN** les données sont retrouvées avec leurs relations (parties, participations, équipes, prêts)

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
