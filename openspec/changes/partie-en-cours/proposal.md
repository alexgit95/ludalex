## Why

Au-delà de la déclaration a posteriori, l'utilisateur veut lancer une partie en direct, la mettre en pause (en notant à qui c'était de jouer) et la terminer, y compris depuis le téléphone de n'importe quel invité via un QR code. Ce QR code ouvre un accès très limité sans Cloudflare Access : c'est le point le plus sensible de la sécurité de l'application.

## What Changes

- Statuts `EN_COURS` et `EN_PAUSE` sur `Partie` (champs déjà prévus par `parties`) ; **une seule partie en cours ou en pause à la fois**.
- Lancement : jeu + joueurs (équipes facultatives). Lancer une seconde partie impose de **terminer d'abord la précédente** en remplissant ses champs de fin.
- Pause avec le joueur à qui c'était de jouer (obligatoire), reprise, fin depuis l'application.
- **Token de fin de partie** : aléatoire, haché, lié à une partie, à usage unique, expirant à 7 jours, limité en débit.
- Page publique `/fin/{token}` : terminer (gagnants multiples, scores facultatifs, résultat coopératif) ou mettre en pause ; aucune autre donnée exposée.
- Tests de sécurité exigés : token valide, expiré, déjà utilisé, d'une autre partie, inconnu.

## Capabilities

### New Capabilities
- `partie-en-cours`: lancement, pause, reprise, fin, règle d'unicité de la partie active.
- `fin-partie-token`: génération, stockage, validation et consommation du token ; page publique `/fin/{token}`.

### Modified Capabilities
<!-- Aucune : le modèle `Partie` est étendu mais n'est pas encore archivé -->

## Impact

- Code : package `parties.encours` (service de cycle de vie, tokens, contrôleur public `/fin`), filtre de limitation de débit.
- Base : table `token_fin_partie` ; champ `terminee_le` sur `partie`.
- Sécurité : exception `/fin/**` du filtre JWT (posée par `socle`), règle Cloudflare Bypass (`docs/fin-de-partie-qr.md`).
- Dépend de `parties`. Version : `0.7.0`.
