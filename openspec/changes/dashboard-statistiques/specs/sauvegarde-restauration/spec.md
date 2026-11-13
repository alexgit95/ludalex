## ADDED Requirements

### Requirement: Sauvegarde des réglages
Le système SHALL exporter en format ZIP v3 l'ensemble des réglages persistés (génériquement, tout réglage présent en base, y compris ceux ajoutés ultérieurement), et la restauration SHALL les rétablir. La restauration SHALL accepter les formats v1, v2 et v3 ; pour un ZIP v1 ou v2, les réglages existants SHALL être supprimés et les valeurs par défaut s'appliquent. Une clé de réglage inconnue ou une valeur invalide dans un ZIP SHALL faire échouer la validation avant toute suppression.

#### Scenario: Aller-retour d'une sauvegarde v3
- **WHEN** l'utilisateur modifie des seuils, sauvegarde puis restaure
- **THEN** les réglages retrouvent exactement leurs valeurs

#### Scenario: Restaurer une sauvegarde v1 ou v2
- **WHEN** l'utilisateur restaure un ZIP sans réglages
- **THEN** les réglages précédents sont supprimés et les valeurs par défaut s'appliquent

#### Scenario: Réglage invalide dans l'archive
- **WHEN** le ZIP contient une clé inconnue ou une valeur hors bornes
- **THEN** la restauration est refusée sans modifier les données existantes

#### Scenario: Exhaustivité
- **WHEN** un nouveau réglage est ajouté au catalogue
- **THEN** il est exporté et restauré sans modification du format

#### Scenario: Version antérieure
- **WHEN** un ZIP v3 est restauré par une version de l'application antérieure à 2.0.0
- **THEN** la restauration est refusée, ce qui est documenté comme breaking change

### Requirement: Compteurs du manifeste pour les réglages
Le manifeste v3 SHALL indiquer le nombre de réglages exportés et la validation SHALL contrôler ce compteur.

#### Scenario: Compteur incohérent
- **WHEN** le nombre de réglages du fichier diffère du manifeste
- **THEN** la restauration est refusée
