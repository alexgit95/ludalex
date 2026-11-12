## Context

L'écran Affichage tourne en continu sur un navigateur (télé, tablette), protégé par Cloudflare Access (session d'un mois). Il lit l'état de la partie active créé par `partie-en-cours`. Contraintes : Raspberry Pi, pas de SPA ni de chaîne de build front, pas de dépendance externe pour le diaporama.

## Goals / Non-Goals

**Goals:**
- Affichage lisible de loin, mis à jour en quelques secondes.
- QR code toujours porteur d'un token valide, sans stocker le token en clair.
- Animation de victoire fiable sans WebSocket.

**Non-Goals:**
- Plusieurs écrans synchronisés avec des états différents.
- Gestion des images du diaporama depuis l'interface (embarquées à la compilation).
- Sons.

## Decisions

- **Polling HTMX plutôt que SSE/WebSocket** : `hx-get="/affichage/etat" hx-trigger="every 3s" hx-swap="outerHTML"`. Les connexions longues traversent mal les timeouts de Cloudflare et occupent des ressources sur le Pi ; une requête de 3 s sur un petit fragment est négligeable. Alternative rejetée : SSE (heartbeats, reconnexions, 100 s d'inactivité Cloudflare).
- **Point d'état unique** : `/affichage/etat` renvoie le fragment complet de l'état courant : `EN_COURS` (jeu, joueurs, QR), `EN_PAUSE` (joueur suivant), `VICTOIRE` (terminée depuis moins de 30 s), `VEILLE`. Un résumé d'état (hash) dans un attribut permet de ne pas rejouer l'animation à chaque poll.
- **Animation de victoire** : l'état `VICTOIRE` est déduit de `termineeLe` (≤ 30 s). La page détecte la transition vers `VICTOIRE` et lance les animations CSS (`@keyframes`) ; après 15 s le fragment suivant repasse en veille. Ouvrir l'écran au-delà de 30 s n'anime pas.
- **Token du QR** : à l'ouverture de la page et à chaque renouvellement (token expirant dans moins de 24 h), le service génère un nouveau token (l'ancien non utilisé est invalidé) ; le clair n'est jamais stocké. Le polling n'en génère pas à chaque appel : seulement si absent, consommé ou bientôt expiré. Inconvénient accepté : le QR change au rechargement de la page, un scan antérieur devient invalide.
- **URL du QR** : schéma et hôte tirés de la requête (headers `X-Forwarded-*` pris en charge par le socle), sans nouvelle variable d'environnement obligatoire ; propriété optionnelle `ludalex.url-publique` en surcharge (par exemple en `dev`).
- **QR code** : ZXing `core` (Java pur) génère un SVG inline (`BitMatrix` → chemin SVG) ; pas de PNG ni de décodage d'image, donc faible coût.
- **Diaporama** : images dans `src/main/resources/static/diaporama/` énumérées au démarrage (`PathMatchingResourcePatternResolver`) ; le fragment `VEILLE` fournit la liste et un script minimal (≈ 20 lignes) alterne deux couches avec fondu CSS ; une image préchargée, une affichée.
- **Reprise après session Cloudflare expirée** : un petit script compte les échecs de `htmx:responseError`/`htmx:sendError` ; au troisième échec consécutif, `location.reload()`, ce qui fait réapparaître la connexion Cloudflare ; compteur remis à zéro au premier succès.
- **Écran allumé** : `navigator.wakeLock.request('screen')` si disponible (HTTPS), ignoré sinon.
- **Pas de données superflues** : le fragment ne contient que nom, avatar, image de victoire et couleur ; aucune statistique.
- **MVC Thymeleaf** : pas d'API REST, pas d'OpenAPI.

## Risks / Trade-offs

- [Polling permanent sur Raspberry Pi] → fragment minuscule, requêtes indexées (partie active, `termineeLe`) ; intervalle configurable.
- [Un QR périmé après rechargement de l'écran] → documenté ; l'invité peut rescanner le nouveau.
- [Animation ratée si le navigateur dort] → l'état `VICTOIRE` reste visible 30 s ; au-delà elle est perdue (accepté).
- [Session Cloudflare à renouveler chaque mois] → rechargement automatique qui affiche la connexion ; une intervention humaine est requise une fois par mois (à documenter dans le README).
- [Images de diaporama trop lourdes] → plafond de 2 Mo documenté ; vérifié à la revue.
