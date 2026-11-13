## Context

`etat.html` rend la partie en cours avec une grille `1fr auto` : l'image du jeu (jusqu'à 62vh), puis les joueurs en bas, dans un `body.kiosque` en `overflow: hidden`. Le total dépasse 100vh sur tablette : la rangée des joueurs est rognée. Les avatars valent `clamp(4rem, 9vw, 8rem)`. `VuePartie` expose déjà `participations`, `equipes` et `jeu.isCooperatif()` ; `Participation.getEquipe()` donne l'équipe d'un joueur. La signature d'état (`EN_COURS:id:hash du token`) évite les remplacements HTMX inutiles toutes les 3 s. La couleur de fond du jeu est déjà appliquée en `background-color` sur `.zone`.

## Goals / Non-Goals

**Goals:**
- Joueurs toujours visibles, grands, lisibles de loin sur tablette.
- Image du jeu en fond, avec la couleur du jeu comme base et repli.
- Cartouche VS thématique et sobre, adapté à duel, multi, équipes, coopératif, solo.

**Non-Goals:**
- Aucun changement de schéma, d'API, de sécurité, de configuration ni de CI/CD.
- Pas de nouvelle bibliothèque ni de chaîne de build front.
- Les écrans de veille et de victoire ne sont pas retouchés (hors fond de pause).

## Decisions

- **Grille** : `grid-template-rows: minmax(0, 1fr) auto` ; la zone du jeu perd son image au premier plan, ne garde que le titre. Le problème de dépassement disparaît à la racine au lieu d'être arbitré par un `max-height` en `vh`. Alternative écartée : réduire seulement l'image (fragile selon le ratio d'écran).
- **Avatars en `vh`** : taille de type `clamp(6rem, 18vh, 14rem)`, la hauteur étant la contrainte réelle d'un écran en paysage. Alternative écartée : `vw` (variable selon le ratio).
- **Fond** : un `<div class="fond-jeu" aria-hidden="true">` dédié (`position: absolute; inset: 0; z-index: 0`) portant l'image via variable CSS `--fond-image`, avec flou et `scale(1.1)`, plus un voile en dégradé teinté. `.zone::before/::after` restent le décor de coins. Le flou s'applique à une couche statique (aucune animation), pour limiter le coût GPU ; le rendu est à valider sur la tablette réelle, avec repli possible vers un simple voile plus opaque sans flou. L'URL est échappée côté Thymeleaf ; une image par URL est chargée par le navigateur seul (aucune requête serveur, pas de surface SSRF nouvelle). La couleur reste la base et le repli si l'image échoue.
- **Fond en pause** : le même fragment de fond est factorisé et réutilisé pour `EN_COURS` et `EN_PAUSE`.
- **Mode de présentation** : un type calculé côté serveur (par exemple dans `VuePartie` ou un petit enregistrement dédié) : `COOPERATIF`, `SOLO`, `EQUIPES`, `DUEL`, `MULTI`. Priorité : coopératif, puis solo, puis équipes (au moins une participation avec équipe), puis duel (2 joueurs), sinon multi. Le gabarit ne contient aucune règle métier, ce qui permet de tester la logique en JUnit. Les joueurs sans équipe dans une partie par équipes forment un groupe distinct sans nom.
- **Regroupement** : les participations sont regroupées par équipe (ordre stable : celui des équipes de la partie, puis le groupe sans équipe). Pas de requête supplémentaire : `DetailPartie` fournit déjà tout, sans collection volumineuse.
- **VS** : cartouche parchemin (mêmes variables que `.titre-jeu` et `.nom`, ajouté à la liste de sélecteurs partagée), épées croisées en SVG inline (`currentColor`/`--or`, `aria-hidden`). Pulsation dédiée `pulsation-vs` : échelle 1 vers 1,04, 2,5 à 3 s, `ease-in-out alternate`, `transform`/`opacity` seulement, `will-change: transform` sur le seul cartouche, désactivée sous `prefers-reduced-motion: reduce`. L'animation `pulsation` existante (1,12 en 0,9 s) est réservée à la victoire.
- **Signature** : la composition des équipes est fixée au lancement et ne change pas en cours de partie ; la signature actuelle suffit, donc l'animation du VS n'est pas réinitialisée toutes les 3 s.

## Risks / Trade-offs

- [Coût du flou sur tablette modeste] → couche statique, flou modéré ; repli sans flou si nécessaire.
- [Lisibilité du texte sur un fond variable] → cartouches parchemin conservés et voile en dégradé.
- [Nombreux joueurs ou équipes sur un petit écran] → avatars en `vh` avec retour à la ligne (`flex-wrap`) et plancher de taille ; scénario de rognage couvert par la spec.
- [Image très claire ou très chargée en fond] → voile teinté ; couleur de fond modifiable manuellement déjà possible côté jeu.
- [VS et animation en continu sur batterie] → amplitude faible, un seul élément animé, `prefers-reduced-motion`.
