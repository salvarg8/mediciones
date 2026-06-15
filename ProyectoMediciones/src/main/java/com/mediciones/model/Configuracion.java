package com.mediciones.model;

public class Configuracion {

    private int id;
    private String origenDatos;
    private String rutaArchivo;

    public Configuracion() {
    }

    public Configuracion(int id, String origenDatos, String rutaArchivo) {
        this.id = id;
        this.origenDatos = origenDatos;
        this.rutaArchivo = rutaArchivo;
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
}
