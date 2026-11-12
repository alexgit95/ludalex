## ADDED Requirements

### Requirement: Prêter un jeu
Le système SHALL permettre de prêter un jeu à une personne désignée par un texte libre, avec une date de prêt (par défaut aujourd'hui).

#### Scenario: Prêt enregistré
- **WHEN** l'utilisateur prête « Catan » à « Marie » le 2026-05-02
- **THEN** un prêt en cours est enregistré pour ce jeu

#### Scenario: Emprunteur vide
- **WHEN** l'utilisateur valide sans nom d'emprunteur
- **THEN** le système refuse

#### Scenario: Jeu déjà prêté
- **WHEN** l'utilisateur prête un jeu qui a déjà un prêt en cours
- **THEN** le système refuse

### Requirement: Retour d'un jeu
Le système SHALL permettre d'enregistrer le retour d'un jeu prêté, avec une date de retour (par défaut aujourd'hui).

#### Scenario: Retour enregistré
- **WHEN** l'utilisateur déclare le retour d'un jeu prêté
- **THEN** le prêt est clôturé avec sa date de retour et le jeu n'est plus indiqué comme prêté

#### Scenario: Retour antérieur au prêt
- **WHEN** l'utilisateur saisit une date de retour antérieure à la date de prêt
- **THEN** le système refuse

### Requirement: Historique des prêts
Le système SHALL conserver tous les prêts d'un jeu et les afficher du plus récent au plus ancien sur la fiche du jeu.

#### Scenario: Historique conservé
- **WHEN** un jeu a été prêté trois fois
- **THEN** la fiche affiche les trois prêts avec emprunteur, date de prêt et date de retour

### Requirement: Un seul prêt en cours par jeu
Le système SHALL garantir qu'un jeu n'a jamais plus d'un prêt en cours.

#### Scenario: Deux prêts simultanés
- **WHEN** deux demandes de prêt du même jeu arrivent simultanément
- **THEN** une seule est acceptée
