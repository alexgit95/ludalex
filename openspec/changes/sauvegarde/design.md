## Context

Données répartis dans `joueur`, `jeu`, `pret`, `partie`, `participation`, `equipe` et un volume d'images. Le schéma est généré par Hibernate (pas de migration) et doit être restaurable d'un moteur à l'autre (SQLite ↔ PostgreSQL). Contraintes Raspberry Pi : mémoire bornée, traitement en flux. Décision projet : un seul ZIP, ré-uploadable sans modification, restauration par remplacement complet, tout inclus (données et images uploadées).

## Goals / Non-Goals

**Goals:**
- Export et restauration en flux, portables entre moteurs.
- Restauration sûre : validation préalable, atomique, ZIP hostile refusé.

**Non-Goals:**
- Sauvegardes planifiées ou distantes.
- Restauration partielle ou fusion.
- Sauvegarde des tokens de fin de partie ou de la configuration.

## Decisions

- **Format du ZIP** :
  ```
  manifest.json          { formatVersion, appVersion, date, compteurs }
  donnees/joueurs.json   tableaux d'objets (un objet par enregistrement)
  donnees/jeux.json
  donnees/prets.json
  donnees/parties.json
  donnees/participations.json
  donnees/equipes.json
  images/<nom-de-fichier>
  ```
  Entrées dans cet ordre (manifeste d'abord). `formatVersion` démarre à `1`.
- **JSON indépendant du moteur** : identifiants d'origine conservés comme **références** uniquement (`idSauvegarde`) ; dates en ISO-8601, décimaux en chaînes, booléens JSON. À l'import, de nouveaux identifiants sont générés et une table de correspondance reconstruit les relations : aucun problème de séquence PostgreSQL ou d'auto-incrément SQLite.
- **Export en flux** : `ZipOutputStream` sur la réponse HTTP ; chaque fichier JSON écrit avec le générateur de flux Jackson ; lecture par lots (`Pageable` de 500, vidage du contexte de persistance entre lots) ; images copiées par flux depuis le volume. Mémoire bornée.
- **Restauration en deux passes** sur un fichier temporaire :
  1. **Validation** (`ZipFile` sur le fichier temporaire, accès aléatoire sans tout charger) : manifeste et version, entrées autorisées (`manifest.json`, `donnees/*.json`, `images/*`), nom normalisé sans `..`, nombre d'entrées (≤ 10 000), taille décompressée totale (≤ 1 Go) et ratio par entrée, images ≤ 2 Mo avec signature PNG/JPEG/GIF/WebP, cohérence des références, présence des fichiers référencés, compteurs du manifeste.
  2. **Application** : une transaction unique ; suppression dans l'ordre des dépendances ; insertion par lots ; images extraites dans un répertoire de préparation ; après commit, échange atomique du répertoire du volume (déplacement du répertoire de préparation, ancien répertoire supprimé). En cas d'échec : rollback et suppression du répertoire de préparation.
- **Confirmation** : l'utilisateur doit saisir un mot de confirmation (« RESTAURER ») et un jeton CSRF valide est exigé ; la page rappelle que tout sera remplacé.
- **Limite d'upload spécifique** : la limite globale de 2 Mo des images ne s'applique pas à cette route ; plafond configurable (`ludalex.sauvegarde.taille-max`, défaut 512 Mo) avec écriture en flux vers un fichier temporaire.
- **Tokens** : table `token_fin_partie` vidée à la restauration et non exportée.
- **Verrou** : une seule restauration à la fois (verrou en mémoire) ; l'application reste disponible mais les écritures concurrentes peuvent être refusées pendant l'import.
- **Suivi des fichiers orphelins** : à la fin d'une restauration, le volume ne contient que les images référencées par les données importées.
- **Pas d'API REST** : pages MVC ; le téléchargement est un `GET` renvoyant un flux (pas d'annotations OpenAPI car rendu HTML + téléchargement binaire géré par contrôleur MVC).

## Risks / Trade-offs

- [Échange du volume non atomique sur certains systèmes de fichiers] → préparation sur le même volume, `Files.move` atomique quand disponible, ordre garanti : base d'abord, fichiers ensuite, nettoyage des restes au prochain démarrage.
- [Mémoire de la JVM limitée par `-Xmx`] → flux, lots de 500, aucun `readAllBytes`.
- [Taille d'un ZIP volumineux sur la carte SD] → fichier temporaire supprimé en fin de traitement ; contrôle de l'espace disponible avant restauration.
- [Format évolutif] → `formatVersion` ; refus explicite d'une version inconnue ; règle de migration écrite à chaque changement (breaking change au besoin).
- [Données ajoutées par les évolutions futures] → toute nouvelle entité doit être ajoutée à l'export (test d'exhaustivité qui compare les entités JPA aux fichiers exportés).

## Open Questions

- Seuils exacts de taille du ZIP et du volume (512 Mo / 1 Go proposés) à confirmer selon l'espace disponible sur le Pi.
