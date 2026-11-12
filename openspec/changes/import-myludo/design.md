## Context

Fonction de la **v2**. L'utilisateur utilise MyLudo et souhaite en importer l'historique. Le format exact de l'export MyLudo n'est pas connu à ce stade : il doit être vérifié sur un export réel avant d'implémenter. Les entités cibles (`Jeu`, `Joueur`, `Partie`, `Participation`) existent déjà.

## Goals / Non-Goals

**Goals:**
- Importer jeux, joueurs et parties avec aperçu, rapprochement et rapport.
- Import rejouable sans doublon.

**Non-Goals:**
- Synchronisation continue avec MyLudo.
- Export vers MyLudo.
- Import des images à partir de MyLudo (seule une URL d'image éventuelle est reprise, jamais copiée).

## Decisions

- **Analyse en deux temps** : lecture en flux de l'export vers un modèle intermédiaire léger (`ImportPlan`), aperçu, puis application. Le format de l'export sera isolé dans un lecteur remplaçable (`LecteurExportMyLudo`) pour absorber un format différent.
- **Rapprochement par nom normalisé** (insensible à la casse, accents et espaces) pour jeux et joueurs ; choix de l'utilisateur à l'aperçu en cas d'ambiguïté.
- **Idempotence** : clé fonctionnelle (jeu, date, ensemble des joueurs) ; une partie déjà présente est ignorée.
- **Atomicité** : une transaction unique pour l'application, rapport (créés / ignorés / erreurs) conservé le temps de la page.
- **Images par URL** : si l'export fournit une URL d'image, elle suit les règles de `images` (protection SSRF, couleur de fond) ; une image inaccessible laisse la couleur neutre.
- **Sécurité** : limite de taille d'upload, traitement en flux, parseur XML sans entités externes si le format est XML, protections d'archive si le format est un ZIP.

## Risks / Trade-offs

- [Format MyLudo inconnu ou instable] → lecteur isolé, change à conception détaillée après examen d'un export réel.
- [Faux rapprochements de noms] → validation par l'utilisateur à l'aperçu.
- [Volume important] → traitement en flux, insertion par lots.

## Open Questions

- Format précis et contenu de l'export MyLudo (jeux, parties, scores, lieux, joueurs) à examiner sur un export réel avant de lancer ce change.
