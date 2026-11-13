## ADDED Requirements

### Requirement: Onglet Statistiques dans la navigation
La navigation principale SHALL contenir un onglet « Statistiques », visible aussi dans le menu burger sur petit écran.

#### Scenario: Navigation
- **WHEN** une page quelconque est affichée
- **THEN** la navigation contient le lien « Statistiques » vers `/statistiques`

#### Scenario: Petit écran
- **WHEN** l'écran est étroit et le menu burger ouvert
- **THEN** le lien « Statistiques » y figure
