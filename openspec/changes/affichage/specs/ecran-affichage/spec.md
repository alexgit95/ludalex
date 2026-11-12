## ADDED Requirements

### Requirement: Affichage de la partie en cours
Le système SHALL afficher sur `/affichage`, lorsqu'une partie est en cours, l'image du jeu en grand sur un fond de la couleur de fond du jeu, l'avatar et le nom de chaque joueur, et un QR code.

#### Scenario: Partie en cours
- **WHEN** une partie « en cours » de « Catan » avec trois joueurs est active
- **THEN** l'écran montre l'image de Catan, le fond de sa couleur, les trois avatars et le QR code

#### Scenario: Image absente
- **WHEN** le jeu n'a pas d'image
- **THEN** l'écran affiche le titre du jeu en grand à la place

#### Scenario: Avatar absent
- **WHEN** un joueur n'a pas d'avatar
- **THEN** l'écran affiche l'avatar par défaut avec son nom

### Requirement: QR code de fin de partie
Le système SHALL afficher un QR code encodant l'URL `/fin/{token}` de l'origine publique de la requête, généré côté serveur ; le token en clair n'est jamais stocké.

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
Le système SHALL afficher, lorsque la partie est en pause, en grand le joueur à qui c'est de jouer (avatar et nom) avec la mention de la reprise.

#### Scenario: Pause
- **WHEN** la partie est en pause avec Marie comme joueur suivant
- **THEN** l'écran affiche l'avatar de Marie en grand avec son nom

#### Scenario: Pas de QR pendant la pause
- **WHEN** la partie est en pause
- **THEN** aucun QR code n'est affiché (le token a été consommé)

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
- **WHEN** l'écran est ouvert plus de 30 secondes après la fin d'une partie
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
