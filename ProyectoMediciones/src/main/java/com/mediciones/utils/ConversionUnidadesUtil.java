package com.mediciones.utils;

public final class ConversionUnidadesUtil {

    private ConversionUnidadesUtil() {
        // Clase utilitaria
    }

    public enum UnidadPresion {
        PSIG,
        BARG,
        KGCM2
    }

    public static double convertirPresion(
            double valor,
            UnidadPresion unidadOrigen,
            UnidadPresion unidadDestino) {

        if (unidadOrigen == unidadDestino) {
            return valor;
        }

        // Primero convertimos todo a BARG
        double valorEnBarg;

        switch (unidadOrigen) {
            case BARG:
                valorEnBarg = valor;
                break;

            case PSIG:
                valorEnBarg = valor / 14.5038;
                break;

            case KGCM2:
                valorEnBarg = valor / 1.01972;
                break;

            default:
                throw new IllegalArgumentException(
                        "Unidad de presión origen no soportada: " + unidadOrigen);
        }

        // De BARG a la unidad destino
        switch (unidadDestino) {
            case BARG:
                return valorEnBarg;

            case PSIG:
                return valorEnBarg * 14.5038;

            case KGCM2:
                return valorEnBarg * 1.01972;

            default:
                throw new IllegalArgumentException(
                        "Unidad de presión destino no soportada: " + unidadDestino);
        }
    }
}