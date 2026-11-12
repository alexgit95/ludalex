## ADDED Requirements

### Requirement: Image par upload
Le système SHALL accepter l'upload d'une image JPEG, PNG, GIF ou WebP de 2 Mo maximum, la stocker sur le volume d'images sous un nom généré par le serveur, et ne jamais la stocker en base.

#### Scenario: Upload valide
- **WHEN** l'utilisateur envoie un fichier PNG de 500 Ko
- **THEN** le fichier est écrit sur le volume sous un nom aléatoire et sa référence est conservée

#### Scenario: Fichier trop volumineux
- **WHEN** l'utilisateur envoie un fichier de plus de 2 Mo
- **THEN** le système refuse l'upload avec un message explicite et n'écrit rien sur le volume

#### Scenario: Type non autorisé
- **WHEN** l'utilisateur envoie un fichier dont le contenu réel n'est pas JPEG, PNG, GIF ou WebP (même avec une extension d'image)
- **THEN** le système refuse l'upload

#### Scenario: Nom de fichier malveillant
- **WHEN** le nom de fichier fourni contient des séquences de chemin (`../`)
- **THEN** le nom fourni est ignoré et le fichier est stocké sous un nom généré

### Requirement: Image par URL
Le système SHALL accepter une URL d'image http(s), ne conserver que l'URL, et ne jamais copier l'image sur le volume.

#### Scenario: URL enregistrée
- **WHEN** l'utilisateur saisit `https://exemple.org/jeu.jpg`
- **THEN** seule cette URL est enregistrée et utilisée par le navigateur pour afficher l'image

#### Scenario: Schéma refusé
- **WHEN** l'utilisateur saisit une URL `file:///etc/passwd` ou `ftp://…`
- **THEN** le système refuse la saisie

### Requirement: Source d'image exclusive
Une image SHALL être soit un fichier uploadé, soit une URL, jamais les deux.

#### Scenario: Remplacement par une URL
- **WHEN** l'utilisateur remplace une image uploadée par une URL
- **THEN** le fichier précédent est supprimé du volume et seule l'URL est conservée

### Requirement: Protection SSRF
Toute requête sortante vers une URL saisie par l'utilisateur SHALL être précédée d'une validation : schéma http(s), résolution DNS, refus des adresses de boucle locale, privées, de lien local et de métadonnées cloud, vérification de l'adresse effectivement connectée, délai et taille maximums, redirections revalidées.

#### Scenario: Adresse locale
- **WHEN** l'URL vise `http://127.0.0.1/…` ou `http://192.168.1.10/…`
- **THEN** le système refuse sans émettre de requête

#### Scenario: Nom de domaine résolvant vers une adresse privée
- **WHEN** le nom de domaine saisi résout vers une adresse privée
- **THEN** le système refuse sans émettre de requête

#### Scenario: Redirection vers une adresse interne
- **WHEN** une URL publique redirige vers `http://169.254.169.254/`
- **THEN** le système interrompt la requête

#### Scenario: Réponse trop volumineuse
- **WHEN** la réponse dépasse la taille maximale autorisée
- **THEN** le système interrompt le téléchargement

### Requirement: Restitution des images uploadées
Le système SHALL restituer une image uploadée en flux, avec le bon `Content-Type`, sans exposer d'autre fichier du volume.

#### Scenario: Image existante
- **WHEN** le navigateur demande `/images/<nom>` pour un fichier présent
- **THEN** le système répond 200 avec le contenu et un en-tête de cache

#### Scenario: Traversée de chemin
- **WHEN** le navigateur demande `/images/..%2F..%2Fetc%2Fpasswd`
- **THEN** le système répond par une erreur client (400 ou 404) sans exposer aucun fichier
