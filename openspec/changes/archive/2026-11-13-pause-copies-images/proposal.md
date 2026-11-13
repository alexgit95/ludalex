## Why

Le téléchargement automatique des copies d'images tourne en continu dès le démarrage, ce qui charge réseau, disque et CPU d'un Raspberry Pi sans contrôle. L'utilisateur veut décider quand la récupération s'exécute.

## What Changes

- Le **téléchargement** des copies est **en pause à chaque démarrage** de l'application (état volatil, non persisté).
- Un bouton **Activer / Mettre en pause** dans Admin > Images distantes (`/admin/images`), protégé par CSRF et le contrôle d'accès existant, avec bandeau d'état actualisé par le suivi HTMX.
- En pause, l'**inscription** des références continue (les copies sont créées « en attente », sans accès réseau) : les compteurs montrent le retard à rattraper. Aucune tentative n'est consommée et rien n'est téléchargé.
- Un téléchargement déjà commencé à la mise en pause **se termine** ; aucun nouveau n'est démarré.
- Propriété `ludalex.images.copies.demarrage-actif` (défaut `false`, variable Docker `LUDALEX_IMAGES_COPIES_DEMARRAGE_ACTIF`) pour démarrer actif.
- L'affichage des images n'est pas modifié : les copies disponibles restent servies.
- **BREAKING** : comportement fonctionnel modifié, les installations existantes ne téléchargent plus rien tant que la récupération n'est pas activée. Version majeure 4.0.0.

## Capabilities

### New Capabilities

### Modified Capabilities
- `copies-images-distantes`: la collecte inscrit toujours les copies mais ne télécharge que lorsque la récupération est active ; nouvelle exigence d'état de pause au démarrage et de commande depuis l'administration.

## Impact

- Code : nouveau composant d'état `EtatCopies`, garde dans `PlanificationCopies.traiter()`, deux actions POST dans `CopiesImagesController`, bandeau et bouton dans `admin/images.html`.
- Base, sauvegarde, API : aucun changement (état en mémoire, format ZIP v4 inchangé).
- Configuration : nouvelle propriété ; `ludalex.images.copies.actif` reste l'arrêt complet par configuration.
- Tests : les `@SpringBootTest` qui comptent sur le scheduler doivent démarrer actifs.
- Documentation : `README.md`, `CHANGELOG.md` avec `[BREAKING CHANGE]`, version 4.0.0 (`pom.xml`), `docker-compose*.yml` et `.env.example` si pertinent, cache du service worker.
