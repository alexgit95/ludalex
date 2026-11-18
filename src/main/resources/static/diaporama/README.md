# Images du diaporama

Ces images sont affichées en boucle sur l'écran `/affichage` lorsqu'aucune partie n'est en cours ou en pause.

## Ajouter ou remplacer des images

1. Déposez vos fichiers directement dans ce dossier. Les formats acceptés sont `jpg`, `jpeg`, `png`, `webp`, `gif` et `svg`.
2. Prévoyez des images de 2 Mo maximum chacune pour préserver les ressources du Raspberry Pi.
3. Pour n'afficher que vos images, retirez les illustrations existantes de ce dossier.
4. Reconstruisez et redémarrez l'application pour intégrer les changements. Avec Docker Compose depuis le dossier du projet : `docker compose up -d --build app`.

Les images sont détectées automatiquement au démarrage ; aucune modification du code n'est nécessaire. L'image Docker Hub préconstruite utilisée par Portainer ne contient pas vos fichiers locaux : il faut construire et déployer une image personnalisée pour les inclure. Il n'y a pas d'ajout depuis l'interface d'administration.

Si aucune image n'est présente, l'écran affiche le nom LUDALEX sur un fond neutre.

## Ordre d'affichage

Les fichiers sont triés par nom, puis affichés dans cet ordre en boucle, avec un changement toutes les 10 secondes. Pour maîtriser leur succession, ajoutez un préfixe numérique de largeur fixe, par exemple :

```text
01-chateau.jpg
02-village.png
03-foret.webp
```

Renommez ou retirez également les images existantes selon l'ordre souhaité. Le tri est sensible à la casse ; privilégiez des noms simples en minuscules. La première image est choisie aléatoirement à chaque démarrage du diaporama : la numérotation fixe la succession, mais pas l'image de départ.