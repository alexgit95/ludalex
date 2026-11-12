## Why

L'interface actuelle (vert grisé, police système) est fonctionnelle mais sans identité. L'utilisateur veut une charte propre, minimaliste et tournée vers l'heroic fantasy : tons crème et blanc cassé, parchemin et petits lierres très discrets, avec une typographie qui reste parfaitement lisible, y compris sur l'écran Affichage regardé de loin.

## What Changes

- Nouvelle charte graphique commune à toute l'application : palette crème / blanc cassé, encre brun-noir, accents vert lierre et or terni ; texte sur fond clair toujours à fort contraste.
- Texture de parchemin très subtile (dégradé et grain en CSS/SVG intégrés, aucune image lourde) et petits lierres en SVG discrets (coins d'en-tête, séparateurs, bordures de cartes), jamais sous le texte courant.
- Typographie : titres sérif à caractère (pile de polices système, éventuellement une police auto-hébergée légère sous licence libre), corps de texte sérif ou sans-serif très lisible, tailles et interlignes généreux.
- Reprise des composants existants (en-tête, navigation, boutons, formulaires, vignettes de jeux, fiches, tableaux, classement, messages, pages d'erreur) sur les mêmes variables.
- Écran Affichage inclus : habillage heroic fantasy compatible avec les fonds colorés des jeux (cartouches parchemin derrière les textes, lisibilité de loin), victoire et pause conservées.
- PWA : couleurs `theme-color` et manifeste alignées sur la nouvelle palette ; icônes inchangées sauf nécessité.
- Aucun changement de comportement fonctionnel, de données ni d'API.

## Capabilities

### New Capabilities
- `charte-graphique`: identité visuelle heroic fantasy commune (palette, parchemin, lierres, typographie), lisibilité et sobriété, application à l'interface courante et à l'écran Affichage.

### Modified Capabilities
<!-- Aucune : les exigences fonctionnelles existantes ne changent pas -->

## Impact

- Code : `static/css/ludalex.css`, `static/css/affichage.css`, gabarits Thymeleaf (balises décoratives, suppression des `style=` en ligne restants), `fragments/base.html`, `manifest.webmanifest`, nouveaux SVG décoratifs sous `static/`.
- Aucune dépendance ajoutée, aucune ressource externe (CDN, polices distantes) : PWA et Cloudflare Access non impactés.
- Performance (Raspberry Pi) : CSS et SVG légers, pas de JavaScript, pas de grosse image de fond.
- Pas de modification de schéma ni de format de sauvegarde. Version : `0.10.0` si ce change est livré avant `import-myludo` (à ajuster selon l'ordre de livraison, une version par modification).
