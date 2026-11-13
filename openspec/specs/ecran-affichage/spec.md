# ecran-affichage Specification

## Purpose
Écran Affichage : partie en cours, QR de fin de partie, pause, victoire et mise à jour automatique.

## Requirements

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

### Requirement: QR code de fin de partie
Le système SHALL afficher un QR code encodant l'URL `/fin/{token}` de l'origine publique de la requête, généré côté serveur ; le token en clair n'est jamais stocké en base (il n'est conservé qu'en mémoire du processus pour être réutilisé tant qu'il est valide). L'origine peut être imposée par `ludalex.affichage.url-publique`.

#### Scenario: QR code valide
- **WHEN** l'écran affiche la partie en cours
- **THEN** le QR code encode `https://<domaine>/fin/<token>` avec un token valide non utilisé de cette partie

#### Scenario: Nouveau token à l'affichage
- **WHEN** la page est ouverte ou rechargée
- **THEN** un nouveau token est généré et l'ancien token non utilisé est invalidé

#### Scenario: Renouvellement avant expiration
- **WHEN** le token a moins de 24 heures de validité restante
- **THEN** le système en génère un nouveau lors de la mise à jour suivante de l'écran

#### Scenario: Origine publique
- **WHEN** l'écran est servi derrière Cloudflare avec les headers `X-Forwarded-*`
- **THEN** l'URL du QR utilise le schéma et le domaine publics, pas l'adresse interne

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

### Requirement: Animation de victoire
Lorsqu'une partie se termine alors que l'écran est ouvert, le système SHALL déclencher une animation sur l'avatar et l'image de victoire de chaque vainqueur pendant environ 15 secondes, puis revenir à la veille.

#### Scenario: Fin par scan du QR
- **WHEN** un invité termine la partie via le QR en désignant Marie
- **THEN** l'écran, dans les secondes qui suivent, anime l'avatar et l'image de victoire de Marie

#### Scenario: Plusieurs vainqueurs
- **WHEN** deux joueurs gagnent
- **THEN** les deux avatars sont animés

#### Scenario: Défaite coopérative
- **WHEN** une partie coopérative est perdue
- **THEN** l'écran affiche un message de défaite sans animation de victoire

#### Scenario: Écran ouvert après coup
- **WHEN** l'écran est ouvert plus de 15 secondes après la fin d'une partie
- **THEN** il n'affiche pas l'animation

### Requirement: Mise à jour automatique sobre
L'écran SHALL se mettre à jour par interrogation du serveur toutes les 3 secondes, avec une réponse légère, sans recharger la page entière.

#### Scenario: Nouvelle partie lancée
- **WHEN** une partie est lancée pendant que l'écran est en veille
- **THEN** l'écran passe à l'affichage de la partie en moins de 5 secondes

#### Scenario: Charge serveur
- **WHEN** l'écran reste ouvert une journée
- **THEN** les requêtes de mise à jour ne chargent aucune collection volumineuse

### Requirement: Reprise après expiration de la session Cloudflare
Si les mises à jour échouent de façon répétée (session Cloudflare expirée, réseau coupé), l'écran SHALL recharger la page entière afin que Cloudflare Access redemande la connexion, puis reprendre normalement.

#### Scenario: Session expirée
- **WHEN** trois mises à jour consécutives échouent
- **THEN** la page est rechargée en entier

#### Scenario: Retour du réseau
- **WHEN** le réseau revient après une coupure
- **THEN** l'écran reprend ses mises à jour sans intervention

### Requirement: Écran maintenu allumé
L'écran SHALL demander au navigateur de ne pas se mettre en veille lorsque l'API correspondante est disponible.

#### Scenario: API indisponible
- **WHEN** le navigateur ne propose pas l'API de maintien d'écran
- **THEN** l'affichage fonctionne sans erreur
