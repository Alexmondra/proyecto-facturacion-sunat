package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.xml.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumeroALetrasUtil {

    private static final String[] UNIDADES = {
            "", "UN ", "DOS ", "TRES ", "CUATRO ", "CINCO ", "SEIS ", "SIETE ", "OCHO ", "NUEVE "
    };

    private static final String[] DECENAS = {
            "DIEZ ", "ONCE ", "DOCE ", "TRECE ", "CATORCE ", "QUINCE ", "DIECISEIS ", "DIECISIETE ", "DIECIOCHO ", "DIECINUEVE ",
            "VEINTE ", "VEINTIUN ", "VEINTIDOS ", "VEINTITRES ", "VEINTICUATRO ", "VEINTICINCO ", "VEINTISEIS ", "VEINTISIETE ", "VEINTIOCHO ", "VEINTINUEVE "
    };

    private static final String[] DIEZ_DECENAS = {
            "", "DIEZ ", "VEINTE ", "TREINTA ", "CUARENTA ", "CINCUENTA ", "SESENTA ", "SETENTA ", "OCHENTA ", "NOVENTA "
    };

    private static final String[] CENTENAS = {
            "", "CIENTO ", "DOSCIENTOS ", "TRESCIENTOS ", "CUATROCIENTOS ", "QUINIENTOS ", "SEISCIENTOS ", "SETECIENTOS ", "OCHOCIENTOS ", "NOVECIENTOS "
    };

    public static String convertir(BigDecimal monto, String moneda) {
        if (monto == null) {
            return "";
        }

        BigDecimal montoRedondeado = monto.setScale(2, RoundingMode.HALF_UP);
        long parteEntera = montoRedondeado.longValue();
        int centimos = montoRedondeado.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).intValue();

        String monedaNombre = "SOLES";
        if ("USD".equalsIgnoreCase(moneda)) {
            monedaNombre = "DOLARES AMERICANOS";
        } else if ("EUR".equalsIgnoreCase(moneda)) {
            monedaNombre = "EUROS";
        }

        if (parteEntera == 0) {
            return String.format("CERO Y %02d/100 %s", centimos, monedaNombre);
        }

        String letras = convertirNumero(parteEntera).trim();
        return String.format("%s Y %02d/100 %s", letras, centimos, monedaNombre);
    }

    private static String convertirNumero(long n) {
        if (n == 100) return "CIEN ";
        if (n < 10) return UNIDADES[(int) n];
        if (n < 30) return DECENAS[(int) (n - 10)];
        if (n < 100) {
            int d = (int) (n / 10);
            int u = (int) (n % 10);
            return DIEZ_DECENAS[d] + (u > 0 ? "Y " + UNIDADES[u] : "");
        }
        if (n < 1000) {
            int c = (int) (n / 100);
            long resto = n % 100;
            return CENTENAS[c] + convertirNumero(resto);
        }
        if (n < 1000000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String strMiles = miles == 1 ? "MIL " : convertirNumero(miles) + "MIL ";
            return strMiles + convertirNumero(resto);
        }
        if (n < 1000000000000L) {
            long millones = n / 1000000;
            long resto = n % 1000000;
            String strMillones = millones == 1 ? "UN MILLON " : convertirNumero(millones) + "MILLONES ";
            return strMillones + convertirNumero(resto);
        }
        return String.valueOf(n);
    }
}
