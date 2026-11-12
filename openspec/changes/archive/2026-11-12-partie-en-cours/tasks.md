## 1. Modèle

- [x] 1.1 Ajouter `termineeLe` à `Partie` et activer les statuts `EN_COURS`/`EN_PAUSE`
- [x] 1.2 Créer l'entité `TokenFinPartie` (empreinte unique, expiration, utilisé) et son dépôt (consommation atomique)
- [x] 1.3 Mettre à jour `docs/database.puml`

## 2. Cycle de vie de la partie

- [x] 2.1 Implémenter le lancement (joueurs actifs, équipes facultatives, génération du token)
- [x] 2.2 Garantir l'unicité de la partie active (verrou, refus si en cours ou en pause)
- [x] 2.3 Implémenter le flux « terminer la précédente puis lancer » côté service et interface
- [x] 2.4 Implémenter pause (joueur suivant obligatoire et participant), reprise et fin depuis l'application
- [x] 2.5 Réutiliser pour la fin les règles de gagnants de `parties` et invalider les tokens
- [x] 2.6 Tests : lancement, un seul actif, fin puis lancement, pause valide/invalide, reprise, fin compétitive/coopérative, aucun gagnant, lancements simultanés

## 3. Token et accès public

- [x] 3.1 Implémenter la génération (256 bits, SHA-256, un seul token valide par partie)
- [x] 3.2 Implémenter la consommation atomique et la réponse uniforme (inconnu, expiré, utilisé, partie terminée)
- [x] 3.3 Implémenter le contrôleur `/fin/{token}` (affichage, fin, pause) avec CSRF et en-têtes de protection
- [x] 3.4 Implémenter le filtre de limitation de débit sur `/fin/**` avec nettoyage des seaux
- [x] 3.5 Tests de sécurité : token valide, expiré, déjà utilisé, d'une autre partie, inconnu, joueur hors partie, simultanéité, 429, CSRF manquant, en-têtes, hors `/fin` en `prod`

## 4. Interface

- [x] 4.1 Créer les écrans de lancement, de pause, de reprise et de fin dans l'application
- [x] 4.2 Créer la page publique `/fin/{token}` (mobile d'abord, aucune navigation vers le reste de l'application)
- [x] 4.3 Créer les pages de confirmation et la page neutre d'erreur de token

## 5. Documentation et livraison

- [x] 5.1 Vérifier que `docs/fin-de-partie-qr.md` reste conforme (token, expiration, scores facultatifs)
- [x] 5.2 Mettre à jour `README.md` (parties en direct, pause, QR, règle de la partie unique)
- [x] 5.3 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à `0.7.0`, identique dans les deux fichiers
- [x] 5.4 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
