## MODIFIED Requirements

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

## ADDED Requirements

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