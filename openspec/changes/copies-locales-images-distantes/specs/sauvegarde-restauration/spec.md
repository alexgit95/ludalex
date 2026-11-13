## MODIFIED Requirements

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

## ADDED Requirements

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