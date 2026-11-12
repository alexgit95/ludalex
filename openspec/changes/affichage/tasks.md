## 1. Service d'état

- [ ] 1.1 Définir les états (`EN_COURS`, `EN_PAUSE`, `VICTOIRE`, `VEILLE`) et le service qui les calcule depuis la partie active et `termineeLe`
- [ ] 1.2 Implémenter la génération et le renouvellement du token pour le QR (absent, consommé, expirant dans moins de 24 h)
- [ ] 1.3 Implémenter la détermination de l'origine publique (headers de proxy, surcharge `ludalex.url-publique`)
- [ ] 1.4 Ajouter ZXing `core` au `pom.xml` et implémenter la génération du QR en SVG
- [ ] 1.5 Tests : chaque état, fenêtre de 30 s, nouveau token à l'ouverture, renouvellement avant expiration, URL derrière proxy, défaite coopérative, plusieurs vainqueurs

## 2. Interface

- [ ] 2.1 Créer la page `/affichage` et le fragment `/affichage/etat` avec polling HTMX de 3 s
- [ ] 2.2 Créer la vue partie en cours (image, fond coloré, avatars, QR) lisible de loin
- [ ] 2.3 Créer la vue pause (joueur suivant en grand)
- [ ] 2.4 Créer la vue victoire avec animations CSS des avatars et images de victoire, 15 s
- [ ] 2.5 Ajouter le script de reprise (rechargement après trois échecs) et la demande de maintien d'écran
- [ ] 2.6 Gérer image de jeu absente et avatar absent

## 3. Diaporama

- [ ] 3.1 Énumérer les images de `static/diaporama/` au démarrage
- [ ] 3.2 Créer le fragment de veille et le script de fondu à deux couches avec préchargement d'une seule image
- [ ] 3.3 Ajouter un jeu initial d'images celtiques / village médiéval (2 Mo maximum chacune)
- [ ] 3.4 Gérer le dossier vide (fond neutre avec le nom de l'application)

## 4. Documentation et livraison

- [ ] 4.1 Mettre à jour `docs/fin-de-partie-qr.md` (renouvellement du token côté affichage, QR qui change au rechargement)
- [ ] 4.2 Mettre à jour `README.md` (écran Affichage, session Cloudflare d'un mois, diaporama)
- [ ] 4.3 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à `0.8.0`, identique dans les deux fichiers
- [ ] 4.4 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
