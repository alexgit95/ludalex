## Context

Le plugin Spring Boot génère déjà `BuildProperties` à partir de la version Maven et de la propriété `git.commit`. La CI transmet le SHA complet au build Docker via `GIT_COMMIT`; en exécution locale, cette propriété vaut par défaut `unknown`. La page `/admin` est une page Thymeleaf rendue par `AdminController` et son gabarit `admin/index.html`.

## Goals / Non-Goals

**Goals:**
- Afficher la version du POM dans la page d'administration.
- Ajouter les sept premiers caractères du SHA lorsque le commit est disponible.
- Afficher seulement la version lorsque le SHA est absent, vide ou égal à `unknown`.

**Non-Goals:**
- Exécuter Git à l'exécution ou ajouter un plugin de génération de version.
- Modifier la chaîne CI, les règles d'accès à l'administration ou les données persistées.

## Decisions

- Lire les métadonnées via `BuildProperties`, déjà généré par Maven, plutôt que parser le manifeste du JAR ou appeler une commande Git. Cela reprend l'usage existant de `ObjectProvider<BuildProperties>` dans `SauvegardeExport` et n'ajoute pas de dépendance.
- Construire une valeur unique destinée au modèle Thymeleaf: `<version> - <7 caractères du commit>` si le commit est disponible, sinon `<version>`. Le SHA est abrégé côté serveur; la vue n'implémente pas de logique de formatage.
- Garder la propriété par défaut `git.commit=unknown`. En local sans SHA, la page affiche donc uniquement la version; en CI, le SHA injecté par le workflow devient visible sous forme courte.

## Risks / Trade-offs

- [Les tests ou certaines exécutions peuvent ne pas disposer de `BuildProperties`] → Utiliser le fournisseur optionnel existant et fournir un repli explicite pour l'absence de métadonnées.
- [Un SHA de moins de sept caractères peut être fourni] → Afficher au plus sept caractères disponibles, sans erreur.

## Migration Plan

Aucune migration ni modification de configuration n'est nécessaire. Le build-info Maven et l'injection `GIT_COMMIT` de la CI sont déjà en place. En cas de régression visuelle, retirer l'affichage du modèle et du gabarit sans effet sur les autres pages.

## Open Questions

Aucune.
