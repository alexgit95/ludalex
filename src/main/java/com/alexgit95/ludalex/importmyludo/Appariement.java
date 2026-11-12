package com.alexgit95.ludalex.importmyludo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Rapprochement des parties d'un export avec celles déjà en base pour un même jeu et un même jour. */
final class Appariement {

    private Appariement() {
    }

    /**
     * Pour chaque partie du fichier (joueurs identifiés par leur fiche existante), indique si elle est déjà
     * présente : une partie en base partage au moins un joueur avec elle et ne peut en absorber qu'une seule.
     * Les correspondances exactes passent d'abord, puis le meilleur recouvrement.
     */
    static boolean[] apparier(List<Set<Long>> fichier, List<Set<Long>> base) {
        boolean[] absorbee = new boolean[fichier.size()];
        boolean[] utilisee = new boolean[base.size()];
        for (int i = 0; i < fichier.size(); i++) {
            for (int b = 0; b < base.size() && !absorbee[i]; b++) {
                if (!utilisee[b] && !fichier.get(i).isEmpty() && fichier.get(i).equals(base.get(b))) {
                    absorbee[i] = true;
                    utilisee[b] = true;
                }
            }
        }
        for (int i = 0; i < fichier.size(); i++) {
            if (absorbee[i]) {
                continue;
            }
            int meilleure = -1;
            int recouvrement = 0;
            for (int b = 0; b < base.size(); b++) {
                if (utilisee[b]) {
                    continue;
                }
                int commun = communs(fichier.get(i), base.get(b));
                if (commun > recouvrement) {
                    recouvrement = commun;
                    meilleure = b;
                }
            }
            if (meilleure >= 0) {
                absorbee[i] = true;
                utilisee[meilleure] = true;
            }
        }
        return absorbee;
    }

    private static int communs(Set<Long> a, Set<Long> b) {
        List<Long> communs = new ArrayList<>(a);
        communs.retainAll(b);
        return communs.size();
    }
}
