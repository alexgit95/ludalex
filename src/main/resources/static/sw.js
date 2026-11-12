// Cache limité aux ressources statiques ; incrémenter la version à chaque release.
const CACHE = 'ludalex-statique-0.1.0';
const PREFIXES = ['/css/', '/js/', '/icons/'];

self.addEventListener('install', function () {
    self.skipWaiting();
});

self.addEventListener('activate', function (evenement) {
    evenement.waitUntil(
        caches.keys()
            .then(function (noms) {
                return Promise.all(noms.filter(function (nom) { return nom !== CACHE; })
                    .map(function (nom) { return caches.delete(nom); }));
            })
            .then(function () { return self.clients.claim(); })
    );
});

self.addEventListener('fetch', function (evenement) {
    const requete = evenement.request;
    const url = new URL(requete.url);
    const statique = url.origin === self.location.origin
        && PREFIXES.some(function (prefixe) { return url.pathname.startsWith(prefixe); });
    if (requete.method !== 'GET' || !statique) {
        return;
    }
    evenement.respondWith(
        caches.match(requete).then(function (enCache) {
            return enCache || fetch(requete).then(function (reponse) {
                if (reponse.ok) {
                    const copie = reponse.clone();
                    caches.open(CACHE).then(function (cache) { cache.put(requete, copie); });
                }
                return reponse;
            });
        })
    );
});
