(function () {
    var ECHECS_MAX = 3;
    var echecs = 0;
    var minuteur = null;
    var zoneCourante = null;
    var changerOrientation = null;

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

    function demarrerAffichage() {
        var zone = document.getElementById('zone-affichage');
        if (zone === zoneCourante) {
            return;
        }
        zoneCourante = zone;
        if (minuteur !== null) {
            clearInterval(minuteur);
            minuteur = null;
        }
        changerOrientation = null;
        var conteneur = document.getElementById('diaporama');
        if (!conteneur) {
            return;
        }
        var images = (conteneur.dataset.images || '').split('|').filter(Boolean);
        var couches = conteneur.querySelectorAll('.couche');
        var vide = conteneur.querySelector('.veille-vide');
        var indexes = { portrait: null, paysage: null };
        var active = 0;
        var suivante = null;
        var rotation = document.getElementById('rotation');
        var vues = rotation ? Array.from(rotation.querySelectorAll('[data-vue]')) : [];
        var ordre = rotation ? ['combat', 'image'] : [];
        if (rotation && rotation.querySelector('[data-vue="jeu"]')) {
            ordre.push('jeu', 'image');
        }
        if (rotation && rotation.querySelector('[data-vue="global"]')) {
            ordre.push('global', 'image');
        }
        var position = 0;

        function afficherImage(avancer, changement) {
            var mode = window.matchMedia('(orientation: portrait)').matches ? 'portrait' : 'paysage';
            var selection = images.filter(function (url) {
                var nom = url.substring(url.lastIndexOf('/') + 1);
                return (nom.indexOf('portrait_') === 0) === (mode === 'portrait');
            });
            if (changement) {
                couches.forEach(function (couche) {
                    couche.classList.remove('visible');
                    couche.style.backgroundImage = '';
                });
            }
            vide.hidden = selection.length > 0;
            if (selection.length === 0) {
                couches.forEach(function (couche) { couche.classList.remove('visible'); });
                suivante = null;
                return;
            }
            if (indexes[mode] === null) {
                indexes[mode] = Math.floor(Math.random() * selection.length);
            } else if (avancer) {
                indexes[mode] = (indexes[mode] + 1) % selection.length;
            }
            var cible = 1 - active;
            couches[cible].style.backgroundImage = 'url("' + selection[indexes[mode]] + '")';
            couches[cible].classList.add('visible');
            couches[active].classList.remove('visible');
            active = cible;
            suivante = new Image();
            suivante.src = selection[(indexes[mode] + 1) % selection.length];
        }

        function afficherVue(nom) {
            vues.forEach(function (vue) {
                vue.hidden = vue.dataset.vue !== nom;
                vue.classList.toggle('revele-podium', !vue.hidden && nom === 'jeu');
            });
            if (nom === 'image') {
                afficherImage(true, false);
            }
        }

        changerOrientation = function () {
            if (!rotation || ordre[position] === 'image') {
                afficherImage(false, true);
            } else {
                couches.forEach(function (couche) {
                    couche.classList.remove('visible');
                    couche.style.backgroundImage = '';
                });
                suivante = null;
            }
        };
        if (!rotation) {
            afficherImage(false, false);
        }
        minuteur = setInterval(function () {
            if (rotation) {
                position = (position + 1) % ordre.length;
                afficherVue(ordre[position]);
            } else {
                afficherImage(true, false);
            }
        }, 10000);
    }

    document.addEventListener('DOMContentLoaded', demarrerAffichage);
    document.addEventListener('htmx:afterSettle', demarrerAffichage);
    window.matchMedia('(orientation: portrait)').addEventListener('change', function () {
        if (changerOrientation) {
            changerOrientation();
        }
    });

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
