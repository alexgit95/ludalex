## Why

Sur l'écran Affichage (tablette), pendant une partie en cours, les avatars des joueurs sont rognés en bas de l'écran (l'image du jeu occupe jusqu'à 62vh dans un conteneur `overflow: hidden`) et trop petits pour être lus de loin. L'écran ne montre pas non plus qu'il s'agit d'un affrontement. L'image du jeu en grand prend la place qui devrait revenir aux joueurs.

## What Changes

- Corriger la mise en page de la partie en cours : les joueurs ne sont jamais rognés (grille `minmax(0, 1fr)`) et leurs avatars sont nettement plus grands, dimensionnés selon la hauteur de l'écran.
- L'image du jeu n'est plus affichée en grand au premier plan : elle devient le fond de l'écran (floutée, sous un voile léger), sur la couleur de fond du jeu qui reste la base et le repli.
- Ajouter un cartouche « VS » (lettrage de titre, épées croisées en SVG, pulsation lente et discrète, désactivée si `prefers-reduced-motion`) qui matérialise le combat en cours.
- Adapter la présentation des joueurs selon la partie : duel (face à face), 3 joueurs ou plus sans équipe (VS entre chaque paire), parties par équipes (colonnes d'équipes séparées par le VS, nom d'équipe visible), jeu coopératif (libellé « Ensemble » à la place du VS), joueur seul (pas de VS).
- Appliquer le même fond (image du jeu + couleur) aux écrans de pause et de victoire.
- Aucun **BREAKING** : pas de changement de schéma, d'API ni de configuration.

## Capabilities

### New Capabilities

### Modified Capabilities
- `ecran-affichage`: exigence « Affichage de la partie en cours » (image du jeu en fond, avatars agrandis et jamais rognés, cartouche VS, présentation selon duel / équipes / coopératif / solo) et exigence « Partie en pause » (même fond).

## Impact

- `src/main/resources/templates/affichage/etat.html` : structure de la section partie en cours, couche de fond, cartouche VS (SVG inline), groupes d'équipes.
- `src/main/resources/static/css/affichage.css` : grille, tailles en `vh`, fond, VS, animation.
- `AffichageService` / `VuePartie` : calcul du mode de présentation (duel, multi, équipes, coopératif, solo) et regroupement des participations par équipe ; tests unitaires associés (JUnit 5 + Mockito).
- `README.md`, `CHANGELOG.md`, version `pom.xml` (incrément mineur, versions identiques).
- Aucun impact sur la base de données (`docs/database.puml` inchangé), la sécurité, le CI/CD ni les images stockées.
