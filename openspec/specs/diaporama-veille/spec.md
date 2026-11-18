# diaporama-veille Specification

## Purpose
Diaporama de veille embarqué affiché lorsqu'aucune partie n'est active.

## Requirements

### Requirement: Diaporama de veille
Lorsqu'aucune partie n'est en cours ou en pause, l'écran Affichage SHALL présenter un diaporama en boucle d'images embarquées, sélectionnées selon l'orientation de la fenêtre, avec un fondu entre les images. S'il existe des parties terminées, le diaporama SHALL être interrompu par la galerie des cinq dernières parties après un nombre aléatoire de deux à cinq affichages d'image. Chaque image et chaque passage de la galerie SHALL durer dix secondes.

#### Scenario: Aucune partie
- **WHEN** aucune partie n'est active
- **THEN** l'écran affiche le diaporama avec un fondu entre les images adaptées à l'orientation

#### Scenario: Lancement d'une partie
- **WHEN** une partie est lancée pendant le diaporama
- **THEN** l'écran passe à l'affichage de la partie

#### Scenario: Récapitulatif aléatoire
- **WHEN** des parties terminées existent pendant la veille
- **THEN** après deux à cinq affichages d'image, choisis aléatoirement, la galerie globale apparaît dix secondes avant le retour aux images et un nouveau tirage

#### Scenario: Aucun historique
- **WHEN** aucune partie terminée n'existe
- **THEN** le diaporama continue sans galerie vide

#### Scenario: Image unique ou absente
- **WHEN** une seule image ou aucune image adaptée à l'orientation existe
- **THEN** chaque période de dix secondes affichant cette image ou le fond LUDALEX compte comme un affichage d'image pour la rotation

#### Scenario: Historique modifié en veille
- **WHEN** une partie terminée ou les données de son résultat sont ajoutées, modifiées ou supprimées pendant la veille
- **THEN** la galerie et sa disponibilité sont actualisées sans rechargement manuel ni chargement de l'historique à chaque interrogation

### Requirement: Images embarquées
Les images du diaporama SHALL être fournies avec l'application dans les ressources statiques, sans dépendance à un service externe, et être énumérées automatiquement au démarrage.

#### Scenario: Ajout d'une image
- **WHEN** une image est ajoutée dans le dossier du diaporama et l'application reconstruite
- **THEN** elle apparaît dans le diaporama lorsque son nom correspond à l'orientation, sans autre modification

#### Scenario: Dossier vide
- **WHEN** aucune image n'est présente
- **THEN** l'écran affiche un fond neutre avec le nom de l'application

### Requirement: Diaporama sobre pour Raspberry Pi
Le diaporama SHALL charger une seule image à l'avance, utiliser des images de taille raisonnable (2 Mo maximum chacune) et des transitions CSS sans bibliothèque.

#### Scenario: Défilement continu
- **WHEN** le diaporama tourne pendant plusieurs heures
- **THEN** la mémoire du navigateur reste stable (une image affichée, une préchargée)

### Requirement: Images selon l'orientation
Le diaporama SHALL utiliser uniquement les images dont le nom de fichier commence exactement par `portrait_` lorsque la fenêtre est en portrait ou carrée, et uniquement les autres images en paysage. Cette sélection SHALL s'appliquer en veille et pendant une partie, et être actualisée lorsque l'orientation change sans rechargement de page.

#### Scenario: Portrait
- **WHEN** la fenêtre est plus haute que large et contient des images `portrait_`
- **THEN** seules ces images sont affichées

#### Scenario: Paysage
- **WHEN** la fenêtre est plus large que haute
- **THEN** seules les images dont le nom ne commence pas par `portrait_` sont affichées

#### Scenario: Changement d'orientation
- **WHEN** la fenêtre change d'orientation pendant le diaporama ou la rotation d'une partie
- **THEN** les images affichées et préchargées correspondent à la nouvelle orientation sans rechargement

#### Scenario: Aucune image adaptée
- **WHEN** aucune image ne correspond à l'orientation actuelle
- **THEN** le diaporama affiche LUDALEX sur un fond neutre sans utiliser les images de l'autre orientation

#### Scenario: Ordre des images
- **WHEN** plusieurs images correspondent à l'orientation
- **THEN** elles défilent en boucle par ordre de nom, à partir d'une image initiale choisie aléatoirement

### Requirement: QR de lancement en veille
Pendant la veille, le système SHALL afficher sur le premier affichage d'image puis un affichage d'image sur deux un QR code menant à la page protégée de lancement de partie. Le QR SHALL être masqué sur les autres images et sur la galerie. Il SHALL aussi alterner sur le fond LUDALEX si aucune image adaptée n'est disponible. Le changement d'orientation SHALL conserver l'état d'alternance du QR.

#### Scenario: Alternance du QR
- **WHEN** le diaporama de veille affiche successivement quatre images, avec ou sans galerie entre elles
- **THEN** le QR est visible sur la première et la troisième et masqué sur la deuxième et la quatrième

#### Scenario: Fond neutre
- **WHEN** aucune image n'est adaptée à l'orientation de l'écran
- **THEN** le QR alterne toutes les dix secondes sur le fond LUDALEX

#### Scenario: Rotation de l'écran
- **WHEN** l'orientation change pendant une image de veille
- **THEN** le choix de l'image s'adapte sans compter un nouvel affichage ni changer la visibilité du QR
