package com.mediciones.model;

public class Configuracion {

    private int id;
    private String origenDatos;
    private String rutaArchivo;
    private String puertoComDefault;

    public Configuracion() {
    }

    public Configuracion(int id, String origenDatos, String rutaArchivo, String puertoComDefault) {
        this.id = id;
        this.origenDatos = origenDatos;
        this.rutaArchivo = rutaArchivo;
        this.puertoComDefault = puertoComDefault;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrigenDatos() {
        return origenDatos;
    }

    public void setOrigenDatos(String origenDatos) {
        this.origenDatos = origenDatos;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getPuertoComDefault() {
        return puertoComDefault;
    }

    public void setPuertoComDefault(String puertoComDefault) {
        this.puertoComDefault = puertoComDefault;
    }
}
