## ADDED Requirements

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
