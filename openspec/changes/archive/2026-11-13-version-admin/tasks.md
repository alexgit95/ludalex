## 1. Affichage de la version

- [x] 1.1 Exposer depuis `AdminController` la version Maven, suivie des 7 premiers caractères de `git.commit` uniquement si ce SHA est connu; afficher la version seule si les métadonnées sont absentes ou inconnues.
- [x] 1.2 Afficher la valeur dans `admin/index.html` et tester les cas SHA connu, `unknown`/absent et SHA de moins de 7 caractères.

## 2. Documentation et version

- [x] 2.1 Mettre à jour le README, ajouter l'entrée `[Added]` au changelog, incrémenter à l'identique la version du `pom.xml` et du changelog, puis aligner la version du cache dans `sw.js`.
- [x] 2.2 Exécuter les tests ciblés de l'administration puis la suite Maven avec le binaire `C:\USINE_LOGICIELLE\apache-maven\bin`.
