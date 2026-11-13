## ADDED Requirements

### Requirement: Sauvegarde des variantes de jeux et de parties
Le système SHALL exporter en format ZIP v2 l'indicateur de variantes de chaque jeu et le libellé associé à chaque partie. La restauration SHALL accepter les formats v1 et v2; les données absentes d'un ZIP v1 SHALL être restaurées avec l'indicateur désactivé et sans variante.

#### Scenario: Aller-retour d'une sauvegarde v2
- **WHEN** l'utilisateur sauvegarde puis restaure des jeux et parties avec variantes
- **THEN** l'indicateur des jeux et les libellés des parties sont conservés sans modification

#### Scenario: Restaurer une sauvegarde v1
- **WHEN** l'utilisateur restaure une sauvegarde au format v1 sans champs de variantes
- **THEN** les jeux sont restaurés sans indicateur de variantes et les parties sans libellé

#### Scenario: Restauration entre SQLite et PostgreSQL
- **WHEN** une sauvegarde v2 est restaurée sur l'autre moteur de base pris en charge
- **THEN** les indicateurs et libellés sont conservés avec les autres données de jeu et de partie
