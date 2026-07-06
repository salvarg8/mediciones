package com.mediciones.model;

import java.util.Date;
import java.util.Objects;

/**
 * Clase que representa una calibración de sensor.
 */
public class SensorCalibracion {

    // Constantes para nombres de campos (útil para serialización o logs)
    public static final String FIELD_ID = "id";
    public static final String FIELD_A1 = "a1";
    public static final String FIELD_C1 = "c1";
    public static final String FIELD_A2 = "a2";
    public static final String FIELD_C2 = "c2";
    public static final String FIELD_A3 = "a3"; // Nuevo
    public static final String FIELD_C3 = "c3"; // Nuevo
    public static final String FIELD_PRESION_CONOCIDA = "presionConocida";
    public static final String FIELD_VOLTAGE_CONOCIDO = "voltajeConocido";
    public static final String FIELD_FECHA_CALIBRACION = "fechaCalibracion";
    public static final String FIELD_SENSOR_TYPE = "sensorType";

    private Long id; // Identificador único
    private Double a1; // Constante a1 para CS-PT1200 (puede ser null)
    private Double c1; // Constante c1 para CS-PT1200 (puede ser null)
    private Double a2; // Constante a2 para Endress-Hauser (puede ser null)
    private Double c2; // Constante c2 para Endress-Hauser (puede ser null)
    private Double a3; // Constante a3 para LM35 (puede ser null)
    private Double c3; // Constante c3 para LM35 (puede ser null)
    private Double presionConocida; // Presión conocida durante la calibración (puede ser null)
    private Double voltajeConocido; // Voltaje conocido durante la calibración (puede ser null)
    private Date fechaCalibracion; // Fecha de la calibración
    private String sensorType; // Tipo de sensor

    /**
     * Constructor vacío.
     */
    public SensorCalibracion() {}

    /**
     * Constructor con parámetros.
     */
    public SensorCalibracion(Double a1, Double c1, Double a2, Double c2, Double a3, Double c3, Double presionConocida, Double voltajeConocido, Date fechaCalibracion, String sensorType) {
        this.a1 = a1;
        this.c1 = c1;
        this.a2 = a2;
        this.c2 = c2;
        this.a3 = a3;
        this.c3 = c3;
        setPresionConocida(presionConocida); // Usa setter para validación
        setVoltajeConocido(voltajeConocido); // Usa setter para validación
        this.fechaCalibracion = fechaCalibracion;
        this.sensorType = Objects.requireNonNull(sensorType, "El tipo de sensor no puede ser nulo");
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getA1() {
        return a1;
    }

    public void setA1(Double a1) {
        this.a1 = a1;
    }

    public Double getC1() {
        return c1;
    }

    public void setC1(Double c1) {
        this.c1 = c1;
    }

    public Double getA2() {
        return a2;
    }

    public void setA2(Double a2) {
        this.a2 = a2;
    }

    public Double getC2() {
        return c2;
    }

    public void setC2(Double c2) {
        this.c2 = c2;
    }

    public Double getA3() {
        return a3;
    }

    public void setA3(Double a3) {
        this.a3 = a3;
    }

    public Double getC3() {
        return c3;
    }

    public void setC3(Double c3) {
        this.c3 = c3;
    }

    public Double getPresionConocida() {
        return presionConocida;
    }

    public void setPresionConocida(Double presionConocida) {
        if (presionConocida != null && presionConocida < 0) {
            throw new IllegalArgumentException("La presión conocida no puede ser negativa.");
        }
        this.presionConocida = presionConocida;
    }

    public Double getVoltajeConocido() {
        return voltajeConocido;
    }

    public void setVoltajeConocido(Double voltajeConocido) {
        if (voltajeConocido != null && voltajeConocido < 0) {
            throw new IllegalArgumentException("El voltaje conocido no puede ser negativo.");
        }
        this.voltajeConocido = voltajeConocido;
    }

    public Date getFechaCalibracion() {
        return fechaCalibracion;
    }

    public void setFechaCalibracion(Date fechaCalibracion) {
        this.fechaCalibracion = fechaCalibracion;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = Objects.requireNonNull(sensorType, "El tipo de sensor no puede ser nulo");
    }

    /**
     * Método toString para facilitar la depuración.
     *
     * @return Representación en cadena del objeto.
     */
    @Override
    public String toString() {
        return "SensorCalibracion{" +
                "id=" + id +
                ", a1=" + a1 +
                ", c1=" + c1 +
                ", a2=" + a2 +
                ", c2=" + c2 +
                ", a3=" + a3 +
                ", c3=" + c3 +
                ", presionConocida=" + presionConocida +
                ", voltajeConocido=" + voltajeConocido +
                ", fechaCalibracion=" + fechaCalibracion +
                ", sensorType='" + sensorType + '\'' +
                '}';
    }

    /**
     * Compara dos objetos SensorCalibracion por su ID.
     *
     * @param o Objeto a comparar.
     * @return true si son iguales, false en caso contrario.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorCalibracion that = (SensorCalibracion) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Genera un hash code basado en el ID.
     *
     * @return Hash code del objeto.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}