# dashboard-statistiques Specification

## Purpose
Tableau de bord global des statistiques (onglet Statistiques) : périodes, chiffres clés, diagrammes SVG/CSS rendus côté serveur et cache de 10 minutes invalidé à l'écriture.

## Requirements

### Requirement: Onglet Statistiques
Le système SHALL proposer un onglet « Statistiques » dans la navigation principale ouvrant le tableau de bord `/statistiques`, soumis au même contrôle d'accès que le reste de l'application.

#### Scenario: Accès au tableau de bord
- **WHEN** l'utilisateur clique sur « Statistiques »
- **THEN** le tableau de bord s'affiche avec la période « 12 derniers mois »

#### Scenario: Accès refusé en production
- **WHEN** une requête sans JWT Cloudflare valide demande `/statistiques` en `prod`
- **THEN** la réponse est un rejet 401 ou 403

### Requirement: Période du tableau de bord
Le système SHALL permettre de choisir la période de calcul parmi 30 jours, 12 mois, année civile et tout l'historique, la valeur par défaut étant 12 mois, et SHALL appliquer cette période à tous les blocs.

#### Scenario: Changement de période
- **WHEN** l'utilisateur choisit « Année civile »
- **THEN** tous les blocs sont recalculés sur l'année en cours

#### Scenario: Période inconnue
- **WHEN** la période demandée n'est pas l'une des quatre valeurs
- **THEN** la période par défaut est utilisée

### Requirement: Chiffres clés
Le système SHALL afficher le nombre de parties de la période avec son évolution par rapport à la période précédente de même durée, le nombre de jeux distincts joués sur le total, le nombre de joueurs actifs, le nombre de jeux dont coopératifs, la valeur de la ludothèque et le prix moyen, les prêts en cours et de la période, et la série de semaines consécutives avec au moins une partie.

#### Scenario: Évolution
- **WHEN** 12 parties ont été jouées sur la période et 8 sur la précédente
- **THEN** le chiffre clé affiche 12 avec une évolution de +50 %

#### Scenario: Valeur partielle
- **WHEN** 3 jeux sur 40 n'ont pas de prix
- **THEN** la valeur ne compte que les jeux avec prix et indique « 3 jeux sans prix »

### Requirement: Statistiques d'activité
Le système SHALL afficher les parties par mois, par jour de la semaine, un calendrier de chaleur par jour, le cumul de parties et la répartition par lieu, sur les parties terminées de la période uniquement.

#### Scenario: Parties en cours ignorées
- **WHEN** une partie est en cours ou en pause
- **THEN** elle n'est comptée dans aucun diagramme

#### Scenario: Lieu absent
- **WHEN** des parties n'ont pas de lieu
- **THEN** elles sont regroupées sous « Non renseigné » dans la répartition par lieu

### Requirement: Statistiques des jeux
Le système SHALL afficher le top 10 des jeux les plus joués et les plus gagnés, les jeux jamais joués, les jeux dormants (non joués depuis le seuil configuré), la répartition coopératif/compétitif, le taux de victoire des jeux coopératifs avec le jeu le plus difficile, les variantes les plus jouées et le coût par partie des jeux ayant un prix.

#### Scenario: Jeu jamais joué
- **WHEN** un jeu n'a aucune partie terminée
- **THEN** il apparaît dans la liste des jeux jamais joués

#### Scenario: Jeu dormant
- **WHEN** le seuil est de 6 mois et le dernier jeu joué remonte à 7 mois
- **THEN** le jeu est listé comme dormant

#### Scenario: Coût par partie
- **WHEN** un jeu de 40 € a été joué 10 fois
- **THEN** son coût par partie est de 4,00 €

### Requirement: Statistiques des joueurs
Le système SHALL afficher le classement général (victoires et taux de victoire) limité aux joueurs ayant au moins le nombre minimal de parties configuré, les parties par joueur, les records de score par jeu, les duos jouant le plus ensemble et le nombre de jeux différents par joueur. Les joueurs anonymisés SHALL être comptés dans les totaux de parties mais exclus de tous les classements nominatifs.

