# Fin de partie par QR code

Permet de valider le résultat d'une partie depuis l'appareil de n'importe qui, sans passer par Cloudflare Access.

## Principe

1. L'écran Affichage (protégé par Cloudflare Access) montre un QR code encodant `https://<domaine>/fin/<token>`.
2. Un invité scanne le QR avec son téléphone et arrive sur une page minimale.
3. Il indique que la partie est terminée et désigne le(s) gagnant(s), ou met la partie en pause en indiquant à qui c'était de jouer.
4. Le token est consommé : il ne fonctionne plus ensuite.
5. Si l'écran Affichage est ouvert, l'animation de victoire se déclenche.

```
Écran Affichage ──(QR)──► Téléphone invité ──► Cloudflare (Bypass /fin/*) ──► Application
   (JWT requis)                                                       valide le token, pas de JWT
```

## Règles du token

| Règle | Valeur |
|---|---|
| Génération | Aléatoire, au moins 128 bits, créé au lancement de la partie |
| Stockage | Haché en base (le clair n'existe que dans le QR) |
| Portée | Une seule partie |
| Usage | **Unique** : invalidé dès la première validation |
| Expiration | 7 jours |
| Droits | Désigner un ou plusieurs gagnants, renseigner les scores (facultatif) ou mettre en pause, uniquement parmi les joueurs de la partie |
| Données exposées | Aucune autre que la partie concernée |
| Protection | Limitation de débit, CSRF sur le formulaire, réponse identique pour un token inconnu, expiré ou utilisé |

Une mise en pause consomme aussi le token ; un nouveau token est généré à la reprise de la partie.

## À faire côté Cloudflare

Dans Zero Trust, application Access de Ludalex :

1. Conserver la politique **Allow** actuelle sur toute l'application (propriétaire uniquement).
2. Ajouter une seconde application (ou un chemin) ciblant **uniquement** `<domaine>/fin/*`.
3. Lui associer une politique **Bypass** (Include : Everyone).
4. Vérifier que Bypass ne couvre aucun autre chemin (pas de `/*`, pas de `/api`).
5. Tester : sans session, `/fin/<token>` répond ; `/` redirige vers la connexion Cloudflare.

Le Bypass désactive la protection Cloudflare sur ce chemin : la sécurité repose alors entièrement sur le token et le contrôle de débit côté application. Il est conseillé d'ajouter une règle de limitation de débit Cloudflare sur `/fin/*`.

## À faire côté application

- En `prod`, le filtre de validation du JWT ignore uniquement le chemin `/fin/**` ; tout le reste exige un JWT valide.
- En `dev`, aucune authentification n'est requise.
- Tests : token valide, expiré, déjà utilisé, d'une autre partie, inconnu ; JWT toujours exigé hors `/fin/**`.
