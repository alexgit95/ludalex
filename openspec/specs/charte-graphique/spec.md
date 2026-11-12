# charte-graphique Specification

## Purpose
Identité visuelle heroic fantasy sobre de toute l'application : palette, lisibilité, décors discrets, Affichage, PWA.

## Requirements

### Requirement: Palette et ambiance heroic fantasy
L'application SHALL présenter une identité visuelle unique, sobre et minimaliste, fondée sur des fonds crème et blanc cassé, une encre brun foncé et des accents vert lierre et or terni, définie par des variables CSS communes à toutes les pages.

#### Scenario: Pages courantes
- **WHEN** l'utilisateur ouvre l'accueil, la ludothèque, une fiche de jeu, les parties, les statistiques ou l'administration
- **THEN** elles utilisent la même palette et les mêmes composants (en-tête, boutons, formulaires, tableaux, messages)

#### Scenario: Pages d'erreur
- **WHEN** une page d'erreur (403, 404, 500) s'affiche
- **THEN** elle reprend la même charte

### Requirement: Lisibilité du texte
Le texte SHALL rester pleinement lisible : contraste d'au moins 4,5:1 entre texte et fond, taille de corps d'au moins 16 px, interligne confortable, et aucun décor placé sous le texte courant.

#### Scenario: Contraste
- **WHEN** la palette est vérifiée
- **THEN** chaque couple texte/fond utilisé atteint un contraste d'au moins 4,5:1

#### Scenario: Texte sur parchemin
- **WHEN** un texte est affiché sur la texture de parchemin
- **THEN** la texture est assez discrète pour ne pas nuire à sa lecture

#### Scenario: Focus clavier
- **WHEN** un élément interactif reçoit le focus au clavier
- **THEN** un contour visible et contrasté est affiché

### Requirement: Parchemin et lierres discrets
La charte SHALL utiliser une texture de parchemin très subtile et de petits motifs de lierre discrets, réalisés en CSS et SVG légers intégrés à l'application, sans ressource externe ni image raster lourde.

#### Scenario: Décor discret
- **WHEN** une page s'affiche
- **THEN** les lierres n'apparaissent que dans les marges, coins, séparateurs ou bordures, avec une faible opacité

#### Scenario: Hors ligne et Cloudflare
- **WHEN** l'application est servie derrière Cloudflare Access ou hors ligne (PWA)
- **THEN** le décor et les polices s'affichent sans requête vers un domaine externe

### Requirement: Typographie
Les titres SHALL utiliser une typographie sérif à caractère et le corps de texte une typographie très lisible, sans téléchargement depuis un service externe ; toute police auto-hébergée SHALL être sous licence libre et de poids réduit.

#### Scenario: Police absente
- **WHEN** une police auto-hébergée n'est pas chargée
- **THEN** une pile de polices système équivalente est utilisée sans rupture de mise en page

### Requirement: Écran Affichage aux couleurs de la charte
L'écran Affichage SHALL adopter la charte en conservant la couleur de fond propre à chaque jeu et en gardant tous les textes lisibles de loin, quelle que soit cette couleur.

#### Scenario: Fond de jeu quelconque
- **WHEN** la partie en cours a un fond très clair ou très foncé
- **THEN** le nom du jeu et des joueurs restent lisibles grâce à un cartouche contrastant

#### Scenario: Quatre états
- **WHEN** l'écran passe de la veille à une partie, une pause, puis une victoire
- **THEN** chaque état respecte la charte sans changer son contenu fonctionnel

### Requirement: Aucun impact fonctionnel
La refonte graphique SHALL ne modifier ni le comportement, ni les données, ni les URL, ni les fonctions existantes.

#### Scenario: Parcours existants
- **WHEN** les tests fonctionnels existants sont exécutés
- **THEN** ils réussissent sans modification de leurs attentes

#### Scenario: Mise à jour de la PWA
- **WHEN** une nouvelle version est déployée
- **THEN** les nouveaux styles remplacent ceux en cache après rechargement

### Requirement: Menu burger sur petit écran
Sur petit écran, la navigation principale SHALL être repliée derrière un bouton burger à l'icône aux couleurs du thème, qui ouvre un panneau parchemin sous l'en-tête ; le bouton expose `aria-expanded`, le panneau se ferme au clic extérieur et sur Échap ; sans JavaScript la navigation reste affichée.

#### Scenario: Ouverture
- **WHEN** l'utilisateur appuie sur le burger sur petit écran
- **THEN** le panneau parchemin s'affiche sous l'en-tête et `aria-expanded` vaut `true`

#### Scenario: Fermeture
- **WHEN** l'utilisateur clique à l'extérieur ou appuie sur Échap
- **THEN** le panneau se ferme et `aria-expanded` vaut `false`

#### Scenario: Grand écran
- **WHEN** la largeur est supérieure ou égale à 40 rem
- **THEN** la navigation reste affichée en ligne, sans bouton burger

#### Scenario: Sans JavaScript
- **WHEN** JavaScript est indisponible
- **THEN** les liens de navigation restent visibles et utilisables
