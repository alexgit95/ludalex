# diaporama-veille Specification

## Purpose
Diaporama de veille embarqué affiché lorsqu'aucune partie n'est active.

## Requirements

### Requirement: Diaporama de veille
Lorsqu'aucune partie n'est en cours ou en pause, l'écran Affichage SHALL présenter un diaporama en boucle d'images de décor celtique ou de village médiéval, embarquées dans l'application.

#### Scenario: Aucune partie
- **WHEN** aucune partie n'est active
- **THEN** l'écran affiche le diaporama avec un fondu entre les images

#### Scenario: Lancement d'une partie
- **WHEN** une partie est lancée pendant le diaporama
- **THEN** l'écran passe à l'affichage de la partie

### Requirement: Images embarquées
Les images du diaporama SHALL être fournies avec l'application dans les ressources statiques, sans dépendance à un service externe, et être énumérées automatiquement au démarrage.

#### Scenario: Ajout d'une image
- **WHEN** une image est ajoutée dans le dossier du diaporama et l'application reconstruite
- **THEN** elle apparaît dans le diaporama sans autre modification

#### Scenario: Dossier vide
- **WHEN** aucune image n'est présente
- **THEN** l'écran affiche un fond neutre avec le nom de l'application

### Requirement: Diaporama sobre pour Raspberry Pi
Le diaporama SHALL charger une seule image à l'avance, utiliser des images de taille raisonnable (2 Mo maximum chacune) et des transitions CSS sans bibliothèque.

#### Scenario: Défilement continu
- **WHEN** le diaporama tourne pendant plusieurs heures
- **THEN** la mémoire du navigateur reste stable (une image affichée, une préchargée)
