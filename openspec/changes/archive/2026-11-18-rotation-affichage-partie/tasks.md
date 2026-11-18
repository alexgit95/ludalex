## 1. Données de l'affichage

- [x] 1.1 Ajouter des requêtes bornées et compatibles SQLite/PostgreSQL pour les cinq dernières parties terminées tous jeux et par jeu, avec participations chargées en lot.
- [x] 1.2 Fournir à l'écran les historiques et les champions du jeu en réutilisant les règles de statistiques existantes, y compris la limite visible et le compteur des égalités, l'anonymisation et les résultats coopératifs.
- [x] 1.3 Prévoir l'actualisation des historiques et champions après modification des données, sans chargement massif ni requêtes coûteuses à chaque interrogation légère.
- [x] 1.4 Tester les limites, le tri, les historiques vides, les résultats et les champions ainsi que l'actualisation et les réponses inchangées.
- [x] 1.5 Fournir les trois premiers rangs du podium par nombre de victoires, avec trois joueurs au maximum par rang et un compteur des autres ex æquo, sans charger tous les joueurs en mémoire ; exclure les joueurs anonymisés et tester les joueurs sans victoire et les rangs manquants.

## 2. Diaporama et rotation

- [x] 2.1 Filtrer les images par préfixe `portrait_` selon l'orientation de la fenêtre, gérer les changements d'orientation et le fond LUDALEX en l'absence d'image adaptée.
- [x] 2.2 Afficher la rotation de dix secondes à partir de la confrontation, en intercalant une image différente entre les vues disponibles et en sautant les historiques vides.
- [x] 2.3 Nettoyer les minuteurs et écouteurs lors des remplacements HTMX ; conserver la priorité des états de pause et de victoire et la stabilité de la rotation lors des interrogations inchangées.
- [x] 2.4 Maintenir un QR lisible et valide sur chaque vue de partie en cours, y compris le diaporama et le fond LUDALEX, et préserver sa régénération et son renouvellement.
- [x] 2.5 Adapter les vues d'historiques et de champions aux écrans portrait et paysage sans chevauchement ni contenu masqué.
- [x] 2.6 Présenter le podium dans la vue du jeu et révéler ses rangs du troisième au premier en cinq secondes maximum, sans changer les dix secondes de la vue ni masquer le QR ou les résultats.
- [x] 2.7 Afficher immédiatement le podium complet en mouvement réduit et relancer la révélation à chaque retour sur la vue ; adapter la présentation aux égalités nombreuses et aux rangs manquants.

## 3. Vérifications et documentation

- [x] 3.1 Couvrir les nouveaux comportements Java et les transitions de l'écran avec les tests existants et des tests ciblés supplémentaires.
- [x] 3.2 Vérifier dans un navigateur la séquence, les vues vides, les rotations d'orientation, le QR sur chaque vue, la pause, la fin de partie, le podium animé et réduit, les égalités et la lisibilité sur petits et grands écrans.
- [x] 3.3 Exécuter les tests Maven pertinents avec le binaire imposé par le projet et vérifier l'absence de régression de sécurité du QR.
- [x] 3.4 Mettre à jour le README racine et celui du diaporama avec le préfixe, l'ordre, les orientations, la limite des ex æquo et la nouvelle rotation.
- [x] 3.5 Incrémenter la version majeure dans le POM et le changelog, et documenter le breaking change et sa migration dans le changelog.