#### Scenario: Minimum de parties
- **WHEN** le minimum est de 5 parties et un joueur n'en a joué que 3
- **THEN** il n'apparaît pas dans le classement du taux de victoire

#### Scenario: Joueur anonymisé
- **WHEN** un joueur a été anonymisé
- **THEN** ses parties restent comptées dans les totaux mais il est absent des classements

#### Scenario: Scores facultatifs
- **WHEN** un jeu n'a aucune partie avec score
- **THEN** il n'apparaît pas dans les records de score

### Requirement: Statistiques de la ludothèque
Le système SHALL afficher les achats par mois et par année, l'investissement cumulé, les jeux les plus chers et le nombre de jeux sans prix ou sans date d'achat.

#### Scenario: Date d'achat absente
- **WHEN** un jeu n'a pas de date d'achat
- **THEN** il est exclu des achats par mois et compté dans « sans date d'achat »

### Requirement: Statistiques des prêts
Le système SHALL afficher les prêts par mois, les principaux emprunteurs, la durée moyenne des prêts terminés et les prêts en cours, ceux dépassant le seuil de prêt long étant signalés.

#### Scenario: Prêt long
- **WHEN** le seuil est de 30 jours et un prêt en cours dure depuis 45 jours
- **THEN** ce prêt est signalé comme long

### Requirement: Rendu SVG et CSS côté serveur
Le système SHALL rendre les diagrammes en SVG et CSS générés par Thymeleaf, sans bibliothèque JavaScript supplémentaire, avec les couleurs de la charte, une alternative textuelle (`<title>`, `aria-label`) et un tableau de données accessible.

#### Scenario: Aucune ressource externe
- **WHEN** la page du tableau de bord est rendue
- **THEN** elle ne charge aucun script ni ressource externe supplémentaire

#### Scenario: Données vides
- **WHEN** aucune partie n'existe sur la période
- **THEN** chaque bloc affiche un message d'absence de données sans diagramme cassé

### Requirement: Cache de 10 minutes invalidé à l'écriture
Le système SHALL conserver en mémoire le résultat calculé de chaque période pendant 10 minutes, SHALL servir tous les blocs d'un même affichage à partir du même instantané, et SHALL invalider tout le cache dès qu'une donnée affectant les statistiques est créée, modifiée ou supprimée (parties, y compris fin et pause, jeux, prêts, joueurs, réglages, restauration, import MyLudo). Le premier accès après expiration ou invalidation SHALL déclencher le recalcul. Aucun bouton d'actualisation ne SHALL être proposé.

#### Scenario: Cache valide
- **WHEN** le tableau de bord est demandé deux fois en moins de 10 minutes sans écriture
- **THEN** le second affichage est servi sans nouveau calcul

#### Scenario: Expiration
- **WHEN** le dernier calcul date de plus de 10 minutes
- **THEN** la requête suivante recalcule les statistiques

#### Scenario: Invalidation à l'écriture
- **WHEN** une partie est terminée ou un jeu ajouté
- **THEN** l'affichage suivant reflète la modification

#### Scenario: Cohérence des blocs
- **WHEN** plusieurs blocs sont chargés pour la même période
- **THEN** ils proviennent du même instantané et leurs totaux sont cohérents

#### Scenario: Heure de calcul
- **WHEN** le tableau de bord s'affiche
- **THEN** il indique l'heure de la dernière mise à jour

### Requirement: Calculs sobres en ressources
Le système SHALL calculer les statistiques par requêtes agrégées sans charger de collections entières en mémoire, et SHALL limiter le nombre de requêtes par calcul à un nombre constant indépendant du volume de données.

#### Scenario: Volume
- **WHEN** la base contient plusieurs milliers de parties
- **THEN** le nombre de requêtes d'un calcul reste identique à celui d'une base réduite
