## ADDED Requirements

### Requirement: Réglages persistés
Le système SHALL persister les réglages de l'application sous forme de paires clé/valeur dans une table unique, chaque réglage connu ayant un type, une valeur par défaut et des bornes de validation, et SHALL utiliser la valeur par défaut tant qu'aucune valeur n'est enregistrée.

#### Scenario: Valeur par défaut
- **WHEN** aucun réglage n'a été enregistré
- **THEN** les seuils valent 6 mois (jeu dormant), 5 parties (classement) et 30 jours (prêt long)

#### Scenario: Clé inconnue
- **WHEN** une clé absente du catalogue des réglages est lue ou écrite
- **THEN** elle est rejetée

### Requirement: Modification depuis l'administration
Le système SHALL permettre de modifier les réglages depuis l'administration, protégée par le contrôle d'accès et par CSRF, avec validation des valeurs (entiers positifs bornés) et possibilité de revenir à la valeur par défaut.

#### Scenario: Enregistrement valide
- **WHEN** l'utilisateur saisit 12 mois pour le seuil de jeu dormant
- **THEN** la valeur est enregistrée et le cache des statistiques est invalidé

#### Scenario: Valeur invalide
- **WHEN** l'utilisateur saisit une valeur négative, nulle ou hors bornes
- **THEN** l'enregistrement est refusé avec un message sur le champ concerné

#### Scenario: Rétablir les valeurs par défaut
- **WHEN** l'utilisateur rétablit un réglage
- **THEN** sa valeur enregistrée est supprimée et la valeur par défaut s'applique

### Requirement: Seuils des statistiques
Le système SHALL exposer comme réglages le délai d'inactivité d'un jeu dormant (mois), le nombre minimal de parties pour figurer au classement des taux de victoire et la durée à partir de laquelle un prêt est long (jours).

#### Scenario: Prise en compte
- **WHEN** un seuil est modifié
- **THEN** le tableau de bord suivant utilise la nouvelle valeur
