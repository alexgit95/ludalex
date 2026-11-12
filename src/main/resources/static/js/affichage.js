(function () {
    var ECHECS_MAX = 3;
    var echecs = 0;
    var minuteur = null;

    // Session Cloudflare expirée ou réseau coupé : on recharge la page entière pour que Cloudflare redemande la connexion
    document.addEventListener('htmx:afterRequest', function (evenement) {
        if (evenement.detail.successful) {
            echecs = 0;
            return;
        }
        echecs += 1;
        if (echecs >= ECHECS_MAX) {
            window.location.reload();
        }
    });

    // Diaporama : deux couches en fondu, une seule image préchargée, durée fixe par image
    function demarrerDiaporama() {
        if (minuteur !== null) {
            clearInterval(minuteur);
            minuteur = null;
        }
        var conteneur = document.getElementById('diaporama');
        if (!conteneur || !conteneur.dataset.images) {
            return;
        }
        var images = conteneur.dataset.images.split('|').filter(Boolean);
        var couches = conteneur.querySelectorAll('.couche');
        if (images.length === 0 || couches.length < 2) {
            return;
        }
        var index = Math.floor(Math.random() * images.length);
        var active = 0;
        couches[0].style.backgroundImage = 'url("' + images[index] + '")';
        couches[0].classList.add('visible');
        var suivante = new Image();
        suivante.src = images[(index + 1) % images.length];
        if (images.length === 1) {
            return;
        }
        minuteur = setInterval(function () {
            index = (index + 1) % images.length;
            var cible = 1 - active;
            couches[cible].style.backgroundImage = 'url("' + images[index] + '")';
            couches[cible].classList.add('visible');
            couches[active].classList.remove('visible');
            active = cible;
            suivante = new Image();
            suivante.src = images[(index + 1) % images.length];
        }, 10000);
    }

    document.addEventListener('DOMContentLoaded', demarrerDiaporama);
    document.addEventListener('htmx:afterSettle', demarrerDiaporama);

    // Écran maintenu allumé quand le navigateur le permet (HTTPS)
    var verrou = null;
    function garderAllume() {
        if (!('wakeLock' in navigator)) {
            return;
        }
        navigator.wakeLock.request('screen').then(function (obtenu) { verrou = obtenu; }).catch(function () { /* facultatif */ });
    }
    document.addEventListener('DOMContentLoaded', garderAllume);
    document.addEventListener('visibilitychange', function () {
        if (document.visibilityState === 'visible' && (verrou === null || verrou.released)) {
            garderAllume();
        }
    });
})();
