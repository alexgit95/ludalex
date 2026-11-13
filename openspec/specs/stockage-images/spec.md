# stockage-images Specification

## Purpose
Stockage des images uploadées sur volume, images par URL, restitution et protection SSRF.

## Requirements

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
Le système SHALL accepter une URL d'image http(s), conserver cette URL comme source prioritaire et planifier automatiquement une copie locale de secours sur le volume persistant. Un echec de copie SHALL laisser l'URL et la fiche utilisables.

#### Scenario: URL enregistrée
- **WHEN** l'utilisateur saisit `https://exemple.org/jeu.jpg`
- **THEN** cette URL est enregistree et utilisee en priorite par le navigateur, et sa copie est planifiee en arriere-plan

#### Scenario: Schéma refusé
- **WHEN** l'utilisateur saisit une URL `file:///etc/passwd` ou `ftp://exemple.org/image.jpg`
- **THEN** le système refuse la saisie

### Requirement: Source d'image exclusive
Une image SHALL conserver une seule source choisie : fichier uploade ou URL. Une source URL SHALL pouvoir posseder une copie de secours distincte sans etre convertie en upload ni perdre son URL. Les anciens fichiers et copies non references SHALL etre nettoyes apres validation du remplacement.

#### Scenario: Remplacement par une URL
- **WHEN** l'utilisateur remplace une image uploadée par une URL
- **THEN** le fichier precedent est supprime apres validation de la transaction, la source devient cette URL et une copie de secours est planifiee

#### Scenario: Remplacement d'une URL par un upload
- **WHEN** l'utilisateur remplace une source URL par un fichier uploade
- **THEN** seul l'upload est utilise pour la fiche et l'ancienne copie est nettoyee si aucune autre fiche ne reference cette URL

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

### Requirement: Repli sur erreur de chargement d'une image
Dans toute l'IHM, le systeme SHALL essayer l'URL distante d'une source URL en priorite, puis sa copie locale disponible sur toute erreur de chargement de cette ressource image, puis l'image par defaut si aucune image ne peut etre chargee. Pour les images decoratives retirees sur erreur, le retrait SHALL survenir seulement apres echec des sources disponibles. La chaine SHALL etre finie, sans boucle, et ne SHALL pas s'appliquer aux requetes non images.

#### Scenario: Image distante disponible
- **WHEN** l'image distante se charge correctement et une copie existe
- **THEN** l'image distante est affichee sans charger la copie

#### Scenario: Image distante en erreur
- **WHEN** une image distante echoue avec un 404, un refus d'acces, une erreur reseau ou un contenu indecodable
- **THEN** le navigateur essaie sa copie locale disponible, sans exiger d'identifier le code HTTP

#### Scenario: Copie indisponible ou illisible
- **WHEN** l'image distante echoue et la copie est absente ou echoue egalement
- **THEN** l'image par defaut est utilisee, ou l'image decorative est retiree, sans revenir a la source distante ni boucler si le dernier secours echoue

#### Scenario: Vues multiples et fragments dynamiques
- **WHEN** une image par URL apparait dans un catalogue, une fiche, un apercu, l'ecran d'affichage ou un diaporama, y compris apres remplacement HTMX
- **THEN** la meme priorite distante puis locale s'applique

#### Scenario: Erreur sur une autre ressource
- **WHEN** une page, un script ou une requete metier echoue
- **THEN** aucune copie d'image n'est utilisee pour cette ressource

### Requirement: Restitution protegee des copies locales
Le systeme SHALL restituer les copies locales validees en flux avec le bon Content-Type et les protections de chemin et d'acces appliquees aux images uploadees. Les copies SHALL persister lors d'une recreation du conteneur utilisant le meme volume et ne SHALL ouvrir aucune exception d'acces public.

#### Scenario: Copie restituee
- **WHEN** une requete autorisee demande une copie presente
- **THEN** le serveur renvoie son contenu avec le bon type et un en-tete de cache sans exposer de fichier hors du stockage autorise

#### Scenario: Recreation du conteneur
- **WHEN** le conteneur est recree avec les memes donnees et le meme volume d'images
- **THEN** les copies disponibles restent utilisables sans nouveau telechargement

#### Scenario: Requete sans autorisation ou chemin dangereux
- **WHEN** une demande de copie manque de JWT valide en prod ou tente une traversee de chemin
- **THEN** le serveur refuse la demande sans exposer le fichier
