# Images du diaporama

Ces images sont affichées en boucle sur l'écran `/affichage` en veille et entre les vues d'une partie en cours. En veille, la galerie des dernières parties apparaît après 2 à 5 images ; un QR de lancement est visible une image sur deux.

## Ajouter ou remplacer des images

1. Déposez vos fichiers directement dans ce dossier. Les formats acceptés sont `jpg`, `jpeg`, `png`, `webp`, `gif` et `svg`.
2. Prévoyez des images de 2 Mo maximum chacune pour préserver les ressources du Raspberry Pi.
3. Pour n'afficher que vos images, retirez les illustrations existantes de ce dossier.
4. Reconstruisez et redémarrez l'application pour intégrer les changements. Avec Docker Compose depuis le dossier du projet : `docker compose up -d --build app`.

Les images sont détectées automatiquement au démarrage ; aucune modification du code n'est nécessaire. L'image Docker Hub préconstruite utilisée par Portainer ne contient pas vos fichiers locaux : il faut construire et déployer une image personnalisée pour les inclure. Il n'y a pas d'ajout depuis l'interface d'administration.

Les noms commençant exactement par `portrait_` sont utilisés en portrait (ou pour une fenêtre carrée) ; les autres sont réservés au paysage. L'écran s'adapte aux changements d'orientation sans rechargement. Si aucune image ne correspond à l'orientation, il affiche LUDALEX sur un fond neutre.

## Ordre d'affichage

Les fichiers sont triés par nom, puis affichés dans cet ordre en boucle, avec un changement toutes les 10 secondes. Pour maîtriser leur succession, ajoutez un préfixe numérique de largeur fixe, par exemple :

```text
01-chateau.jpg
02-village.png
03-foret.webp
portrait_01-chateau.jpg
portrait_02-village.png
```

Chaque orientation possède son propre ordre. Renommez ou retirez également les images existantes selon l'ordre souhaité. Le tri est sensible à la casse ; privilégiez des noms simples en minuscules. La première image est choisie aléatoirement : la numérotation fixe la succession, mais pas l'image de départ.