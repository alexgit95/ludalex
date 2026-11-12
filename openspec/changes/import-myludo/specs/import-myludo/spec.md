## ADDED Requirements

### Requirement: Import d'un export MyLudo
Le système SHALL permettre à l'utilisateur d'uploader un export MyLudo et d'en importer les jeux et les parties après un aperçu.

#### Scenario: Aperçu avant import
- **WHEN** l'utilisateur uploade un export MyLudo valide
- **THEN** le système affiche le nombre de jeux, de parties et de joueurs détectés, sans rien enregistrer

#### Scenario: Import confirmé
- **WHEN** l'utilisateur confirme l'import après l'aperçu
- **THEN** les jeux, joueurs, parties et participations sont créés

#### Scenario: Fichier invalide
- **WHEN** le fichier n'est pas un export MyLudo reconnu
- **THEN** le système refuse avec un message explicite et ne modifie rien

### Requirement: Correspondance des joueurs
Le système SHALL proposer de faire correspondre chaque joueur de l'export à un joueur existant ou d'en créer un nouveau, avant l'import.

#### Scenario: Joueur existant
- **WHEN** un joueur de l'export porte le même nom qu'un joueur existant
- **THEN** il est proposé par défaut de les associer

#### Scenario: Nouveau joueur
- **WHEN** un joueur de l'export n'existe pas dans l'application
- **THEN** il est proposé de le créer

### Requirement: Rapprochement des jeux
Le système SHALL rapprocher chaque jeu de l'export d'un jeu existant de même nom (insensible à la casse) pour éviter les doublons.

#### Scenario: Jeu déjà présent
- **WHEN** « Catan » existe déjà
- **THEN** les parties de l'export sont rattachées au jeu existant sans créer de doublon

### Requirement: Import idempotent
Le système SHALL ignorer les parties déjà importées (même jeu, même date, mêmes participants) lorsqu'un même export est importé plusieurs fois.

#### Scenario: Deuxième import du même fichier
- **WHEN** l'utilisateur importe deux fois le même export
- **THEN** aucune partie n'est dupliquée et le rapport indique les parties ignorées

### Requirement: Import atomique avec rapport
L'import SHALL être atomique et produire un rapport (créés, ignorés, erreurs).

#### Scenario: Erreur au milieu
- **WHEN** une erreur survient pendant l'import
- **THEN** rien n'est enregistré et le rapport indique la cause

### Requirement: Contrôle du fichier importé
Le système SHALL limiter la taille du fichier importé, traiter le fichier en flux, et se prémunir des entités externes XML et des entrées d'archive dangereuses selon le format reçu.

#### Scenario: Fichier trop volumineux
- **WHEN** le fichier dépasse la taille maximale autorisée
- **THEN** le système refuse l'upload

#### Scenario: Entité externe XML
- **WHEN** le fichier contient une déclaration d'entité externe
- **THEN** elle n'est pas résolue et le fichier est refusé
