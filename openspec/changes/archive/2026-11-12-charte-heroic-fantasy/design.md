## Context

Le style tient en deux feuilles (`ludalex.css`, 99 lignes ; `affichage.css`, 60 lignes) avec quatre variables de couleur (`--fond`, `--encre`, `--accent`, `--bordure`) et la police système. Les gabarits utilisent peu de styles en ligne (trois occurrences dont la couleur de fond dynamique des jeux, qui doit rester). L'application est une PWA servie derrière Cloudflare, sans chaîne de build front, sur Raspberry Pi : toute ressource doit être statique, légère et locale.

## Goals / Non-Goals

**Goals:**
- Identité heroic fantasy sobre : crème, parchemin et lierres très discrets, propre et minimaliste.
- Lisibilité maximale : contraste, tailles, interlignes ; lisible de loin sur l'écran Affichage.
- Une seule source de vérité (variables CSS) pour toute l'application.

**Non-Goals:**
- Nouveau comportement, nouvelle page ou refonte de la navigation.
- Mode sombre, thèmes sélectionnables, animations complexes.
- Illustrations lourdes ou polices distantes.

## Decisions

- **Variables CSS** : `:root` définit palette et échelles (couleurs, rayons, ombres, espacements, tailles de texte). Palette de départ : fond `#fbf6ea` (crème), surface `#fffdf7` (blanc cassé), parchemin `#f3e9d2`, encre `#2b2118`, texte secondaire `#5a4a3a`, lierre `#4a6741`, or terni `#a8873f`, bordure `#d9ccae`, erreur `#8c2f2f`. Les valeurs exactes sont ajustées pour garantir un contraste WCAG AA (≥ 4,5:1) texte/fond, vérifié par un script de test.
- **Typographie** : pile sérif sans téléchargement (`"Iowan Old Style", "Palatino Linotype", Palatino, Georgia, serif`) pour les titres ; corps en sans-serif humaniste système ou sérif de lecture, 17-18 px, interligne 1,55. Option retenue seulement si le rendu système est insuffisant : une police libre (SIL OFL) auto-hébergée en `woff2`, ≤ 60 Ko, `font-display: swap`, ajoutée au cache de la PWA. Pas de police distante.
- **Parchemin** : texture obtenue par dégradés CSS superposés et un grain SVG (`feTurbulence`) en `background-image` encodé en data-URI de quelques centaines d'octets, opacité ≤ 6 %, appliquée au `body` et aux cartes ; pas d'image raster.
- **Lierres** : 2 à 3 SVG de quelques Ko (`static/decor/`) utilisés en `background-image` : coin d'en-tête, séparateur de sections, angle de cartes. Opacité faible, jamais sous du texte courant (`pointer-events: none`, décor placé dans les marges et pseudo-éléments `::before` / `::after`).
- **Composants** : en-tête sur parchemin avec filet or, boutons pleins lierre (texte crème), boutons secondaires à contour, vignettes de jeux avec bordure fine et coins légèrement arrondis (rayon ≤ 8 px), tableaux à lignes discrètes, états `:focus-visible` nets (anneau or) conservés pour l'accessibilité.
- **Écran Affichage** : la couleur de fond du jeu reste dynamique (style en ligne conservé) ; les textes passent dans des cartouches parchemin semi-opaques (ombre légère) pour rester lisibles quelle que soit la couleur ; polices agrandies par `clamp`, lierres en bordure d'écran ; l'animation de victoire est conservée.
- **PWA** : `theme-color` et `background_color` du manifeste reprennent le crème et l'encre ; version du cache du service worker incrémentée pour que les nouveaux CSS remplacent les anciens.
- **Méthode** : travail par couches (variables, base, composants, pages, Affichage) ; la validation visuelle se fait dans le navigateur sur chaque famille de pages (accueil, ludothèque, fiche, parties, partie en direct, statistiques, admin, erreurs, Affichage dans ses 4 états).

## Risks / Trade-offs

- [Décor qui nuit à la lisibilité] → lierres hors du texte, opacité faible, contraste mesuré ; un décor jugé gênant est retiré plutôt qu'atténué.
- [Cache PWA qui garde l'ancien style] → version du cache incrémentée, vérification après déploiement.
- [Fonds colorés des jeux contrastant mal avec le texte] → cartouches parchemin sur l'écran Affichage et les fiches.
- [Régression visuelle sur une page oubliée] → liste de contrôle des pages dans les tâches, test automatisé qui vérifie l'absence de couleurs codées en dur hors variables.
- [Police auto-hébergée trop lourde pour le Pi] → plafond de poids, repli sur la pile système si le gain est faible.

## Open Questions

- Choix définitif corps sérif ou sans-serif et éventuelle police libre, tranchés après essais sur écran réel (poste et télé de l'Affichage).
