# couleur-fond Specification

## Purpose
Calcul unique, stockage et modification de la couleur de fond d'un jeu à partir de son image.

## Requirements

### Requirement: Calcul unique de la couleur de fond
À l'enregistrement d'un jeu, le système SHALL calculer une seule fois la couleur de fond à partir de l'image, la stocker, et ne plus la recalculer à l'affichage.

#### Scenario: Image uploadée
- **WHEN** l'utilisateur enregistre un jeu avec une image uploadée
- **THEN** le système calcule la couleur dominante et la stocke avec le jeu

#### Scenario: Image par URL
- **WHEN** l'utilisateur enregistre un jeu avec une URL d'image
- **THEN** le système télécharge l'image une seule fois (protection SSRF), calcule la couleur, la stocke et ne conserve que l'URL

#### Scenario: Affichage ultérieur
- **WHEN** une page affiche le jeu
- **THEN** la couleur stockée est utilisée sans nouveau calcul ni téléchargement

### Requirement: Calcul sobre en mémoire
Le calcul SHALL décoder l'image avec sous-échantillonnage, la réduire à environ 32×32 pixels et regrouper les couleurs par classes, sans jamais décoder l'image en pleine résolution.

#### Scenario: Grande image
- **WHEN** une image de 4000×3000 pixels est traitée
- **THEN** la mémoire utilisée reste inférieure à celle d'un décodage complet de l'image

### Requirement: Couleur dominante la plus fréquente
Le système SHALL choisir la classe de couleur la plus fréquente, en ignorant les pixels transparents ainsi que les pixels quasi blancs ou quasi noirs, et non la moyenne des pixels.

#### Scenario: Image majoritairement rouge
- **WHEN** une image est à 60 % rouge, 30 % blanc et 10 % bleu
- **THEN** la couleur retenue est un rouge

#### Scenario: Image transparente
- **WHEN** une image ne contient que des pixels transparents
- **THEN** la couleur neutre est retenue

### Requirement: Format WebP
Le calcul SHALL gérer les images WebP grâce à une bibliothèque tierce.

#### Scenario: Image WebP
- **WHEN** une image WebP est enregistrée
- **THEN** la couleur est calculée comme pour les autres formats

### Requirement: Repli neutre
Si le calcul échoue (format illisible, URL injoignable, URL refusée par la protection SSRF), le système SHALL enregistrer le jeu avec une couleur neutre et ne pas bloquer l'enregistrement.

#### Scenario: URL injoignable
- **WHEN** l'URL ne répond pas dans le délai imparti
- **THEN** le jeu est enregistré avec la couleur neutre

### Requirement: Modification manuelle
L'utilisateur SHALL pouvoir remplacer la couleur de fond par une couleur de son choix, et cette valeur SHALL être conservée lors d'une modification ultérieure du jeu qui ne change pas l'image.

#### Scenario: Couleur choisie
- **WHEN** l'utilisateur saisit la couleur `#336699` pour un jeu
- **THEN** cette couleur est stockée et utilisée à la place de la couleur calculée

#### Scenario: Modification sans changer l'image
- **WHEN** l'utilisateur modifie le nom du jeu sans toucher à l'image
- **THEN** la couleur existante est conservée

#### Scenario: Image changée
- **WHEN** l'utilisateur change l'image d'un jeu sans saisir de couleur
- **THEN** la couleur est recalculée pour la nouvelle image
