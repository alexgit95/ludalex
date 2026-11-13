## MODIFIED Requirements

### Requirement: Affichage de la partie en cours
Le système SHALL afficher sur `/affichage`, lorsqu'une partie est en cours, le titre du jeu, l'avatar et le nom de chaque joueur en grand, et un QR code, sur un fond reprenant l'image du jeu (floutée, sous un voile léger) et sa couleur de fond. Les joueurs SHALL toujours être entièrement visibles, sans rognage, quelle que soit la taille de l'image du jeu, et leurs avatars SHALL être dimensionnés selon la hauteur de l'écran. Un cartouche « VS » SHALL matérialiser l'affrontement selon la composition de la partie.

#### Scenario: Partie en cours
- **WHEN** une partie « en cours » de « Catan » avec trois joueurs est active
- **THEN** l'écran montre le titre de Catan, les trois avatars entièrement visibles, le QR code, sur un fond issu de l'image de Catan et de sa couleur

#### Scenario: Image absente
- **WHEN** le jeu n'a pas d'image
- **THEN** le fond est uniquement de la couleur du jeu et le titre du jeu est affiché en grand

#### Scenario: Échec du chargement de l'image
- **WHEN** l'image du jeu ne peut pas être chargée par le navigateur
- **THEN** le fond reste de la couleur du jeu, sans erreur visible

#### Scenario: Avatar absent
- **WHEN** un joueur n'a pas d'avatar
- **THEN** l'écran affiche l'avatar par défaut avec son nom

#### Scenario: Avatars non rognés
- **WHEN** une partie est en cours sur une tablette en paysage, avec une image de jeu de grande hauteur
- **THEN** les avatars et noms des joueurs sont entièrement visibles dans la hauteur de l'écran

#### Scenario: Duel
- **WHEN** une partie compétitive sans équipe oppose deux joueurs
- **THEN** les deux joueurs sont face à face avec le cartouche « VS » entre eux

#### Scenario: Plus de deux joueurs sans équipe
- **WHEN** une partie compétitive sans équipe réunit trois joueurs ou plus
- **THEN** les joueurs sont en rangée avec un cartouche « VS » entre chaque paire de joueurs adjacents

#### Scenario: Parties par équipes
- **WHEN** une partie compétitive est jouée par deux équipes
- **THEN** chaque équipe est affichée en colonne avec son nom et les avatars de ses membres, et un cartouche « VS » sépare les équipes

#### Scenario: Plus de deux équipes
- **WHEN** une partie est jouée par trois équipes
- **THEN** un cartouche « VS » sépare chaque paire d'équipes adjacentes

#### Scenario: Joueur sans équipe dans une partie par équipes
- **WHEN** un joueur de la partie n'est affecté à aucune équipe alors que d'autres le sont
- **THEN** il est affiché dans un groupe distinct sans nom d'équipe, et un « VS » le sépare des équipes

#### Scenario: Jeu coopératif
- **WHEN** la partie en cours est d'un jeu coopératif
- **THEN** aucun « VS » n'est affiché et un libellé « Ensemble » rassemble les joueurs

#### Scenario: Joueur seul
- **WHEN** la partie en cours ne compte qu'un seul joueur
- **THEN** aucun « VS » n'est affiché

#### Scenario: Pulsation discrète
- **WHEN** le cartouche « VS » est affiché
- **THEN** il pulse lentement et faiblement (variation d'échelle d'au plus 4 %, cycle de 2,5 secondes ou plus), en n'animant que `transform` et `opacity`

#### Scenario: Mouvement réduit
- **WHEN** le navigateur demande un mouvement réduit (`prefers-reduced-motion: reduce`)
- **THEN** le cartouche « VS » n'est pas animé

### Requirement: Partie en pause
Le système SHALL afficher, lorsque la partie est en pause, en grand le joueur à qui c'est de jouer (avatar et nom) avec la mention de la reprise, sur le même fond que la partie en cours (image du jeu et couleur de fond).

#### Scenario: Pause
- **WHEN** la partie est en pause avec Marie comme joueur suivant
- **THEN** l'écran affiche l'avatar de Marie en grand avec son nom

#### Scenario: Pas de QR pendant la pause
- **WHEN** la partie est en pause
- **THEN** aucun QR code n'est affiché (le token a été consommé)

#### Scenario: Fond en pause
- **WHEN** la partie d'un jeu doté d'une image est en pause
- **THEN** le fond reprend l'image et la couleur du jeu
