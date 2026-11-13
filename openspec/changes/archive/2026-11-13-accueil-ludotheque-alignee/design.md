## Context

La route `/` est actuellement traitée par `AccueilController` et rend `accueil.html`, une page intermédiaire sans liste de jeux. La liste paginée existe déjà sur `/jeux`; le manifeste PWA utilise `/` comme `start_url`, et le logo renvoie également à `/`. Dans la liste, la grille est centrée avec une largeur maximale de `40rem`, alors que le titre et le bouton « Ajouter un jeu » suivent la largeur plus grande de `.contenu`.

La modification reste limitée au rendu MVC et au CSS. Il n'y a pas de changement de persistance, de contrat d'API ni de configuration d'authentification.

## Goals / Non-Goals

**Goals:**
- Ouvrir la liste de la ludothèque en visitant `/`, y compris depuis le raccourci PWA et le logo.
- Aligner le titre et l'action principale au bord gauche de la grille, tout en maintenant la grille centrée et responsive.
- Garder une seule implémentation de la liste paginée.

**Non-Goals:**
- Modifier la pagination, le nombre de colonnes, l'affichage des vignettes ou leur largeur maximale.
- Modifier le `start_url`, la navigation de la ludothèque, les règles d'accès ou les données.

## Decisions

- La route `/` redirige vers `/jeux`. Cette route existante reste l'unique propriétaire du chargement paginé, des champions et du rendu de la liste. Rendre directement `jeux/liste` depuis le contrôleur d'accueil nécessiterait de dupliquer ou déplacer cette orchestration; changer le `start_url` obligerait aussi à maintenir une configuration PWA distincte.
- Le titre et le bouton d'ajout sont regroupés dans un conteneur centré de même largeur maximale que la grille (`40rem`). La grille conserve sa largeur maximale et son centrage actuels; les colonnes restent deux sur petit écran et trois à partir de `40rem`.
- Le template d'accueil intermédiaire peut être supprimé s'il n'est plus référencé après le changement de route. Le lien du logo et le `start_url` restent `/` afin de bénéficier de la redirection canonique.

## Risks / Trade-offs

- [La redirection ajoute un aller-retour HTTP à l'ouverture initiale de `/`] → La page de destination reste la route `/jeux` déjà utilisée par le lien de navigation; aucun calcul de liste n'est dupliqué.
- [Un changement de contrôleur pourrait affecter les règles d'accès en production] → La redirection reste dans la chaîne Spring MVC existante et ne modifie pas Spring Security; vérifier le comportement de la route racine dans les tests de sécurité concernés.

## Migration Plan

Aucune migration de données ou de configuration n'est nécessaire. En cas de régression, restaurer le rendu antérieur de la route `/` et le template d'accueil; le manifeste et les URLs existantes ne changent pas.

## Open Questions

Aucune.
