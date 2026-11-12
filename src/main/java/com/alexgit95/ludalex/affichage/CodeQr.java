package com.alexgit95.ludalex.affichage;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Génère un QR code en SVG : aucune image décodée, coût négligeable sur Raspberry Pi. */
@Component
public class CodeQr {

    private static final int MARGE_MODULES = 4;

    public String svg(String contenu) {
        try {
            return versSvg(encoder(contenu));
        } catch (WriterException e) {
            throw new IllegalStateException("QR code impossible à générer", e);
        }
    }

    static BitMatrix encoder(String contenu) throws WriterException {
        Map<EncodeHintType, Object> options = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 0,
                EncodeHintType.CHARACTER_SET, "UTF-8");
        return new QRCodeWriter().encode(contenu, BarcodeFormat.QR_CODE, 0, 0, options);
    }

    /** Un segment horizontal par suite de modules noirs, sur fond blanc avec zone de silence. */
    static String versSvg(BitMatrix matrice) {
        int total = matrice.getWidth() + 2 * MARGE_MODULES;
        StringBuilder chemin = new StringBuilder();
        for (int y = 0; y < matrice.getHeight(); y++) {
            int x = 0;
            while (x < matrice.getWidth()) {
                if (!matrice.get(x, y)) {
                    x++;
                    continue;
                }
                int debut = x;
                while (x < matrice.getWidth() && matrice.get(x, y)) {
                    x++;
                }
                chemin.append('M').append(debut + MARGE_MODULES).append(' ').append(y + MARGE_MODULES)
                        .append('h').append(x - debut).append("v1h-").append(x - debut).append('z');
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + total + " " + total
                + "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"QR code de fin de partie\">"
                + "<rect width=\"" + total + "\" height=\"" + total + "\" fill=\"#fff\"/>"
                + "<path d=\"" + chemin + "\" fill=\"#000\"/></svg>";
    }
}
