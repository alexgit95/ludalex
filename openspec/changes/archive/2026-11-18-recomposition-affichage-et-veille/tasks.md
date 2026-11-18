## 1. Données et QR

- [x] 1.1 Exposer les cinq dernières parties globales en veille et en partie, avec participations et images de secours chargées en lots ; conserver le tri et les requêtes bornées.
- [x] 1.2 Actualiser la galerie en veille après ajout, modification ou suppression de données sans recalcul à chaque interrogation ni réinitialisation lorsque l'état est inchangé.
- [x] 1.3 Générer un QR de lancement distinct et accessible pour l'origine publique et `/parties/direct`, sans nouveau token ni commande de lancement par GET.
- [x] 1.4 Tester les données vides, le tri, les jeux répétés, les résultats coopératifs, les égalités, l'actualisation et la stabilité du QR de fin.

## 2. Présentation et rotation

- [x] 2.1 Réorganiser la vue du jeu en portrait ou carré : podium seul en haut, trois dernières parties puis QR de fin ; conserver cinq parties et la disposition existante en paysage.
- [x] 2.2 Afficher les scores du podium au format `nV` avec alternative accessible et ajouter les lauriers au premier rang, sans modifier l'animation ni les limites des ex æquo.
- [x] 2.3 Créer une galerie de médaillons réutilisée en veille et en partie, montrant les cinq derniers résultats, les images des jeux, les dates et les gagnants, avec défaite coopérative explicite et replis d'image.
- [x] 2.4 Insérer la galerie en veille après deux à cinq affichages d'image tirés aléatoirement, dix secondes par vue ; ne pas afficher de galerie vide.
- [x] 2.5 Faire apparaître le QR de lancement sur la première image de veille puis une image sur deux, sans QR sur la galerie, y compris avec une image unique ou le fond LUDALEX.
- [x] 2.6 Préserver les compteurs lors d'un changement d'orientation, nettoyer les minuteurs et préchargements lors des remplacements HTMX, et conserver les priorités de pause et de victoire.

## 3. Vérifications et documentation

- [x] 3.1 Ajouter ou adapter les tests Java et les tests des scénarios de rotation, d'images, de podium, de galerie et d'accessibilité.
- [x] 3.2 Tester Cloudflare Access en production, l'accès libre en développement, le refus des lancements sans CSRF et l'absence de lancement par simple scan.
- [x] 3.3 Vérifier dans un navigateur les vues portrait étroit, portrait large, carré et paysage, les ex æquo et noms longs, les deux QR, les replis d'image et le mouvement réduit.
- [x] 3.4 Vérifier les changements d'orientation, les bornes aléatoires de deux à cinq images, l'alternance indépendante du QR, les états vides et les transitions de partie sans fuite de minuteurs.
- [x] 3.5 Exécuter les tests Maven pertinents avec le binaire imposé par le projet et vérifier la sobriété des requêtes et de la mémoire.
- [x] 3.6 Mettre à jour le README racine et, si nécessaire, la documentation des images et de l'origine publique des QR.
- [x] 3.7 Incrémenter la version majeure dans le POM et le changelog, renouveler le cache PWA et documenter les breaking changes et leur migration.