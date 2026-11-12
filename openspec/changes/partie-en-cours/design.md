## Context

Ce change fait vivre le modèle `Partie` (statuts, joueur suivant) et pose l'unique accès sans Cloudflare Access de l'application (`/fin/{token}`, voir `docs/fin-de-partie-qr.md`). Le token est l'unique autorisation : il doit être robuste, minimal et à usage unique. Précondition : `parties`. Le filtre JWT exempte déjà `/fin/**` (`socle`).

## Goals / Non-Goals

**Goals:**
- Cycle de vie complet d'une partie (lancer, pause, reprise, fin) avec unicité de la partie active.
- Token ≥ 128 bits, haché, à usage unique, expirant à 7 jours, limité en débit.
- Page `/fin/{token}` minimale, sans fuite.

**Non-Goals:**
- Affichage et QR code (change `affichage`).
- Plusieurs parties simultanées.
- Correction d'un résultat via le token (usage unique).

## Decisions

- **Token** : 256 bits (`SecureRandom`, 32 octets) encodés en Base64 URL-safe ; empreinte **SHA-256** stockée (le token étant à haute entropie, un hachage rapide suffit, un hachage lent type bcrypt serait inutile et coûteux sur Pi). Recherche par empreinte, ce qui évite les comparaisons de secret en base.
- **Entité `TokenFinPartie`** : `id`, `partie`, `empreinte` (unique, indexée), `expireLe`, `utilise`. Un seul token valide par partie : générer en invalide les précédents.
- **Le token en clair n'existe que dans l'URL du QR** : l'écran Affichage (protégé par JWT) doit donc pouvoir en obtenir un frais ; la génération est un service appelé par l'affichage (voir `affichage`), qui invalide l'ancien token non utilisé.
- **Consommation atomique** : `UPDATE token_fin_partie SET utilise = true WHERE id = :id AND utilise = false AND expire_le > :now` ; un seul appelant obtient `1`. Pas de verrou applicatif ; compatible SQLite et PostgreSQL.
- **Réponse uniforme** : inconnu, expiré, utilisé, partie terminée → même page neutre (HTTP 404) pour ne rien révéler.
- **Limitation de débit** : filtre en mémoire (fenêtre glissante, seau par adresse, 30 requêtes/minute) restreint à `/fin/**` ; adresse lue via les headers `X-Forwarded-For`/`CF-Connecting-IP` uniquement si la requête vient d'un proxy de confiance (`forward-headers-strategy` du socle). Nettoyage périodique des seaux inactifs (pas de fuite mémoire). Une règle de rate limiting Cloudflare est recommandée en complément (fiche docs).
- **Pas de session côté invité au-delà du CSRF** : la page `/fin/{token}` émet un jeton CSRF (session technique anonyme, durée courte).
- **En-têtes** : `Cache-Control: no-store`, `Referrer-Policy: no-referrer`, `X-Robots-Tag: noindex` sur `/fin/**`.
- **Unicité de la partie active** : lecture avec verrou pessimiste sur la ligne de partie active (ou table de contrôle simple) dans la transaction de lancement ; lancement refusé si une partie est `EN_COURS` ou `EN_PAUSE`. Le formulaire de fin intercalé est piloté côté interface : `POST /parties/lancer` renvoie le formulaire de fin de la précédente (HTMX) puis relance.
- **Terminer** : service unique `terminer(partie, gagnants|résultat, scores)` partagé entre l'application et le token ; il pose `statut = TERMINEE`, `termineeLe = now`, invalide les tokens et calcule `gagnant` (règles de `parties`).
- **Pause** : `joueurSuivant` obligatoire, appartenance à la partie vérifiée ; tokens invalidés ; reprise génère un nouveau token (à la demande de l'affichage).
- **Anonymisation** : une partie active ne peut pas contenir de joueur anonymisé ; la suppression d'un joueur pendant une partie active est refusée par l'interface `ParticipationsJoueur` existante (la partie compte comme participation).

## Risks / Trade-offs

- [Fuite d'un QR (photo)] → usage unique, rattaché à une partie active, expire ; l'impact maximal est de désigner un gagnant, corrigeable depuis l'application.
- [Force brute du token] → 256 bits + rate limiting ; infaisable.
- [Rate limiting en mémoire non partagé] → une seule instance prévue ; acceptable.
- [Adresse source falsifiée via headers] → headers pris en compte uniquement via la configuration de proxy de confiance.
- [Impossibilité de recréer le QR depuis l'empreinte] → régénération contrôlée côté `affichage`.
- [Blocage si l'utilisateur ne peut terminer la partie précédente] → le formulaire de fin est toujours accessible depuis l'application.
