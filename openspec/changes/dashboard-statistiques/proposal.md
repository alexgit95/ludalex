## Why

Les statistiques existantes sont limitées à la fiche d'un jeu. Il manque une vue globale (activité, jeux, joueurs, ludothèque, prêts) sous forme de tableau de bord, cohérente avec la charte graphique et légère pour un Raspberry Pi.

## What Changes

- Nouvel onglet « Statistiques » dans le menu principal (`/statistiques`) : tableau de bord de chiffres clés et de diagrammes **SVG/CSS rendus côté serveur** (Thymeleaf), sans dépendance JavaScript supplémentaire.
- Filtre de période : 30 jours, 12 mois (défaut), année civile, tout. Blocs chargés en HTMX (`hx-trigger="load"`) à partir d'un même instantané.
- Contenu limité aux données déjà disponibles :
  - chiffres clés (parties, jeux joués, joueurs actifs, ludothèque, valeur, prêts, série de parties consécutives) ;
  - activité (parties par mois, par jour de semaine, calendrier de chaleur, cumul, lieux) ;
  - jeux (plus joués, plus gagnés, jamais joués, dormants, coopératif/compétitif, taux de victoire coopératif, variantes, coût par partie) ;
  - joueurs (classement, parties, records de score, duos, taux de victoire, polyvalence) ;
  - ludothèque (achats par mois/année, investissement cumulé, jeux les plus chers, qualité des données) ;
  - prêts (par mois, emprunteurs, durée moyenne, prêts longs et en cours).
- Exclus faute de données : durée de partie, heure de jeu, retards de prêt.
- Cache en mémoire de 10 minutes par période, **invalidé à toute écriture** (parties, jeux, prêts, joueurs, restauration, import MyLudo) ; aucun bouton d'actualisation ; le premier accès après expiration paie le calcul.
- Réglages des seuils (jeu dormant : 6 mois, classement : 5 parties minimum, prêt long : 30 jours) modifiables depuis l'administration, persistés dans une nouvelle table générique de réglages, et **inclus dans la sauvegarde et la restauration** (ainsi que tout réglage futur).
- Les joueurs anonymisés comptent dans les totaux mais sont exclus des classements nominatifs.
- **BREAKING** : la sauvegarde passe au format v3 (ajout des réglages) ; un ZIP v3 n'est pas restaurable par les versions précédentes. Version majeure 2.0.0.

## Capabilities

### New Capabilities
- `dashboard-statistiques`: onglet de tableau de bord global, périodes, blocs de statistiques, rendu SVG/CSS, cache de 10 minutes invalidé à l'écriture.
- `reglages-application`: réglages persistés (clé/valeur) modifiables depuis l'administration, dont les seuils des statistiques.

### Modified Capabilities
- `sauvegarde-restauration`: le ZIP v3 exporte et restaure l'ensemble des réglages ; la restauration accepte toujours les formats v1 et v2 (réglages par défaut).
- `interface-pwa`: nouvel onglet « Statistiques » dans la navigation principale.

## Impact

- Code : nouveaux composants `statistiques` (requêtes agrégées, cache, contrôleur, fragments SVG), module de réglages, appels d'invalidation dans les services de parties, jeux, prêts, joueurs, restauration et import.
- Base : nouvelle table `reglage` (compatible SQLite et PostgreSQL), `docs/database.puml` mis à jour ; index éventuels sur `partie(date_partie)`.
- Sauvegarde : format v3, exhaustivité des entités exportées étendue aux réglages.
- Dépendances : aucune.
- Documentation : `README.md`, `CHANGELOG.md` avec rubrique `[BREAKING CHANGE]`, version 2.0.0 dans le `pom.xml` et le changelog ; cache du service worker si les ressources statiques changent.
