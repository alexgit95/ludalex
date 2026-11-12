# fin-partie-token Specification

## Purpose
Token à usage unique et page publique /fin/{token} pour terminer ou mettre en pause une partie, avec limitation de débit.

## Requirements

### Requirement: Génération du token
Le système SHALL générer pour une partie active un token aléatoire d'au moins 128 bits issu d'un générateur cryptographique, ne stocker que son empreinte (hachage), et le rattacher à une seule partie.

#### Scenario: Token généré
- **WHEN** un token est généré pour une partie
- **THEN** la base ne contient que l'empreinte du token, sa date d'expiration à 7 jours et son état « non utilisé »

#### Scenario: Nouveau token
- **WHEN** un nouveau token est généré pour une partie qui en possède déjà un non utilisé
- **THEN** l'ancien est invalidé et un seul token reste valide pour cette partie

### Requirement: Token à usage unique
Un token SHALL être consommé par la première action réussie (terminer ou mettre en pause) et ne plus jamais être accepté ensuite, même en cas de requêtes simultanées.

#### Scenario: Réutilisation
- **WHEN** un token déjà consommé est présenté à nouveau
- **THEN** le système refuse sans effet sur la partie

#### Scenario: Requêtes simultanées
- **WHEN** deux requêtes valides avec le même token arrivent en même temps
- **THEN** une seule est acceptée

### Requirement: Expiration du token
Un token SHALL expirer 7 jours après sa génération.

#### Scenario: Token expiré
- **WHEN** un token expiré est présenté
- **THEN** le système refuse sans effet sur la partie

### Requirement: Token limité à sa partie
Un token SHALL permettre d'agir uniquement sur la partie à laquelle il est rattaché, et uniquement tant que celle-ci est active.

#### Scenario: Partie déjà terminée
- **WHEN** un token est présenté alors que la partie est terminée
- **THEN** le système refuse

#### Scenario: Joueur d'une autre partie
- **WHEN** une requête avec le token de la partie A désigne un joueur qui n'est pas dans la partie A
- **THEN** le système refuse

### Requirement: Page publique de fin de partie
Le système SHALL servir `/fin/{token}` sans JWT Cloudflare ; la page affiche le jeu et les joueurs de la partie rattachée et propose de terminer (gagnants, scores facultatifs ou résultat coopératif) ou de mettre en pause (joueur suivant obligatoire).

#### Scenario: Ouverture avec un token valide
- **WHEN** un invité ouvre `/fin/<token valide>` depuis son téléphone
- **THEN** la page affiche le nom du jeu, les joueurs et les actions possibles, sans consommer le token

#### Scenario: Fin par le QR
- **WHEN** l'invité désigne Marie gagnante et valide
- **THEN** la partie est terminée, le token est consommé et une page de confirmation s'affiche

#### Scenario: Pause par le QR
- **WHEN** l'invité met en pause en désignant Paul
- **THEN** la partie est en pause, Paul est enregistré, le token est consommé

#### Scenario: Scores facultatifs
- **WHEN** l'invité termine sans saisir de score
- **THEN** la fin est acceptée

### Requirement: Aucune fuite d'information
La page et les réponses de `/fin/{token}` MUST NOT exposer d'autre donnée que celles de la partie rattachée, et MUST répondre de façon identique pour un token inconnu, expiré ou déjà utilisé.

#### Scenario: Token inconnu
- **WHEN** un token inexistant est présenté
- **THEN** le système répond par la même page neutre que pour un token expiré ou utilisé, sans indiquer la raison

#### Scenario: Navigation hors de la page
- **WHEN** un invité modifie l'URL pour viser `/jeux` ou `/`
- **THEN** il est refusé (401 en `prod`)

#### Scenario: En-têtes de protection
- **WHEN** la page `/fin/{token}` est servie
- **THEN** elle porte `Cache-Control: no-store`, `Referrer-Policy: no-referrer` et `X-Robots-Tag: noindex`

### Requirement: Limitation de débit
Le système SHALL limiter le nombre de requêtes vers `/fin/**` par adresse source et renvoyer 429 au-delà du seuil.

#### Scenario: Rafale de requêtes
- **WHEN** une même adresse dépasse le seuil autorisé en une minute
- **THEN** le système répond 429 sans consulter la base

### Requirement: Protection CSRF de la page publique
Les actions de `/fin/{token}` SHALL rester protégées par un jeton CSRF fourni avec la page.

#### Scenario: Soumission sans jeton
- **WHEN** une soumission POST arrive sans jeton CSRF valide
- **THEN** le système répond 403 et le token n'est pas consommé
